package com.codevision.codevisionbackend.analyze.diagram;

import com.codevision.codevisionbackend.analyze.scanner.AnalysisExclusions;
import com.codevision.codevisionbackend.analyze.scanner.ApiEndpointRecord;
import com.codevision.codevisionbackend.analyze.scanner.ClassMetadataRecord;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ControlFlowSequenceBuilder {

    private static final Logger log = LoggerFactory.getLogger(ControlFlowSequenceBuilder.class);
    private static final int MAX_STATEMENT_DEPTH = 8000;

    private final SourceIndex sourceIndex;
    private final int maxDepth;

    ControlFlowSequenceBuilder(
            Path repoRoot,
            List<Path> moduleRoots,
            List<ClassMetadataRecord> classRecords,
            int maxDepth) {
        this.sourceIndex = new SourceIndex(repoRoot, moduleRoots, classRecords);
        this.maxDepth = maxDepth;
    }

    SequenceFlow build(ApiEndpointRecord endpoint, boolean includeExternal) {
        if (endpoint == null || endpoint.controllerClass() == null || endpoint.controllerMethod() == null) {
            return SequenceFlow.empty();
        }
        String controllerClass = endpoint.controllerClass();
        String controllerMethod = endpoint.controllerMethod();
        MethodKey entry = sourceIndex.resolveEntryMethod(controllerClass, controllerMethod);
        if (entry == null) {
            return SequenceFlow.empty();
        }
        FlowBlock root = buildMethodFlow(entry, includeExternal, new ArrayDeque<>(), 0);
        return new SequenceFlow(entry, root);
    }

    List<String> summarize(SequenceFlow flow, int limit) {
        if (flow == null || flow.root() == null) {
            return List.of();
        }
        List<String> summaries = new ArrayList<>();
        flattenCalls(flow.root(), summaries, limit);
        return summaries;
    }

    private void flattenCalls(FlowBlock block, List<String> summaries, int limit) {
        if (block == null || summaries.size() >= limit) {
            return;
        }
        for (FlowElement element : block.elements()) {
            if (summaries.size() >= limit) {
                return;
            }
            if (element instanceof CallFlow call) {
                summaries.add(call.summaryLabel());
                if (call.inline() != null) {
                    flattenCalls(call.inline(), summaries, limit);
                }
            } else if (element instanceof IfFlow ifFlow) {
                flattenCalls(ifFlow.thenBlock(), summaries, limit);
                if (ifFlow.elseBlock() != null) {
                    flattenCalls(ifFlow.elseBlock(), summaries, limit);
                }
            } else if (element instanceof LoopFlow loopFlow) {
                flattenCalls(loopFlow.body(), summaries, limit);
            } else if (element instanceof SwitchFlow switchFlow) {
                for (CaseFlow caseFlow : switchFlow.cases()) {
                    flattenCalls(caseFlow.body(), summaries, limit);
                }
            } else if (element instanceof TryFlow tryFlow) {
                flattenCalls(tryFlow.tryBlock(), summaries, limit);
                for (CatchFlow catchFlow : tryFlow.catches()) {
                    flattenCalls(catchFlow.body(), summaries, limit);
                }
                if (tryFlow.finallyBlock() != null) {
                    flattenCalls(tryFlow.finallyBlock(), summaries, limit);
                }
            } else if (element instanceof DispatchFlow dispatchFlow) {
                for (DispatchCase dispatchCase : dispatchFlow.cases()) {
                    flattenCalls(dispatchCase.body(), summaries, limit);
                }
            }
        }
    }

    private FlowBlock buildMethodFlow(MethodKey methodKey, boolean includeExternal, Deque<MethodKey> stack, int depth) {
        if (methodKey == null) {
            return FlowBlock.empty();
        }
        if (depth > maxDepth) {
            return FlowBlock.of(new NoteFlow("Max call depth reached"));
        }
        if (stack.contains(methodKey)) {
            return FlowBlock.of(new NoteFlow("[cycle -> " + methodKey.displayName() + "]"));
        }
        CallableDeclaration<?> callable = sourceIndex.callableByKey(methodKey);
        if (callable == null) {
            return FlowBlock.empty();
        }
        stack.push(methodKey);
        FlowBlock block = buildCallableFlow(callable, methodKey, includeExternal, stack, depth);
        stack.pop();
        return block;
    }

    private FlowBlock buildCallableFlow(
            CallableDeclaration<?> callable,
            MethodKey methodKey,
            boolean includeExternal,
            Deque<MethodKey> stack,
            int depth) {
        if (callable == null) {
            return FlowBlock.empty();
        }
        BlockStmt body = extractBody(callable);
        if (body == null) {
            return FlowBlock.empty();
        }
        return buildBlock(body, new MethodContext(methodKey, includeExternal, stack, depth));
    }

    private BlockStmt extractBody(CallableDeclaration<?> callable) {
        if (callable instanceof MethodDeclaration method) {
            return method.getBody().orElse(null);
        }
        if (callable instanceof ConstructorDeclaration constructorDeclaration) {
            return constructorDeclaration.getBody();
        }
        return null;
    }

    private FlowBlock buildBlock(BlockStmt blockStmt, MethodContext context) {
        FlowBlock block = new FlowBlock();
        for (Statement statement : blockStmt.getStatements()) {
            if (block.isTerminated()) {
                break;
            }
            FlowBlock statementFlow = buildStatement(statement, context);
            block.append(statementFlow);
        }
        return block;
    }

    private FlowBlock buildStatement(Statement statement, MethodContext context) {
        if (statement == null) {
            return FlowBlock.empty();
        }
        if (statement instanceof BlockStmt blockStmt) {
            return buildBlock(blockStmt, context);
        }
        if (statement instanceof ExpressionStmt exprStmt) {
            return buildExpression(exprStmt.getExpression(), context);
        }
        if (statement instanceof ReturnStmt returnStmt) {
            FlowBlock block = buildExpression(returnStmt.getExpression().orElse(null), context);
            block.addElement(new ReturnFlow());
            return block;
        }
        if (statement instanceof ThrowStmt throwStmt) {
            FlowBlock block = buildExpression(throwStmt.getExpression(), context);
            block.addElement(new ThrowFlow());
            return block;
        }
        if (statement instanceof BreakStmt) {
            return FlowBlock.of(new BreakFlow());
        }
        if (statement instanceof ContinueStmt) {
            return FlowBlock.of(new ContinueFlow());
        }
        if (statement instanceof IfStmt ifStmt) {
            FlowBlock thenBlock = buildStatement(wrapToBlock(ifStmt.getThenStmt()), context);
            FlowBlock elseBlock = ifStmt.getElseStmt()
                    .map(stmt -> buildStatement(wrapToBlock(stmt), context))
                    .orElse(null);
            return buildConditionBlock(ifStmt.getCondition(), thenBlock, elseBlock, context);
        }
        if (statement instanceof ForStmt forStmt) {
            FlowBlock block = new FlowBlock();
            for (Expression init : forStmt.getInitialization()) {
                block.append(buildExpression(init, context));
            }
            FlowBlock body = buildStatement(wrapToBlock(forStmt.getBody()), context);
            FlowBlock update = new FlowBlock();
            for (Expression updateExpr : forStmt.getUpdate()) {
                update.append(buildExpression(updateExpr, context));
            }
            if (!update.elements().isEmpty()) {
                body.append(update);
            }
            String condition = forStmt.getCompare().map(Node::toString).orElse("for");
            block.addElement(new LoopFlow(condition, body));
            return block;
        }
        if (statement instanceof ForEachStmt forEachStmt) {
            FlowBlock body = buildStatement(wrapToBlock(forEachStmt.getBody()), context);
            String condition = forEachStmt.getVariable().toString() + " : " + forEachStmt.getIterable();
            return FlowBlock.of(new LoopFlow(condition, body));
        }
        if (statement instanceof WhileStmt whileStmt) {
            FlowBlock body = buildStatement(wrapToBlock(whileStmt.getBody()), context);
            return FlowBlock.of(new LoopFlow(whileStmt.getCondition().toString(), body));
        }
        if (statement instanceof DoStmt doStmt) {
            FlowBlock body = buildStatement(wrapToBlock(doStmt.getBody()), context);
            return FlowBlock.of(new LoopFlow("do/while " + doStmt.getCondition(), body));
        }
        if (statement instanceof SwitchStmt switchStmt) {
            return FlowBlock.of(buildSwitchFlow(switchStmt, context));
        }
        if (statement instanceof TryStmt tryStmt) {
            return FlowBlock.of(buildTryFlow(tryStmt, context));
        }
        if (statement instanceof SynchronizedStmt synchronizedStmt) {
            FlowBlock body = buildStatement(wrapToBlock(synchronizedStmt.getBody()), context);
            return FlowBlock.of(new GroupFlow("synchronized " + synchronizedStmt.getExpression(), body));
        }
        if (statement instanceof LabeledStmt labeledStmt) {
            return buildStatement(labeledStmt.getStatement(), context);
        }
        if (statement instanceof AssertStmt assertStmt) {
            return FlowBlock.of(new NoteFlow("assert " + assertStmt.getCheck()));
        }
        return FlowBlock.of(new NoteFlow(statement.getClass().getSimpleName()));
    }

    private FlowBlock buildExpression(Expression expression, MethodContext context) {
        if (expression == null) {
            return FlowBlock.empty();
        }
        if (expression instanceof MethodCallExpr callExpr) {
            FlowBlock block = new FlowBlock();
            callExpr.getScope().ifPresent(scope -> block.append(buildExpression(scope, context)));
            for (Expression arg : callExpr.getArguments()) {
                block.append(buildExpression(arg, context));
            }
            FlowElement call = buildCallFlow(callExpr, context);
            if (call != null) {
                block.addElement(call);
            }
            return block;
        }
        if (expression instanceof ObjectCreationExpr creationExpr) {
            FlowBlock block = new FlowBlock();
            creationExpr.getScope().ifPresent(scope -> block.append(buildExpression(scope, context)));
            for (Expression arg : creationExpr.getArguments()) {
                block.append(buildExpression(arg, context));
            }
            FlowElement ctor = buildConstructorFlow(creationExpr, context);
            if (ctor != null) {
                block.addElement(ctor);
            }
            return block;
        }
        if (expression instanceof ConditionalExpr conditionalExpr) {
            FlowBlock thenBlock = buildExpression(conditionalExpr.getThenExpr(), context);
            FlowBlock elseBlock = buildExpression(conditionalExpr.getElseExpr(), context);
            return buildConditionBlock(conditionalExpr.getCondition(), thenBlock, elseBlock, context);
        }
        if (expression instanceof BinaryExpr binaryExpr) {
            if (binaryExpr.getOperator() == BinaryExpr.Operator.AND
                    || binaryExpr.getOperator() == BinaryExpr.Operator.OR) {
                FlowBlock thenBlock = FlowBlock.of(new NoteFlow("condition true"));
                FlowBlock elseBlock = FlowBlock.of(new NoteFlow("condition false"));
                return buildConditionBlock(binaryExpr, thenBlock, elseBlock, context);
            }
            FlowBlock block = new FlowBlock();
            block.append(buildExpression(binaryExpr.getLeft(), context));
            block.append(buildExpression(binaryExpr.getRight(), context));
            return block;
        }
        if (expression instanceof UnaryExpr unaryExpr) {
            return buildExpression(unaryExpr.getExpression(), context);
        }

        FlowBlock block = new FlowBlock();
        for (Node child : expression.getChildNodes()) {
            if (child instanceof Expression childExpr) {
                block.append(buildExpression(childExpr, context));
            }
        }
        return block;
    }

    private FlowBlock buildConditionBlock(
            Expression condition,
            FlowBlock thenBlock,
            FlowBlock elseBlock,
            MethodContext context) {
        if (condition == null) {
            return FlowBlock.of(new IfFlow("condition", thenBlock, elseBlock));
        }
        if (condition instanceof BinaryExpr binaryExpr) {
            if (binaryExpr.getOperator() == BinaryExpr.Operator.AND) {
                FlowBlock nestedThen = buildConditionBlock(binaryExpr.getRight(), thenBlock, elseBlock, context);
                return buildConditionBlock(binaryExpr.getLeft(), nestedThen, elseBlock, context);
            }
            if (binaryExpr.getOperator() == BinaryExpr.Operator.OR) {
                FlowBlock nestedElse = buildConditionBlock(binaryExpr.getRight(), thenBlock, elseBlock, context);
                return buildConditionBlock(binaryExpr.getLeft(), thenBlock, nestedElse, context);
            }
        }
        if (condition instanceof UnaryExpr unaryExpr && unaryExpr.getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
            return buildConditionBlock(unaryExpr.getExpression(), elseBlock, thenBlock, context);
        }
        FlowBlock block = new FlowBlock();
        block.append(buildExpression(condition, context));
        block.addElement(new IfFlow(condition.toString(), thenBlock, elseBlock));
        return block;
    }

    private FlowElement buildSwitchFlow(SwitchStmt switchStmt, MethodContext context) {
        List<SwitchEntry> entries = switchStmt.getEntries();
        List<CaseFlow> cases = new ArrayList<>();
        for (SwitchEntry entry : entries) {
            String label = entry.getLabels().isEmpty()
                    ? "default"
                    : entry.getLabels().stream().map(Node::toString).collect(Collectors.joining(", "));
            FlowBlock block = new FlowBlock();
            for (Statement statement : entry.getStatements()) {
                if (block.isTerminated()) {
                    break;
                }
                block.append(buildStatement(statement, context));
            }
            boolean fallsThrough = !block.isTerminated();
            cases.add(new CaseFlow(label, block, fallsThrough));
        }

        List<CaseFlow> expanded = new ArrayList<>();
        for (int i = 0; i < cases.size(); i++) {
            CaseFlow current = cases.get(i);
            FlowBlock combined = current.body().copy();
            int j = i;
            while (current.fallsThrough() && j + 1 < cases.size()) {
                CaseFlow next = cases.get(j + 1);
                combined.addElement(new NoteFlow("fallthrough -> " + next.label()));
                combined.append(next.body());
                if (!next.fallsThrough()) {
                    break;
                }
                j++;
            }
            expanded.add(new CaseFlow(current.label(), combined, current.fallsThrough()));
        }

        return new SwitchFlow(switchStmt.getSelector().toString(), expanded);
    }

    private FlowElement buildTryFlow(TryStmt tryStmt, MethodContext context) {
        FlowBlock tryBlock = buildStatement(wrapToBlock(tryStmt.getTryBlock()), context);
        List<CatchFlow> catches = tryStmt.getCatchClauses().stream()
                .map(catchClause -> {
                    String label = catchClause.getParameter().getType() + " " + catchClause.getParameter().getName();
                    FlowBlock body = buildStatement(wrapToBlock(catchClause.getBody()), context);
                    return new CatchFlow(label, body);
                })
                .collect(Collectors.toList());
        FlowBlock finallyBlock = tryStmt.getFinallyBlock()
                .map(block -> buildStatement(wrapToBlock(block), context))
                .orElse(null);
        return new TryFlow(tryBlock, catches, finallyBlock);
    }

    private BlockStmt wrapToBlock(Statement statement) {
        if (statement instanceof BlockStmt blockStmt) {
            return blockStmt;
        }
        BlockStmt block = new BlockStmt();
        block.addStatement(statement);
        return block;
    }

    private FlowElement buildCallFlow(MethodCallExpr callExpr, MethodContext context) {
        CallTarget target = sourceIndex.resolveCall(callExpr, context.methodKey());
        if (target == null) {
            return new NoteFlow("call " + callExpr.getNameAsString() + "()");
        }
        if (target.external() && !context.includeExternal()) {
            return null;
        }
        FlowBlock inline = null;
        if (!target.external()) {
            if (target.candidates().size() > 1) {
                List<DispatchCase> cases = new ArrayList<>();
                for (MethodKey candidate : target.candidates()) {
                    FlowBlock candidateFlow = buildMethodFlow(candidate, context.includeExternal(), context.stack(), context.depth() + 1);
                    cases.add(new DispatchCase(candidate.className(), candidateFlow));
                }
                return new DispatchFlow(
                        context.methodKey().className(),
                        target.primary().className(),
                        callExpr.getNameAsString(),
                        cases);
            }
            MethodKey resolved = target.primary();
            inline = buildMethodFlow(resolved, context.includeExternal(), context.stack(), context.depth() + 1);
        }
        return new CallFlow(context.methodKey().className(), target.primary().className(), callExpr.getNameAsString(), inline);
    }

    private FlowElement buildConstructorFlow(ObjectCreationExpr creationExpr, MethodContext context) {
        CallTarget target = sourceIndex.resolveConstructor(creationExpr, context.methodKey());
        if (target == null) {
            return new NoteFlow("new " + creationExpr.getType());
        }
        if (target.external() && !context.includeExternal()) {
            return null;
        }
        FlowBlock inline = null;
        if (!target.external()) {
            MethodKey resolved = target.primary();
            inline = buildMethodFlow(resolved, context.includeExternal(), context.stack(), context.depth() + 1);
        }
        return new CallFlow(context.methodKey().className(), target.primary().className(), "<init>", inline);
    }

    private record MethodContext(
            MethodKey methodKey,
            boolean includeExternal,
            Deque<MethodKey> stack,
            int depth) {}

    private record MethodKey(String className, String methodName, String signature) {
        String displayName() {
            return className + "." + methodName + "()";
        }
    }

    private record CallTarget(MethodKey primary, List<MethodKey> candidates, boolean external) {
        String displayName() {
            if (primary == null) {
                return "call";
            }
            return primary.className() + "." + primary.methodName() + "()";
        }
    }

    private static final class SourceIndex {
        private final Map<String, CallableDeclaration<?>> callablesBySignature = new LinkedHashMap<>();
        private final Map<String, List<MethodKey>> methodsByClassAndName = new LinkedHashMap<>();
        private final Map<String, String> classBySimpleName = new LinkedHashMap<>();
        private final Map<String, List<String>> implementersByType = new LinkedHashMap<>();
        private final Set<String> knownClasses = new LinkedHashSet<>();
        private final JavaParser parser;
        private final TypeSolver typeSolver;

        private SourceIndex(Path repoRoot, List<Path> moduleRoots, List<ClassMetadataRecord> classRecords) {
            CombinedTypeSolver solver = new CombinedTypeSolver();
            solver.add(new ReflectionTypeSolver(false));
            List<Path> sourceRoots = resolveSourceRoots(repoRoot, moduleRoots);
            for (Path root : sourceRoots) {
                solver.add(new JavaParserTypeSolver(root));
            }
            this.typeSolver = solver;
            ParserConfiguration configuration = new ParserConfiguration()
                    .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE)
                    .setSymbolResolver(new JavaSymbolSolver(solver));
            this.parser = new JavaParser(configuration);

            if (classRecords != null) {
                for (ClassMetadataRecord record : classRecords) {
                    if (record.fullyQualifiedName() != null) {
                        knownClasses.add(record.fullyQualifiedName());
                    }
                    if (record.className() != null && record.fullyQualifiedName() != null) {
                        classBySimpleName.putIfAbsent(record.className(), record.fullyQualifiedName());
                    }
                }
            }
            indexSourceFiles(sourceRoots);
        }

        private List<Path> resolveSourceRoots(Path repoRoot, List<Path> moduleRoots) {
            if (moduleRoots == null || moduleRoots.isEmpty()) {
                return repoRoot == null ? List.of() : List.of(repoRoot.resolve("src/main/java"));
            }
            return moduleRoots.stream()
                    .map(root -> root.resolve("src/main/java"))
                    .filter(Files::isDirectory)
                    .distinct()
                    .toList();
        }

        private void indexSourceFiles(List<Path> sourceRoots) {
            Set<Path> visited = new HashSet<>();
            for (Path root : sourceRoots) {
                if (root == null || !Files.isDirectory(root) || !visited.add(root)) {
                    continue;
                }
                try (Stream<Path> paths = Files.walk(root, MAX_STATEMENT_DEPTH, FileVisitOption.FOLLOW_LINKS)) {
                    paths.filter(Files::isRegularFile)
                            .filter(path -> path.toString().endsWith(".java"))
                            .filter(path -> !AnalysisExclusions.isExcludedPath(path))
                            .forEach(this::parseAndIndex);
                } catch (IOException ex) {
                    log.warn("Failed walking {}: {}", root, ex.getMessage());
                }
            }
        }

        private void parseAndIndex(Path sourceFile) {
            try {
                Optional<CompilationUnit> unit = parser.parse(sourceFile).getResult();
                if (unit.isEmpty()) {
                    return;
                }
                String packageName = unit.get().getPackageDeclaration()
                        .map(pkg -> pkg.getName().asString())
                        .orElse("");
                for (TypeDeclaration<?> type : unit.get().getTypes()) {
                    indexType(type, packageName);
                }
            } catch (IOException | ParseProblemException ex) {
                log.debug("Failed parsing {}: {}", sourceFile, ex.getMessage());
            }
        }

        private void indexType(TypeDeclaration<?> type, String packageName) {
            if (type.isAnnotationDeclaration()) {
                return;
            }
            String className = type.getNameAsString();
            String fqName = type.getFullyQualifiedName()
                    .orElseGet(() -> packageName == null || packageName.isBlank()
                            ? className
                            : packageName + "." + className);
            knownClasses.add(fqName);
            classBySimpleName.putIfAbsent(className, fqName);

            if (type instanceof ClassOrInterfaceDeclaration classDecl) {
                registerImplementations(classDecl, fqName);
                for (MethodDeclaration method : classDecl.getMethods()) {
                    registerCallable(fqName, method);
                }
                for (ConstructorDeclaration ctor : classDecl.getConstructors()) {
                    registerCallable(fqName, ctor);
                }
            } else if (type instanceof RecordDeclaration recordDeclaration) {
                registerImplementations(recordDeclaration, fqName);
                for (MethodDeclaration method : recordDeclaration.getMethods()) {
                    registerCallable(fqName, method);
                }
                for (ConstructorDeclaration ctor : recordDeclaration.getConstructors()) {
                    registerCallable(fqName, ctor);
                }
            } else if (type instanceof EnumDeclaration enumDeclaration) {
                for (MethodDeclaration method : enumDeclaration.getMethods()) {
                    registerCallable(fqName, method);
                }
            }
        }

        private void registerImplementations(ClassOrInterfaceDeclaration declaration, String className) {
            if (declaration == null) {
                return;
            }
            declaration.getImplementedTypes().forEach(type -> registerImplementation(type, className));
            declaration.getExtendedTypes().forEach(type -> registerImplementation(type, className));
        }

        private void registerImplementations(RecordDeclaration declaration, String className) {
            if (declaration == null) {
                return;
            }
            declaration.getImplementedTypes().forEach(type -> registerImplementation(type, className));
        }

        private void registerImplementation(com.github.javaparser.ast.type.ClassOrInterfaceType type, String className) {
            if (type == null) {
                return;
            }
            String key = resolveTypeName(type.asString());
            if (key == null) {
                return;
            }
            implementersByType
                    .computeIfAbsent(key, ignored -> new ArrayList<>())
                    .add(className);
        }

        private void registerCallable(String className, CallableDeclaration<?> callable) {
            String signature = resolveSignature(className, callable);
            if (signature == null) {
                return;
            }
            callablesBySignature.put(signature, callable);
            MethodKey key = new MethodKey(className, callable.getNameAsString(), signature);
            String lookupKey = className + "#" + callable.getNameAsString();
            methodsByClassAndName
                    .computeIfAbsent(lookupKey, ignored -> new ArrayList<>())
                    .add(key);
        }

        private String resolveSignature(String className, CallableDeclaration<?> callable) {
            try {
                if (callable instanceof MethodDeclaration method) {
                    ResolvedMethodDeclaration resolved = method.resolve();
                    return resolved.getQualifiedSignature();
                }
                if (callable instanceof ConstructorDeclaration constructorDeclaration) {
                    ResolvedConstructorDeclaration resolved = constructorDeclaration.resolve();
                    return resolved.getQualifiedSignature();
                }
            } catch (RuntimeException ex) {
                // fall back below
            }
            int params = callable.getParameters().size();
            return className + "#" + callable.getNameAsString() + "/" + params;
        }

        MethodKey resolveEntryMethod(String className, String methodName) {
            if (className == null || methodName == null) {
                return null;
            }
            List<MethodKey> candidates = methodsByClassAndName.get(className + "#" + methodName);
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }
            if (candidates.size() > 1) {
                candidates = candidates.stream()
                        .sorted(Comparator.comparing(MethodKey::signature))
                        .toList();
            }
            return candidates.getFirst();
        }

        CallableDeclaration<?> callableByKey(MethodKey key) {
            if (key == null) {
                return null;
            }
            return callablesBySignature.get(key.signature());
        }

        CallTarget resolveCall(MethodCallExpr callExpr, MethodKey context) {
            if (callExpr == null) {
                return null;
            }
            try {
                ResolvedMethodDeclaration resolved = callExpr.resolve();
                String signature = resolved.getQualifiedSignature();
                String className = resolved.declaringType().getQualifiedName();
                MethodKey key = new MethodKey(className, resolved.getName(), signature);
                return resolveDispatch(key);
            } catch (UnsolvedSymbolException | UnsupportedOperationException ex) {
                return fallbackResolve(callExpr, context);
            } catch (RuntimeException ex) {
                return fallbackResolve(callExpr, context);
            }
        }

        CallTarget resolveConstructor(ObjectCreationExpr creationExpr, MethodKey context) {
            if (creationExpr == null) {
                return null;
            }
            try {
                ResolvedConstructorDeclaration resolved = creationExpr.resolve();
                String signature = resolved.getQualifiedSignature();
                String className = resolved.declaringType().getQualifiedName();
                MethodKey key = new MethodKey(className, "<init>", signature);
                return resolveDispatch(key);
            } catch (RuntimeException ex) {
                String typeName = resolveTypeName(creationExpr.getType().asString());
                if (typeName == null) {
                    return null;
                }
                MethodKey key = new MethodKey(typeName, "<init>", typeName + "#<init>/" + creationExpr.getArguments().size());
                return resolveDispatch(key);
            }
        }

        private CallTarget resolveDispatch(MethodKey key) {
            if (key == null) {
                return null;
            }
            List<MethodKey> candidates = new ArrayList<>();
            String resolvedClass = key.className();
            if (resolvedClass != null) {
                candidates.add(key);
                List<String> implementers = implementersByType.get(resolvedClass);
                if (implementers != null && !implementers.isEmpty()) {
                    for (String impl : implementers) {
                        MethodKey implKey = new MethodKey(impl, key.methodName(), impl + "#" + key.methodName() + "/" + parameterCount(key.signature()));
                        candidates.add(implKey);
                    }
                }
            }
            boolean external = !isKnownClass(key.className());
            List<MethodKey> resolvedCandidates = new ArrayList<>();
            for (MethodKey candidate : candidates) {
                if (candidate == null) {
                    continue;
                }
                if (callablesBySignature.containsKey(candidate.signature())) {
                    resolvedCandidates.add(candidate);
                    continue;
                }
                List<MethodKey> byName = methodsByClassAndName.get(candidate.className() + "#" + candidate.methodName());
                if (byName == null || byName.isEmpty()) {
                    continue;
                }
                int desiredParams = parameterCount(candidate.signature());
                List<MethodKey> matching = byName.stream()
                        .filter(methodKey -> parameterCount(methodKey.signature()) == desiredParams)
                        .toList();
                if (!matching.isEmpty()) {
                    resolvedCandidates.addAll(matching);
                } else {
                    resolvedCandidates.add(byName.getFirst());
                }
            }
            resolvedCandidates = resolvedCandidates.stream().distinct().toList();
            if (!resolvedCandidates.isEmpty()) {
                MethodKey primary = resolvedCandidates.getFirst();
                return new CallTarget(primary, resolvedCandidates, false);
            }
            return new CallTarget(key, List.of(key), external);
        }

        private CallTarget fallbackResolve(MethodCallExpr callExpr, MethodKey context) {
            String scopeType = resolveScopeType(callExpr, context);
            if (scopeType == null) {
                return null;
            }
            String methodName = callExpr.getNameAsString();
            int paramCount = callExpr.getArguments().size();
            MethodKey key = new MethodKey(scopeType, methodName, scopeType + "#" + methodName + "/" + paramCount);
            return resolveDispatch(key);
        }

        private String resolveScopeType(MethodCallExpr callExpr, MethodKey context) {
            if (callExpr.getScope().isEmpty()) {
                return context == null ? null : context.className();
            }
            Expression scope = callExpr.getScope().get();
            try {
                ResolvedReferenceType type = JavaParserFacade.get(typeSolver).getType(scope).asReferenceType();
                return type.getQualifiedName();
            } catch (RuntimeException ex) {
                String fallback = scope.toString();
                if (classBySimpleName.containsKey(fallback)) {
                    return classBySimpleName.get(fallback);
                }
            }
            return null;
        }

        private String resolveTypeName(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            String cleaned = raw.replace("[]", "").trim();
            int genericStart = cleaned.indexOf('<');
            if (genericStart > 0) {
                cleaned = cleaned.substring(0, genericStart);
            }
            if (cleaned.contains(".")) {
                return cleaned;
            }
            return classBySimpleName.getOrDefault(cleaned, cleaned);
        }

        private boolean isKnownClass(String className) {
            if (className == null || className.isBlank()) {
                return false;
            }
            if (knownClasses.contains(className)) {
                return true;
            }
            String simple = className;
            int idx = simple.lastIndexOf('.');
            if (idx >= 0) {
                simple = simple.substring(idx + 1);
            }
            return classBySimpleName.containsKey(simple);
        }

        private int parameterCount(String signature) {
            if (signature == null) {
                return 0;
            }
            int start = signature.indexOf('(');
            int end = signature.indexOf(')');
            if (start < 0 || end <= start) {
                int slash = signature.lastIndexOf('/');
                if (slash >= 0) {
                    try {
                        return Integer.parseInt(signature.substring(slash + 1));
                    } catch (NumberFormatException ignored) {
                        return 0;
                    }
                }
                return 0;
            }
            String args = signature.substring(start + 1, end).trim();
            if (args.isEmpty()) {
                return 0;
            }
            return (int) Arrays.stream(args.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .count();
        }
    }

    static final class SequenceFlow {
        private final MethodKey entry;
        private final FlowBlock root;

        private SequenceFlow(MethodKey entry, FlowBlock root) {
            this.entry = entry;
            this.root = root == null ? FlowBlock.empty() : root;
        }

        static SequenceFlow empty() {
            return new SequenceFlow(null, FlowBlock.empty());
        }

        MethodKey entry() {
            return entry;
        }

        FlowBlock root() {
            return root;
        }
    }

    static final class FlowBlock {
        private final List<FlowElement> elements = new ArrayList<>();
        private boolean terminated;

        static FlowBlock empty() {
            return new FlowBlock();
        }

        static FlowBlock of(FlowElement element) {
            FlowBlock block = new FlowBlock();
            if (element != null) {
                block.addElement(element);
            }
            return block;
        }

        void addElement(FlowElement element) {
            if (element == null) {
                return;
            }
            elements.add(element);
            if (element.terminates()) {
                terminated = true;
            }
        }

        void append(FlowBlock block) {
            if (block == null) {
                return;
            }
            for (FlowElement element : block.elements()) {
                if (terminated) {
                    break;
                }
                addElement(element);
            }
        }

        boolean isTerminated() {
            return terminated;
        }

        List<FlowElement> elements() {
            return Collections.unmodifiableList(elements);
        }

        FlowBlock copy() {
            FlowBlock clone = new FlowBlock();
            clone.elements.addAll(this.elements);
            clone.terminated = this.terminated;
            return clone;
        }
    }

    interface FlowElement {
        boolean terminates();
    }

    static final class CallFlow implements FlowElement {
        private final String sourceClass;
        private final String targetClass;
        private final String methodName;
        private final FlowBlock inline;

        CallFlow(String sourceClass, String targetClass, String methodName, FlowBlock inline) {
            this.sourceClass = sourceClass;
            this.targetClass = targetClass;
            this.methodName = methodName;
            this.inline = inline;
        }

        String sourceClass() {
            return sourceClass;
        }

        String targetClass() {
            return targetClass;
        }

        String methodName() {
            return methodName;
        }

        FlowBlock inline() {
            return inline;
        }

        String summaryLabel() {
            String left = simple(sourceClass);
            String right = simple(targetClass);
            String label = methodName == null ? "call" : methodName;
            return left + " -> " + right + "." + label + "()";
        }

        @Override
        public boolean terminates() {
            return false;
        }

        private String simple(String fqn) {
            if (fqn == null) {
                return "";
            }
            int idx = fqn.lastIndexOf('.');
            return idx >= 0 ? fqn.substring(idx + 1) : fqn;
        }
    }

    static final class IfFlow implements FlowElement {
        private final String condition;
        private final FlowBlock thenBlock;
        private final FlowBlock elseBlock;

        IfFlow(String condition, FlowBlock thenBlock, FlowBlock elseBlock) {
            this.condition = condition;
            this.thenBlock = thenBlock == null ? FlowBlock.empty() : thenBlock;
            this.elseBlock = elseBlock;
        }

        String condition() {
            return condition;
        }

        FlowBlock thenBlock() {
            return thenBlock;
        }

        FlowBlock elseBlock() {
            return elseBlock;
        }

        @Override
        public boolean terminates() {
            if (elseBlock == null) {
                return false;
            }
            return thenBlock.isTerminated() && elseBlock.isTerminated();
        }
    }

    static final class LoopFlow implements FlowElement {
        private final String condition;
        private final FlowBlock body;

        LoopFlow(String condition, FlowBlock body) {
            this.condition = condition;
            this.body = body == null ? FlowBlock.empty() : body;
        }

        String condition() {
            return condition;
        }

        FlowBlock body() {
            return body;
        }

        @Override
        public boolean terminates() {
            return false;
        }
    }

    static final class SwitchFlow implements FlowElement {
        private final String selector;
        private final List<CaseFlow> cases;

        SwitchFlow(String selector, List<CaseFlow> cases) {
            this.selector = selector;
            this.cases = cases == null ? List.of() : cases;
        }

        String selector() {
            return selector;
        }

        List<CaseFlow> cases() {
            return cases;
        }

        @Override
        public boolean terminates() {
            if (cases.isEmpty()) {
                return false;
            }
            return cases.stream().allMatch(caseFlow -> caseFlow.body().isTerminated());
        }
    }

    static final class CaseFlow {
        private final String label;
        private final FlowBlock body;
        private final boolean fallsThrough;

        CaseFlow(String label, FlowBlock body, boolean fallsThrough) {
            this.label = label;
            this.body = body == null ? FlowBlock.empty() : body;
            this.fallsThrough = fallsThrough;
        }

        String label() {
            return label;
        }

        FlowBlock body() {
            return body;
        }

        boolean fallsThrough() {
            return fallsThrough;
        }
    }

    static final class TryFlow implements FlowElement {
        private final FlowBlock tryBlock;
        private final List<CatchFlow> catches;
        private final FlowBlock finallyBlock;

        TryFlow(FlowBlock tryBlock, List<CatchFlow> catches, FlowBlock finallyBlock) {
            this.tryBlock = tryBlock == null ? FlowBlock.empty() : tryBlock;
            this.catches = catches == null ? List.of() : catches;
            this.finallyBlock = finallyBlock;
        }

        FlowBlock tryBlock() {
            return tryBlock;
        }

        List<CatchFlow> catches() {
            return catches;
        }

        FlowBlock finallyBlock() {
            return finallyBlock;
        }

        @Override
        public boolean terminates() {
            boolean tryTerminates = tryBlock.isTerminated();
            boolean catchesTerminate = !catches.isEmpty() && catches.stream().allMatch(catchFlow -> catchFlow.body().isTerminated());
            if (finallyBlock != null && finallyBlock.isTerminated()) {
                return true;
            }
            return tryTerminates && catchesTerminate;
        }
    }

    static final class CatchFlow {
        private final String label;
        private final FlowBlock body;

        CatchFlow(String label, FlowBlock body) {
            this.label = label;
            this.body = body == null ? FlowBlock.empty() : body;
        }

        String label() {
            return label;
        }

        FlowBlock body() {
            return body;
        }
    }

    static final class DispatchFlow implements FlowElement {
        private final String sourceClass;
        private final String targetClass;
        private final String methodName;
        private final List<DispatchCase> cases;

        DispatchFlow(String sourceClass, String targetClass, String methodName, List<DispatchCase> cases) {
            this.sourceClass = sourceClass;
            this.targetClass = targetClass;
            this.methodName = methodName;
            this.cases = cases == null ? List.of() : cases;
        }

        String sourceClass() {
            return sourceClass;
        }

        String targetClass() {
            return targetClass;
        }

        String methodName() {
            return methodName;
        }

        List<DispatchCase> cases() {
            return cases;
        }

        @Override
        public boolean terminates() {
            return cases.stream().allMatch(dispatchCase -> dispatchCase.body().isTerminated());
        }
    }

    static final class DispatchCase {
        private final String label;
        private final FlowBlock body;

        DispatchCase(String label, FlowBlock body) {
            this.label = label;
            this.body = body == null ? FlowBlock.empty() : body;
        }

        String label() {
            return label;
        }

        FlowBlock body() {
            return body;
        }
    }

    static final class GroupFlow implements FlowElement {
        private final String label;
        private final FlowBlock body;

        GroupFlow(String label, FlowBlock body) {
            this.label = label;
            this.body = body == null ? FlowBlock.empty() : body;
        }

        String label() {
            return label;
        }

        FlowBlock body() {
            return body;
        }

        @Override
        public boolean terminates() {
            return body.isTerminated();
        }
    }

    static final class NoteFlow implements FlowElement {
        private final String message;

        NoteFlow(String message) {
            this.message = message;
        }

        String message() {
            return message;
        }

        @Override
        public boolean terminates() {
            return false;
        }
    }

    static final class ReturnFlow implements FlowElement {
        @Override
        public boolean terminates() {
            return true;
        }
    }

    static final class ThrowFlow implements FlowElement {
        @Override
        public boolean terminates() {
            return true;
        }
    }

    static final class BreakFlow implements FlowElement {
        @Override
        public boolean terminates() {
            return true;
        }
    }

    static final class ContinueFlow implements FlowElement {
        @Override
        public boolean terminates() {
            return true;
        }
    }
}
