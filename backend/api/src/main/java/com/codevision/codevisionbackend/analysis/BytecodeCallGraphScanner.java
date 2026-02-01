package com.codevision.codevisionbackend.analysis;

import com.codevision.codevisionbackend.analysis.config.CompiledAnalysisProperties;
import com.codevision.codevisionbackend.analyze.scanner.AnalysisExclusions;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Builds method-level and class-level call graphs using ASM without classloading analyzed code.
 */
@Component
public class BytecodeCallGraphScanner {

    private static final Logger log = LoggerFactory.getLogger(BytecodeCallGraphScanner.class);

    private final CompiledAnalysisProperties properties;

    public BytecodeCallGraphScanner(CompiledAnalysisProperties properties) {
        this.properties = properties;
    }

    public CallGraphResult scan(ClasspathBuilder.ClasspathDescriptor descriptor, List<String> acceptPackages) {
        Map<String, Set<String>> classEdges = new LinkedHashMap<>();
        List<GraphModel.MethodCallEdge> methodEdges = new ArrayList<>();

        for (Path entry : descriptor.getClasspathEntries()) {
            try {
                if (Files.isDirectory(entry)) {
                    scanDirectory(entry, acceptPackages, classEdges, methodEdges);
                } else if (Files.isRegularFile(entry) && entry.toString().endsWith(".jar")) {
                    scanJar(entry, acceptPackages, classEdges, methodEdges);
                }
            } catch (IOException ex) {
                log.warn("Failed scanning {} for call graph: {}", entry, ex.getMessage());
            }
        }
        log.info(
                "Call graph captured {} class edges and {} method edges",
                classEdges.values().stream().mapToInt(Set::size).sum(),
                methodEdges.size());
        return new CallGraphResult(methodEdges, classEdges);
    }

    private void scanDirectory(
            Path directory,
            List<String> acceptPackages,
            Map<String, Set<String>> classEdges,
            List<GraphModel.MethodCallEdge> methodEdges)
            throws IOException {
        Files.walk(directory)
                .filter(path -> path.toString().endsWith(".class"))
                .forEach(classFile -> {
                    try (InputStream inputStream = Files.newInputStream(classFile)) {
                        parseClass(inputStream.readAllBytes(), acceptPackages, classEdges, methodEdges);
                    } catch (IOException ex) {
                        log.debug("Failed to parse class {}: {}", classFile, ex.getMessage());
                    }
                });
    }

    private void scanJar(
            Path jarPath,
            List<String> acceptPackages,
            Map<String, Set<String>> classEdges,
            List<GraphModel.MethodCallEdge> methodEdges)
            throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }
                try (InputStream inputStream = jarFile.getInputStream(entry)) {
                    parseClass(inputStream.readAllBytes(), acceptPackages, classEdges, methodEdges);
                } catch (IOException ioException) {
                    log.debug("Failed to parse {} from {}: {}", entry.getName(), jarPath, ioException.getMessage());
                }
            }
        }
    }

    private void parseClass(
            byte[] bytes,
            List<String> acceptPackages,
            Map<String, Set<String>> classEdges,
            List<GraphModel.MethodCallEdge> methodEdges)
            throws IOException {
        ClassReader reader = new ClassReader(bytes);
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            private String currentClassName;

            @Override
            public void visit(
                    int version,
                    int access,
                    String name,
                    String signature,
                    String superName,
                    String[] interfaces) {
                this.currentClassName = name.replace('/', '.');
            }

            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(
                            int opcode,
                            String owner,
                            String methodName,
                            String methodDescriptor,
                            boolean isInterface) {
                        String callerClass = currentClassName;
                        String calleeClass = owner.replace('/', '.');
                        if (!shouldInclude(callerClass, acceptPackages)
                                || !shouldInclude(calleeClass, acceptPackages)) {
                            return;
                        }
                        methodEdges.add(new GraphModel.MethodCallEdge(
                                callerClass, name, descriptor, calleeClass, methodName, methodDescriptor));
                        classEdges.computeIfAbsent(callerClass, key -> new LinkedHashSet<>()).add(calleeClass);
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    private boolean shouldInclude(String className, List<String> acceptPackages) {
        if (className == null || className.startsWith("java.") || className.startsWith("jakarta.")) {
            return false;
        }
        if (AnalysisExclusions.isMockClassName(className)) {
            return false;
        }
        return GraphModel.isUserPackage(className, acceptPackages);
    }

    public record CallGraphResult(
            List<GraphModel.MethodCallEdge> methodEdges, Map<String, Set<String>> classAdjacency) {}
}
