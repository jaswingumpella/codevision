#!/usr/bin/env node
/**
 * Tree-sitter parse bridge for CodeVision.
 *
 * Usage: node tree-sitter-parse.js <file-path> <language>
 *
 * Parses the source file using the tree-sitter grammar for the given language
 * and outputs a JSON AST to stdout in the format expected by ParsedNode.java.
 *
 * Exit codes:
 *   0 — success (JSON AST on stdout)
 *   1 — error (JSON error object on stdout)
 */
'use strict';

const fs = require('fs');
const path = require('path');

// Map canonical language names to tree-sitter npm package names
const GRAMMAR_PACKAGES = {
  java:        'tree-sitter-java',
  python:      'tree-sitter-python',
  typescript:  'tree-sitter-typescript',
  javascript:  'tree-sitter-javascript',
  go:          'tree-sitter-go',
  rust:        'tree-sitter-rust',
  c_sharp:     'tree-sitter-c-sharp',
  kotlin:      'tree-sitter-kotlin',
  ruby:        'tree-sitter-ruby',
  swift:       'tree-sitter-swift',
  cpp:         'tree-sitter-cpp',
  c:           'tree-sitter-c',
  scala:       'tree-sitter-scala',
  bash:        'tree-sitter-bash',
  php:         'tree-sitter-php',
  hcl:         'tree-sitter-hcl',
  html:        'tree-sitter-html',
  css:         'tree-sitter-css',
  json:        'tree-sitter-json',
  yaml:        'tree-sitter-yaml',
  toml:        'tree-sitter-toml',
  lua:         'tree-sitter-lua',
  r:           'tree-sitter-r',
};

function convertNode(node, sourceLines) {
  const startRow = node.startPosition.row;
  const endRow = node.endPosition.row;
  const startCol = node.startPosition.column;
  const endCol = node.endPosition.column;

  // Extract text for this node from source lines
  let text = '';
  if (startRow === endRow) {
    text = (sourceLines[startRow] || '').substring(startCol, endCol);
  } else {
    const lines = [];
    lines.push((sourceLines[startRow] || '').substring(startCol));
    for (let i = startRow + 1; i < endRow; i++) {
      lines.push(sourceLines[i] || '');
    }
    lines.push((sourceLines[endRow] || '').substring(0, endCol));
    text = lines.join('\n');
  }

  // Limit text length to avoid huge payloads for large nodes
  if (text.length > 500) {
    text = text.substring(0, 500);
  }

  const children = [];
  for (let i = 0; i < node.namedChildCount; i++) {
    children.push(convertNode(node.namedChild(i), sourceLines));
  }

  return {
    type: node.type,
    text: text,
    startLine: startRow + 1,  // Convert 0-based to 1-based
    endLine: endRow + 1,
    startColumn: startCol,
    endColumn: endCol,
    children: children
  };
}

function main() {
  const args = process.argv.slice(2);
  if (args.length < 2) {
    process.stdout.write(JSON.stringify({
      error: 'Usage: tree-sitter-parse.js <file-path> <language>'
    }));
    process.exit(1);
  }

  const filePath = args[0];
  const language = args[1];

  // Read source file
  let source;
  try {
    source = fs.readFileSync(filePath, 'utf8');
  } catch (e) {
    process.stdout.write(JSON.stringify({ error: 'Cannot read file: ' + e.message }));
    process.exit(1);
  }

  const sourceLines = source.split('\n');

  // Load grammar
  const packageName = GRAMMAR_PACKAGES[language];
  if (!packageName) {
    process.stdout.write(JSON.stringify({
      error: 'Unsupported language: ' + language,
      supportedLanguages: Object.keys(GRAMMAR_PACKAGES)
    }));
    process.exit(1);
  }

  let Parser, grammarModule;
  try {
    Parser = require('tree-sitter');
  } catch (e) {
    process.stdout.write(JSON.stringify({ error: 'tree-sitter not installed: ' + e.message }));
    process.exit(1);
  }

  try {
    grammarModule = require(packageName);
  } catch (e) {
    process.stdout.write(JSON.stringify({
      error: 'Grammar not installed: ' + packageName + ' — ' + e.message
    }));
    process.exit(1);
  }

  // Some grammar packages export the language directly, others have sub-languages
  // (e.g., tree-sitter-typescript exports .typescript and .tsx)
  let grammarLang = grammarModule;
  if (language === 'typescript' && grammarModule.typescript) {
    grammarLang = grammarModule.typescript;
  }
  if (language === 'php' && grammarModule.php) {
    grammarLang = grammarModule.php;
  }

  // Parse
  const parser = new Parser();
  parser.setLanguage(grammarLang);
  const tree = parser.parse(source);

  // Convert to our JSON format — output only named children of the root
  const rootNode = tree.rootNode;
  const children = [];
  for (let i = 0; i < rootNode.namedChildCount; i++) {
    children.push(convertNode(rootNode.namedChild(i), sourceLines));
  }

  process.stdout.write(JSON.stringify(children));
  process.exit(0);
}

main();
