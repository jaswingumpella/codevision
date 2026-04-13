package com.codevision.codevisionbackend.analysis.multilang;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Singleton registry that maps file extensions and canonical names to
 * {@link LanguageDefinition} instances. Supports 50+ programming languages.
 */
@Component
public class LanguageRegistry {

    private final Map<String, LanguageDefinition> byExtension;
    private final Map<String, LanguageDefinition> byName;

    public LanguageRegistry() {
        var extensionMap = new LinkedHashMap<String, LanguageDefinition>();
        var nameMap = new LinkedHashMap<String, LanguageDefinition>();

        register(extensionMap, nameMap, "java", "Java", List.of(".java"), "java");
        register(extensionMap, nameMap, "typescript", "TypeScript", List.of(".ts", ".tsx"), "typescript");
        register(extensionMap, nameMap, "javascript", "JavaScript", List.of(".js", ".jsx"), "javascript");
        register(extensionMap, nameMap, "python", "Python", List.of(".py"), "python");
        register(extensionMap, nameMap, "go", "Go", List.of(".go"), "go");
        register(extensionMap, nameMap, "rust", "Rust", List.of(".rs"), "rust");
        register(extensionMap, nameMap, "csharp", "C#", List.of(".cs"), "c_sharp");
        register(extensionMap, nameMap, "kotlin", "Kotlin", List.of(".kt", ".kts"), "kotlin");
        register(extensionMap, nameMap, "ruby", "Ruby", List.of(".rb"), "ruby");
        register(extensionMap, nameMap, "swift", "Swift", List.of(".swift"), "swift");
        register(extensionMap, nameMap, "cpp", "C++", List.of(".cpp", ".cc", ".cxx", ".hpp", ".hxx"), "cpp");
        register(extensionMap, nameMap, "c", "C", List.of(".c", ".h"), "c");
        register(extensionMap, nameMap, "scala", "Scala", List.of(".scala"), "scala");
        register(extensionMap, nameMap, "dart", "Dart", List.of(".dart"), "dart");
        register(extensionMap, nameMap, "lua", "Lua", List.of(".lua"), "lua");
        register(extensionMap, nameMap, "php", "PHP", List.of(".php"), "php");
        register(extensionMap, nameMap, "r", "R", List.of(".r", ".R"), "r");
        register(extensionMap, nameMap, "julia", "Julia", List.of(".jl"), "julia");
        register(extensionMap, nameMap, "elixir", "Elixir", List.of(".ex", ".exs"), "elixir");
        register(extensionMap, nameMap, "haskell", "Haskell", List.of(".hs", ".lhs"), "haskell");
        register(extensionMap, nameMap, "ocaml", "OCaml", List.of(".ml", ".mli"), "ocaml");
        register(extensionMap, nameMap, "bash", "Bash", List.of(".sh", ".bash"), "bash");
        register(extensionMap, nameMap, "sql", "SQL", List.of(".sql"), "sql");
        register(extensionMap, nameMap, "html", "HTML", List.of(".html", ".htm"), "html");
        register(extensionMap, nameMap, "css", "CSS", List.of(".css"), "css");
        register(extensionMap, nameMap, "json", "JSON", List.of(".json"), "json");
        register(extensionMap, nameMap, "yaml", "YAML", List.of(".yml", ".yaml"), "yaml");
        register(extensionMap, nameMap, "toml", "TOML", List.of(".toml"), "toml");
        register(extensionMap, nameMap, "xml", "XML", List.of(".xml"), "xml");
        register(extensionMap, nameMap, "markdown", "Markdown", List.of(".md", ".markdown"), "markdown");
        register(extensionMap, nameMap, "dockerfile", "Dockerfile", List.of(".dockerfile"), "dockerfile");
        register(extensionMap, nameMap, "makefile", "Makefile", List.of(".mk"), "make");
        register(extensionMap, nameMap, "cmake", "CMake", List.of(".cmake"), "cmake");
        register(extensionMap, nameMap, "proto", "Protocol Buffers", List.of(".proto"), "proto");
        register(extensionMap, nameMap, "graphql", "GraphQL", List.of(".graphql", ".gql"), "graphql");
        register(extensionMap, nameMap, "terraform", "Terraform", List.of(".tf", ".hcl"), "hcl");
        register(extensionMap, nameMap, "zig", "Zig", List.of(".zig"), "zig");
        register(extensionMap, nameMap, "nim", "Nim", List.of(".nim"), "nim");
        register(extensionMap, nameMap, "clojure", "Clojure", List.of(".clj", ".cljs", ".cljc"), "clojure");
        register(extensionMap, nameMap, "erlang", "Erlang", List.of(".erl", ".hrl"), "erlang");
        register(extensionMap, nameMap, "perl", "Perl", List.of(".pl", ".pm"), "perl");
        register(extensionMap, nameMap, "groovy", "Groovy", List.of(".groovy"), "groovy");
        register(extensionMap, nameMap, "objective-c", "Objective-C", List.of(".m", ".mm"), "objc");
        register(extensionMap, nameMap, "vb", "Visual Basic", List.of(".vb", ".vbs"), "vb");
        register(extensionMap, nameMap, "fsharp", "F#", List.of(".fs", ".fsx"), "fsharp");
        register(extensionMap, nameMap, "powershell", "PowerShell", List.of(".ps1", ".psm1"), "powershell");
        register(extensionMap, nameMap, "coffeescript", "CoffeeScript", List.of(".coffee"), "coffeescript");
        register(extensionMap, nameMap, "elm", "Elm", List.of(".elm"), "elm");
        register(extensionMap, nameMap, "purescript", "PureScript", List.of(".purs"), "purescript");
        register(extensionMap, nameMap, "solidity", "Solidity", List.of(".sol"), "solidity");

        this.byExtension = Collections.unmodifiableMap(extensionMap);
        this.byName = Collections.unmodifiableMap(nameMap);
    }

    /**
     * Detects the language of a file based on its filename extension.
     *
     * @param filename the filename or path to inspect
     * @return the matching definition, or empty if the extension is unrecognised
     */
    public Optional<LanguageDefinition> detect(String filename) {
        if (filename == null) {
            return Optional.empty();
        }
        var lastDot = filename.lastIndexOf('.');
        if (lastDot < 0) {
            // Handle extensionless files by basename
            var baseName = filename.contains("/") ? filename.substring(filename.lastIndexOf('/') + 1) : filename;
            return switch (baseName.toLowerCase()) {
                case "dockerfile" -> Optional.ofNullable(byName.get("dockerfile"));
                case "makefile" -> Optional.ofNullable(byName.get("makefile"));
                default -> Optional.empty();
            };
        }
        var extension = filename.substring(lastDot);
        return Optional.ofNullable(byExtension.get(extension));
    }

    /**
     * Looks up a language by its canonical name.
     *
     * @param name canonical lowercase name (e.g. "java", "typescript")
     * @return the matching definition, or empty if unknown
     */
    public Optional<LanguageDefinition> getLanguage(String name) {
        return Optional.ofNullable(byName.get(name));
    }

    /**
     * Returns all registered language definitions.
     *
     * @return unmodifiable collection of definitions
     */
    public Collection<LanguageDefinition> allLanguages() {
        return byName.values();
    }

    /**
     * Returns all file extensions that map to a known language.
     *
     * @return unmodifiable set of extensions including the dot prefix
     */
    public Set<String> supportedExtensions() {
        return byExtension.keySet();
    }

    private static void register(
            Map<String, LanguageDefinition> extensionMap,
            Map<String, LanguageDefinition> nameMap,
            String name, String displayName, List<String> extensions, String treeSitterLanguage) {

        var def = new LanguageDefinition(name, displayName, extensions, treeSitterLanguage);
        nameMap.put(name, def);
        for (var ext : extensions) {
            extensionMap.put(ext, def);
        }
    }
}
