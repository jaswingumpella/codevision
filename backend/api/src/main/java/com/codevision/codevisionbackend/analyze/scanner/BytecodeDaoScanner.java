package com.codevision.codevisionbackend.analyze.scanner;

import com.codevision.codevisionbackend.analysis.ClasspathBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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

@Component
public class BytecodeDaoScanner {

    private static final Logger log = LoggerFactory.getLogger(BytecodeDaoScanner.class);
    private static final Set<String> DB_OWNER_PREFIXES = Set.of(
            "org.hibernate.",
            "jakarta.persistence.",
            "javax.persistence.",
            "org.springframework.jdbc.");
    private static final Set<String> INSERT_UPDATE_METHODS =
            Set.of("save", "saveorupdate", "persist", "merge");
    private static final Set<String> UPDATE_METHODS =
            Set.of("update", "flush", "executeupdate");
    private static final Set<String> DELETE_METHODS = Set.of("delete", "remove");
    private static final Set<String> SELECT_METHODS = Set.of(
            "find",
            "get",
            "load",
            "list",
            "getresultlist",
            "getsingleresult",
            "uniqueresult",
            "createquery",
            "createnativequery",
            "createsqlquery",
            "createcriteria",
            "getcriteriabuilder",
            "createcriteriabuilder");

    public Map<String, List<DaoOperationRecord>> scan(
            ClasspathBuilder.ClasspathDescriptor descriptor,
            List<String> acceptPackages,
            Map<String, DbEntityRecord> entities) {
        Map<String, List<DaoOperationRecord>> operationsByClass = new HashMap<>();
        if (descriptor == null) {
            return operationsByClass;
        }
        for (Path entry : descriptor.getClasspathEntries()) {
            try {
                if (Files.isDirectory(entry)) {
                    scanDirectory(entry, acceptPackages, entities, operationsByClass);
                } else if (Files.isRegularFile(entry) && entry.toString().endsWith(".jar")) {
                    scanJar(entry, acceptPackages, entities, operationsByClass);
                }
            } catch (IOException ex) {
                log.warn("Failed scanning {} for bytecode DAO operations: {}", entry, ex.getMessage());
            }
        }
        return operationsByClass;
    }

    private void scanDirectory(
            Path directory,
            List<String> acceptPackages,
            Map<String, DbEntityRecord> entities,
            Map<String, List<DaoOperationRecord>> operationsByClass)
            throws IOException {
        Files.walk(directory)
                .filter(path -> path.toString().endsWith(".class"))
                .forEach(classFile -> {
                    try (InputStream inputStream = Files.newInputStream(classFile)) {
                        parseClass(inputStream.readAllBytes(), acceptPackages, entities, operationsByClass);
                    } catch (IOException ex) {
                        log.debug("Failed to parse class {}: {}", classFile, ex.getMessage());
                    }
                });
    }

    private void scanJar(
            Path jarPath,
            List<String> acceptPackages,
            Map<String, DbEntityRecord> entities,
            Map<String, List<DaoOperationRecord>> operationsByClass)
            throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }
                try (InputStream inputStream = jarFile.getInputStream(entry)) {
                    parseClass(inputStream.readAllBytes(), acceptPackages, entities, operationsByClass);
                } catch (IOException ex) {
                    log.debug("Failed to parse {} from {}: {}", entry.getName(), jarPath, ex.getMessage());
                }
            }
        }
    }

    private void parseClass(
            byte[] bytes,
            List<String> acceptPackages,
            Map<String, DbEntityRecord> entities,
            Map<String, List<DaoOperationRecord>> operationsByClass) {
        ClassReader reader = new ClassReader(bytes);
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            private String currentClassName;
            private boolean includeClass;

            @Override
            public void visit(
                    int version,
                    int access,
                    String name,
                    String signature,
                    String superName,
                    String[] interfaces) {
                this.currentClassName = name.replace('/', '.');
                this.includeClass = shouldIncludeClass(this.currentClassName, acceptPackages);
            }

            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                if (!includeClass || name.startsWith("<")) {
                    return null;
                }
                String callerMethodName = name;
                return new MethodVisitor(Opcodes.ASM9) {
                    private String operationType;

                    @Override
                    public void visitMethodInsn(
                            int opcode,
                            String owner,
                            String methodName,
                            String methodDescriptor,
                            boolean isInterface) {
                        String ownerClass = owner.replace('/', '.');
                        if (!isDbOwner(ownerClass)) {
                            return;
                        }
                        String detected = classifyOperation(methodName);
                        if (detected == null) {
                            return;
                        }
                        operationType = mergeOperation(operationType, detected);
                    }

                    @Override
                    public void visitEnd() {
                        if (operationType == null) {
                            return;
                        }
                        String targetDescriptor = resolveTargetDescriptor(currentClassName, entities);
                        DaoOperationRecord record = new DaoOperationRecord(
                                currentClassName,
                                callerMethodName,
                                operationType,
                                targetDescriptor,
                                null);
                        operationsByClass
                                .computeIfAbsent(currentClassName, key -> new ArrayList<>())
                                .add(record);
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    private boolean shouldIncludeClass(String className, List<String> acceptPackages) {
        if (className == null || AnalysisExclusions.isMockClassName(className)) {
            return false;
        }
        if (acceptPackages == null || acceptPackages.isEmpty()) {
            return true;
        }
        for (String prefix : acceptPackages) {
            if (prefix == null || prefix.isBlank()) {
                continue;
            }
            if (className.equals(prefix)
                    || className.startsWith(prefix + ".")
                    || className.startsWith(prefix + "$")) {
                return true;
            }
        }
        return false;
    }

    private boolean isDbOwner(String ownerClass) {
        if (ownerClass == null) {
            return false;
        }
        for (String prefix : DB_OWNER_PREFIXES) {
            if (ownerClass.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String classifyOperation(String methodName) {
        if (methodName == null) {
            return null;
        }
        String normalized = methodName.toLowerCase(Locale.ROOT);
        if (DELETE_METHODS.contains(normalized)) {
            return "DELETE";
        }
        if (UPDATE_METHODS.contains(normalized)) {
            return "UPDATE";
        }
        if (INSERT_UPDATE_METHODS.contains(normalized)) {
            return "INSERT_OR_UPDATE";
        }
        if (SELECT_METHODS.contains(normalized)) {
            return "SELECT";
        }
        return null;
    }

    private String mergeOperation(String current, String next) {
        if (current == null) {
            return next;
        }
        if ("DELETE".equals(current) || "DELETE".equals(next)) {
            return "DELETE";
        }
        if ("UPDATE".equals(current) || "UPDATE".equals(next)) {
            return "UPDATE";
        }
        if ("INSERT_OR_UPDATE".equals(current) || "INSERT_OR_UPDATE".equals(next)) {
            return "INSERT_OR_UPDATE";
        }
        return current;
    }

    private String resolveTargetDescriptor(String className, Map<String, DbEntityRecord> entities) {
        if (className == null || className.isBlank()) {
            return className;
        }
        if (entities == null || entities.isEmpty()) {
            return className;
        }
        String simple = simpleName(className);
        String stripped = stripDaoSuffix(simple);
        DbEntityRecord entity = entities.get(stripped);
        if (entity == null) {
            return simple;
        }
        String tableName = entity.tableName();
        if (tableName == null || tableName.isBlank() || tableName.equals(entity.className())) {
            return entity.className();
        }
        return entity.className() + " [" + tableName + "]";
    }

    private String simpleName(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        int lastDot = value.lastIndexOf('.');
        if (lastDot >= 0 && lastDot + 1 < value.length()) {
            return value.substring(lastDot + 1);
        }
        return value;
    }

    private String stripDaoSuffix(String className) {
        if (className == null || className.isBlank()) {
            return className;
        }
        String stripped = className;
        String[] suffixes = {"Impl", "Repository", "Repo", "DAO", "Dao"};
        boolean updated;
        do {
            updated = false;
            for (String suffix : suffixes) {
                if (stripped.endsWith(suffix) && stripped.length() > suffix.length()) {
                    stripped = stripped.substring(0, stripped.length() - suffix.length());
                    updated = true;
                }
            }
        } while (updated);
        return stripped;
    }
}
