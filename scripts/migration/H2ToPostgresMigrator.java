import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * One-off utility that copies all rows from the legacy on-disk H2 database into PostgreSQL.
 *
 * <p>Usage (from repo root):
 *
 * <pre>
 *   export PG_URL=jdbc:postgresql://host:5432/db
 *   export PG_USER=...
 *   export PG_PASSWORD=...
 *   export H2_DB_PATH=./backend/api/data/codevision   # optional
 *   scripts/migration/run-h2-to-postgres.sh
 * </pre>
 *
 * The tool is idempotent thanks to {@code ON CONFLICT DO NOTHING}.
 */
public final class H2ToPostgresMigrator {

    private static final List<String> TABLE_INSERT_ORDER = List.of(
            "project",
            "project_snapshot",
            "class_metadata",
            "api_endpoint",
            "db_entity",
            "dao_operation",
            "asset_image",
            "diagram",
            "log_statement",
            "pii_pci_finding");

    private static final Set<String> TABLES_WITH_IDENTITY =
            Set.of("project", "class_metadata", "api_endpoint", "db_entity", "dao_operation",
                    "asset_image", "diagram", "log_statement", "pii_pci_finding");

    public static void main(String[] args) throws Exception {
        Config config = Config.load();
        try (Connection h2 = DriverManager.getConnection(config.h2Url(), config.h2User(), config.h2Password());
                Connection pg = DriverManager.getConnection(config.pgUrl(), config.pgUser(), config.pgPassword())) {
            pg.setAutoCommit(false);
            Map<String, List<String>> tableColumns = resolveTableColumns(h2);
            for (String table : TABLE_INSERT_ORDER) {
                if (!tableColumns.containsKey(table)) {
                    System.out.printf("Skipping %s because it does not exist in H2%n", table);
                    continue;
                }
                migrateTable(h2, pg, table, tableColumns.get(table));
            }
            syncSequences(pg);
            pg.commit();
            System.out.println("Migration completed successfully.");
        }
    }

    private static Map<String, List<String>> resolveTableColumns(Connection h2) throws SQLException {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (String table : TABLE_INSERT_ORDER) {
            List<String> columns = new ArrayList<>();
            try (PreparedStatement ps = h2.prepareStatement("""
                    SELECT COLUMN_NAME
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ?
                    ORDER BY ORDINAL_POSITION
                    """)) {
                ps.setString(1, table.toUpperCase(Locale.ROOT));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        columns.add(rs.getString(1).toLowerCase(Locale.ROOT));
                    }
                }
            }
            if (!columns.isEmpty()) {
                map.put(table, columns);
            }
        }
        return map;
    }

    private static void migrateTable(Connection h2, Connection pg, String table, List<String> columns)
            throws SQLException {
        String selectSql = "SELECT " + columns.stream()
                .map(col -> "\"" + col.toUpperCase(Locale.ROOT) + "\"")
                .collect(Collectors.joining(", "))
                + " FROM \"" + table.toUpperCase(Locale.ROOT) + "\"";

        String insertSql = "INSERT INTO " + table
                + " (" + String.join(", ", columns) + ") VALUES ("
                + columns.stream().map(c -> "?").collect(Collectors.joining(", "))
                + ") ON CONFLICT DO NOTHING";

        int rowCount = 0;
        try (PreparedStatement select = h2.prepareStatement(selectSql);
                ResultSet rs = select.executeQuery();
                PreparedStatement insert = pg.prepareStatement(insertSql)) {
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                for (int i = 0; i < columns.size(); i++) {
                    Object value = extractValue(rs, meta, i + 1);
                    insert.setObject(i + 1, value);
                }
                insert.addBatch();
                rowCount++;
                if (rowCount % 500 == 0) {
                    insert.executeBatch();
                }
            }
            insert.executeBatch();
        }
        System.out.printf("Migrated %d rows from %s%n", rowCount, table);
    }

    private static Object extractValue(ResultSet rs, ResultSetMetaData meta, int columnIndex) throws SQLException {
        int type = meta.getColumnType(columnIndex);
        return switch (type) {
            case Types.CLOB, Types.LONGVARCHAR -> {
                Clob clob = rs.getClob(columnIndex);
                if (clob == null) {
                    yield null;
                }
                try {
                    yield clob.getSubString(1, (int) clob.length());
                } catch (SQLException e) {
                    throw e;
                }
            }
            case Types.TIMESTAMP_WITH_TIMEZONE -> rs.getObject(columnIndex, OffsetDateTime.class);
            case Types.TIMESTAMP -> {
                java.sql.Timestamp ts = rs.getTimestamp(columnIndex);
                yield ts == null ? null : ts.toInstant().atOffset(ZoneOffset.UTC);
            }
            default -> rs.getObject(columnIndex);
        };
    }

    private static void syncSequences(Connection pg) throws SQLException {
        for (String table : TABLES_WITH_IDENTITY) {
            String sql = """
                    SELECT setval(
                        pg_get_serial_sequence(?, 'id'),
                        COALESCE((SELECT MAX(id) FROM %s), 0),
                        true
                    )
                    """.formatted(table);
            try (PreparedStatement ps = pg.prepareStatement(sql)) {
                ps.setString(1, "public." + table);
                ps.execute();
            }
        }
    }

    private record Config(String h2Url, String h2User, String h2Password,
                          String pgUrl, String pgUser, String pgPassword) {

        static Config load() {
            Path defaultH2 = Paths.get("backend", "api", "data", "codevision").toAbsolutePath();
            String h2Path = envOrDefault("H2_DB_PATH", defaultH2.toString());
            String pgUrl = envRequired("PG_URL",
                    "Set PG_URL (e.g. jdbc:postgresql://host:5432/codevision_postgres)");
            String pgUser = envRequired("PG_USER", "Set PG_USER (database username)");
            String pgPassword = envRequired("PG_PASSWORD", "Set PG_PASSWORD (database password)");
            return new Config(
                    "jdbc:h2:file:%s;MODE=PostgreSQL".formatted(h2Path),
                    envOrDefault("H2_DB_USER", "sa"),
                    envOrDefault("H2_DB_PASSWORD", ""),
                    pgUrl,
                    pgUser,
                    pgPassword);
        }

        private static String envOrDefault(String key, String fallback) {
            return System.getenv().getOrDefault(key, fallback);
        }

        private static String envRequired(String key, String message) {
            String value = System.getenv(key);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException(message);
            }
            return value;
        }
    }
}
