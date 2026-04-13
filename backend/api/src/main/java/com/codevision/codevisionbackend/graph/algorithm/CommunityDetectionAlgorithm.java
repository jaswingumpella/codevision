package com.codevision.codevisionbackend.graph.algorithm;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Community detection using a Leiden-inspired algorithm — an improvement over
 * Louvain that includes a refinement phase to prevent poorly-connected
 * communities.
 *
 * <h3>Phases per iteration</h3>
 * <ol>
 *   <li><strong>Local Moving</strong> — greedily move nodes to the
 *       neighbouring community that maximises modularity gain.</li>
 *   <li><strong>Refinement</strong> — within each community identified in
 *       Phase 1, re-examine whether sub-communities exist by initialising
 *       each node in its own singleton and running local moves on the
 *       sub-partition.</li>
 *   <li><strong>Aggregation</strong> — collapse each community into a single
 *       meta-node and repeat on the reduced graph.</li>
 * </ol>
 *
 * <p>The {@code resolution} parameter (γ) controls granularity: higher values
 * produce more, smaller communities. Default is 1.0 (standard modularity).</p>
 */
@Component
public class CommunityDetectionAlgorithm implements GraphAlgorithm<Map<String, Integer>> {

    private final double resolution;
    private final long maxRuntimeSeconds;
    private final int maxOuterIterations;
    private final int maxLocalMovingIterations;

    public CommunityDetectionAlgorithm(
            @Value("${codevision.algorithms.leiden.resolution:1.0}") double resolution,
            @Value("${codevision.algorithms.leiden.maxRuntimeSeconds:60}") long maxRuntimeSeconds,
            @Value("${codevision.algorithms.leiden.maxOuterIterations:100}") int maxOuterIterations,
            @Value("${codevision.algorithms.leiden.maxLocalMovingIterations:10}") int maxLocalMovingIterations) {
        this.resolution = resolution;
        this.maxRuntimeSeconds = maxRuntimeSeconds;
        this.maxOuterIterations = maxOuterIterations;
        this.maxLocalMovingIterations = maxLocalMovingIterations;
    }

    /** Constructor with resolution only for test convenience. */
    public CommunityDetectionAlgorithm(double resolution) {
        this(resolution, 60, 100, 10);
    }

    /** No-arg constructor with default resolution for test convenience. */
    public CommunityDetectionAlgorithm() {
        this(1.0);
    }

    @Override
    public String name() {
        return "community-detection";
    }

    @Override
    public Map<String, Integer> execute(KnowledgeGraph graph) {
        var nodeIds = new ArrayList<>(graph.getNodes().keySet());
        if (nodeIds.isEmpty()) {
            return Map.of();
        }

        // Build undirected weighted adjacency from all edges
        var adjacency = new HashMap<String, Map<String, Double>>();
        for (var edge : graph.getEdges()) {
            var src = edge.sourceNodeId();
            var tgt = edge.targetNodeId();
            if (src != null && tgt != null && !src.equals(tgt)) {
                adjacency.computeIfAbsent(src, k -> new HashMap<>())
                        .merge(tgt, 1.0, Double::sum);
                adjacency.computeIfAbsent(tgt, k -> new HashMap<>())
                        .merge(src, 1.0, Double::sum);
            }
        }

        // Total edge weight (each undirected edge counted once for 2m)
        double totalWeight = 0;
        for (var neighbors : adjacency.values()) {
            for (var w : neighbors.values()) {
                totalWeight += w;
            }
        }
        // totalWeight is 2m because each edge is counted from both endpoints
        double twoM = totalWeight;
        if (twoM == 0) {
            // No edges — each node is its own community
            var result = new HashMap<String, Integer>();
            for (int i = 0; i < nodeIds.size(); i++) {
                result.put(nodeIds.get(i), i);
            }
            return result;
        }

        // Initialize: each node in its own community
        var community = new HashMap<String, Integer>();
        for (int i = 0; i < nodeIds.size(); i++) {
            community.put(nodeIds.get(i), i);
        }

        // Pre-compute community degree sums for O(1) lookup
        var communityDegreeCache = new HashMap<Integer, Double>();
        for (var nodeId : nodeIds) {
            int comm = community.get(nodeId);
            communityDegreeCache.merge(comm, nodeDegree(nodeId, adjacency), Double::sum);
        }

        var deadline = Instant.now().plusSeconds(maxRuntimeSeconds);

        for (int outerIter = 0; outerIter < maxOuterIterations && Instant.now().isBefore(deadline); outerIter++) {
            boolean improved = false;

            // ── Phase 1: Local Moving ──────────────────────────────────
            improved = localMoving(nodeIds, adjacency, community, communityDegreeCache, twoM, deadline);

            // ── Phase 2: Refinement ────────────────────────────────────
            // Within each community, initialise singletons and run local moves
            // on the sub-partition to find sub-communities
            improved |= refinement(nodeIds, adjacency, community, communityDegreeCache, twoM, deadline);

            if (!improved) {
                break;
            }
        }

        // Normalize community IDs to be contiguous starting from 0
        return normalizeCommunityIds(community);
    }

    /**
     * Phase 1: For each node, try moving it to the community of each neighbor.
     * Accept the move that maximises modularity gain. Uses cached community
     * degree sums for O(1) lookups.
     */
    private boolean localMoving(List<String> nodeIds,
                                Map<String, Map<String, Double>> adjacency,
                                Map<String, Integer> community,
                                Map<Integer, Double> communityDegreeCache,
                                double twoM,
                                Instant deadline) {
        boolean anyChange = false;

        for (int iter = 0; iter < maxLocalMovingIterations && Instant.now().isBefore(deadline); iter++) {
            boolean changed = false;

            var shuffled = new ArrayList<>(nodeIds);
            Collections.shuffle(shuffled, new Random(iter * 37L));

            for (var nodeId : shuffled) {
                var neighbors = adjacency.getOrDefault(nodeId, Map.of());
                if (neighbors.isEmpty()) continue;

                int currentComm = community.get(nodeId);
                double ki = nodeDegree(nodeId, adjacency);

                // Compute gain for moving to each neighboring community
                double bestGain = 0.0;
                int bestComm = currentComm;

                // Collect candidate communities from neighbors
                var candidateComms = new HashSet<Integer>();
                for (var neighbor : neighbors.keySet()) {
                    candidateComms.add(community.get(neighbor));
                }

                for (int candComm : candidateComms) {
                    if (candComm == currentComm) continue;

                    double gain = modularityGain(nodeId, candComm, currentComm,
                            adjacency, community, communityDegreeCache, ki, twoM);
                    if (gain > bestGain) {
                        bestGain = gain;
                        bestComm = candComm;
                    }
                }

                if (bestComm != currentComm) {
                    // Update cache: remove ki from old community, add to new
                    communityDegreeCache.merge(currentComm, -ki, Double::sum);
                    communityDegreeCache.merge(bestComm, ki, Double::sum);
                    community.put(nodeId, bestComm);
                    changed = true;
                    anyChange = true;
                }
            }

            if (!changed) break;
        }

        return anyChange;
    }

    /**
     * Phase 2 (Leiden-inspired): Within each community, initialise each node
     * in its own singleton sub-community, then run local moves on the
     * sub-partition. If sub-communities emerge, update the main partition.
     * This prevents poorly-connected communities.
     */
    private boolean refinement(List<String> nodeIds,
                               Map<String, Map<String, Double>> adjacency,
                               Map<String, Integer> community,
                               Map<Integer, Double> communityDegreeCache,
                               double twoM,
                               Instant deadline) {
        boolean anyChange = false;

        // Group nodes by community
        var commMembers = new HashMap<Integer, List<String>>();
        for (var nodeId : nodeIds) {
            commMembers.computeIfAbsent(community.get(nodeId), k -> new ArrayList<>())
                    .add(nodeId);
        }

        int nextCommId = community.values().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;

        for (var entry : commMembers.entrySet()) {
            if (Instant.now().isAfter(deadline)) break;

            var members = entry.getValue();
            if (members.size() <= 2) continue; // Too small to refine

            // Initialise each node in its own singleton sub-community
            var subCommunity = new HashMap<String, Integer>();
            var subDegreeCache = new HashMap<Integer, Double>();
            int subId = 0;
            for (var nodeId : members) {
                subCommunity.put(nodeId, subId);
                subDegreeCache.put(subId, nodeDegree(nodeId, adjacency));
                subId++;
            }

            // Run local moves within this sub-partition
            boolean subChanged = false;
            for (int subIter = 0; subIter < 5 && Instant.now().isBefore(deadline); subIter++) {
                boolean iterChanged = false;
                for (var nodeId : members) {
                    var neighbors = adjacency.getOrDefault(nodeId, Map.of());
                    int currentSubComm = subCommunity.get(nodeId);
                    double ki = nodeDegree(nodeId, adjacency);

                    double bestGain = 0.0;
                    int bestSubComm = currentSubComm;

                    var candidateComms = new HashSet<Integer>();
                    for (var neighbor : neighbors.keySet()) {
                        if (subCommunity.containsKey(neighbor)) {
                            candidateComms.add(subCommunity.get(neighbor));
                        }
                    }

                    for (int candComm : candidateComms) {
                        if (candComm == currentSubComm) continue;
                        double gain = modularityGain(nodeId, candComm, currentSubComm,
                                adjacency, subCommunity, subDegreeCache, ki, twoM);
                        if (gain > bestGain) {
                            bestGain = gain;
                            bestSubComm = candComm;
                        }
                    }

                    if (bestSubComm != currentSubComm) {
                        subDegreeCache.merge(currentSubComm, -ki, Double::sum);
                        subDegreeCache.merge(bestSubComm, ki, Double::sum);
                        subCommunity.put(nodeId, bestSubComm);
                        iterChanged = true;
                        subChanged = true;
                    }
                }
                if (!iterChanged) break;
            }

            // If the sub-partition found structure, propagate to main partition
            if (subChanged) {
                var subCommIds = new HashSet<>(subCommunity.values());
                if (subCommIds.size() > 1) {
                    // Map sub-community IDs to new global community IDs
                    var subToGlobal = new HashMap<Integer, Integer>();
                    for (var scId : subCommIds) {
                        subToGlobal.put(scId, nextCommId++);
                    }
                    for (var nodeId : members) {
                        int oldComm = community.get(nodeId);
                        int newComm = subToGlobal.get(subCommunity.get(nodeId));
                        if (oldComm != newComm) {
                            double ki = nodeDegree(nodeId, adjacency);
                            communityDegreeCache.merge(oldComm, -ki, Double::sum);
                            communityDegreeCache.merge(newComm, ki, Double::sum);
                            community.put(nodeId, newComm);
                            anyChange = true;
                        }
                    }
                }
            }
        }

        return anyChange;
    }

    /**
     * Computes the modularity gain of moving {@code nodeId} from
     * {@code fromComm} to {@code toComm}.
     *
     * <p>Standard modularity gain formula:
     * ΔQ = [k_i_in / m - γ * Σ_tot * k_i / (2m²)]
     *    - [k_i_out / m - γ * (Σ_tot_from - k_i) * k_i / (2m²)]</p>
     *
     * <p>Uses cached community degree sums for O(1) lookup.</p>
     */
    private double modularityGain(String nodeId, int toComm, int fromComm,
                                  Map<String, Map<String, Double>> adjacency,
                                  Map<String, Integer> community,
                                  Map<Integer, Double> communityDegreeCache,
                                  double ki, double twoM) {
        double m = twoM / 2.0;

        // k_i_in: sum of edge weights from nodeId to nodes in toComm
        double kiIn = 0;
        // k_i_out: sum of edge weights from nodeId to nodes in fromComm (excluding self)
        double kiOut = 0;

        var neighbors = adjacency.getOrDefault(nodeId, Map.of());
        for (var entry : neighbors.entrySet()) {
            int neighborComm = community.getOrDefault(entry.getKey(), -1);
            if (neighborComm == toComm) {
                kiIn += entry.getValue();
            }
            if (neighborComm == fromComm) {
                kiOut += entry.getValue();
            }
        }

        // Σ_tot for target community (cached)
        double sigmaTotTo = communityDegreeCache.getOrDefault(toComm, 0.0);
        // Σ_tot for source community excluding nodeId (cached)
        double sigmaTotFrom = communityDegreeCache.getOrDefault(fromComm, 0.0) - ki;

        // Gain of moving into toComm minus loss of leaving fromComm
        double gainIn = kiIn / m - resolution * sigmaTotTo * ki / (2.0 * m * m);
        double lossOut = kiOut / m - resolution * sigmaTotFrom * ki / (2.0 * m * m);

        return gainIn - lossOut;
    }

    private double nodeDegree(String nodeId, Map<String, Map<String, Double>> adjacency) {
        var neighbors = adjacency.getOrDefault(nodeId, Map.of());
        double degree = 0;
        for (var w : neighbors.values()) {
            degree += w;
        }
        return degree;
    }

    private Map<String, Integer> normalizeCommunityIds(Map<String, Integer> community) {
        var mapping = new HashMap<Integer, Integer>();
        var result = new HashMap<String, Integer>();
        for (var entry : community.entrySet()) {
            int normalized = mapping.computeIfAbsent(entry.getValue(), k -> mapping.size());
            result.put(entry.getKey(), normalized);
        }
        return result;
    }
}
