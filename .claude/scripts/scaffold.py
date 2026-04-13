#!/usr/bin/env python3
"""Code Scaffolding - Generates Java class + test pairs from templates.

Usage:
    python scaffold.py record graph.KgNode
    python scaffold.py service usecase.ImpactAnalysisService
    python scaffold.py algorithm graph.algorithm.PageRankAlgorithm
    python scaffold.py interface dependency.DependencyResolver
"""

import argparse
import os
import sys
from pathlib import Path

BASE_PACKAGE = "com.codevision.codevisionbackend"
MAIN_DIR = "backend/api/src/main/java/com/codevision/codevisionbackend"
TEST_DIR = "backend/api/src/test/java/com/codevision/codevisionbackend"


TEMPLATES = {
    "record": '''package {package};

/**
 * {class_name} - immutable value type.
 */
public record {class_name}(
    // TODO: add fields
    String id
) {{
}}
''',

    "service": '''package {package};

import org.springframework.stereotype.Service;

/**
 * {class_name} - service implementing business logic.
 */
@Service
public class {class_name} {{

    // TODO: inject dependencies via constructor

    // TODO: implement methods
}}
''',

    "algorithm": '''package {package};

import com.codevision.codevisionbackend.graph.KnowledgeGraph;

/**
 * {class_name} - graph algorithm implementation.
 *
 * @param <R> result type
 */
public class {class_name}<R> implements GraphAlgorithm<R> {{

    @Override
    public R execute(KnowledgeGraph graph) {{
        // TODO: implement algorithm
        throw new UnsupportedOperationException("Not yet implemented");
    }}

    @Override
    public String name() {{
        return "{class_name}";
    }}
}}
''',

    "interface": '''package {package};

/**
 * {class_name} - strategy interface.
 */
public interface {class_name} {{

    // TODO: define contract methods
}}
''',

    "test": '''package {package};

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {{@link {target_class}}}.
 */
class {class_name} {{

    @Nested
    class Given_DefaultState {{

        @Nested
        class When_Created {{

            @Test
            void Then_IsNotNull() {{
                // TODO: implement test
                assertNotNull(new Object());
            }}
        }}
    }}
}}
''',
}


def create_file(template_name: str, package_path: str):
    """Generate a Java class from template and its corresponding test."""
    parts = package_path.rsplit(".", 1)
    if len(parts) != 2:
        print(f"ERROR: Expected format 'package.ClassName', got '{package_path}'")
        sys.exit(1)

    sub_package = parts[0]
    class_name = parts[1]
    full_package = f"{BASE_PACKAGE}.{sub_package}"
    dir_path = sub_package.replace(".", "/")

    # Create main class
    main_file = Path(MAIN_DIR) / dir_path / f"{class_name}.java"
    os.makedirs(main_file.parent, exist_ok=True)

    if main_file.exists():
        print(f"SKIP: {main_file} already exists")
    else:
        content = TEMPLATES[template_name].format(
            package=full_package,
            class_name=class_name,
        )
        main_file.write_text(content)
        print(f"CREATED: {main_file}")

    # Create test class
    test_name = f"{class_name}Test"
    test_file = Path(TEST_DIR) / dir_path / f"{test_name}.java"
    os.makedirs(test_file.parent, exist_ok=True)

    if test_file.exists():
        print(f"SKIP: {test_file} already exists")
    else:
        test_content = TEMPLATES["test"].format(
            package=full_package,
            class_name=test_name,
            target_class=class_name,
        )
        test_file.write_text(test_content)
        print(f"CREATED: {test_file}")


def main():
    parser = argparse.ArgumentParser(description="CodeVision Code Scaffolding")
    parser.add_argument("type", choices=["record", "service", "algorithm", "interface"],
                        help="Type of class to generate")
    parser.add_argument("name", help="Package-qualified class name (e.g., graph.KgNode)")

    args = parser.parse_args()
    create_file(args.type, args.name)


if __name__ == "__main__":
    main()
