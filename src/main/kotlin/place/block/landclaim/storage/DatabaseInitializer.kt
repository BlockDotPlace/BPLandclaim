package place.block.landclaim.storage

import java.sql.Connection

class DatabaseInitializer {
    fun initialize(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.executeBatch(
                """
                PRAGMA foreign_keys = ON;

                CREATE TABLE IF NOT EXISTS claims (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    world_id TEXT NOT NULL,
                    owner_uuid TEXT NOT NULL,
                    min_x INTEGER NOT NULL,
                    max_x INTEGER NOT NULL,
                    min_z INTEGER NOT NULL,
                    max_z INTEGER NOT NULL,
                    allow_explosions INTEGER NOT NULL DEFAULT 1,
                    allow_pvp INTEGER NOT NULL DEFAULT 1,
                    created_at TEXT NOT NULL
                );

                CREATE TABLE IF NOT EXISTS claim_permissions (
                    claim_id INTEGER NOT NULL,
                    player_uuid TEXT NOT NULL,
                    block_mutation INTEGER NOT NULL,
                    block_use INTEGER NOT NULL,
                    entity_damage INTEGER NOT NULL DEFAULT 1,
                    PRIMARY KEY (claim_id, player_uuid),
                    FOREIGN KEY (claim_id) REFERENCES claims(id) ON DELETE CASCADE
                );

                CREATE INDEX IF NOT EXISTS idx_claims_owner_uuid
                    ON claims(owner_uuid);

                CREATE INDEX IF NOT EXISTS idx_claims_world_bounds
                    ON claims(world_id, min_x, max_x, min_z, max_z);

                CREATE INDEX IF NOT EXISTS idx_permissions_player_uuid
                    ON claim_permissions(player_uuid);
                """.trimIndent(),
            )
        }

        ensureClaimColumn(connection, "allow_explosions", "INTEGER NOT NULL DEFAULT 1")
        ensureClaimColumn(connection, "allow_pvp", "INTEGER NOT NULL DEFAULT 1")
        ensureClaimPermissionColumn(connection, "entity_damage", "INTEGER NOT NULL DEFAULT 1")
    }

    private fun ensureClaimColumn(connection: Connection, columnName: String, columnDefinition: String) {
        val existingColumns = connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA table_info(claims)").use { resultSet ->
                buildSet {
                    while (resultSet.next()) {
                        add(resultSet.getString("name"))
                    }
                }
            }
        }

        if (columnName !in existingColumns) {
            connection.createStatement().use { statement ->
                statement.execute("ALTER TABLE claims ADD COLUMN $columnName $columnDefinition")
            }
        }
    }

    private fun ensureClaimPermissionColumn(connection: Connection, columnName: String, columnDefinition: String) {
        val existingColumns = connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA table_info(claim_permissions)").use { resultSet ->
                buildSet {
                    while (resultSet.next()) {
                        add(resultSet.getString("name"))
                    }
                }
            }
        }

        if (columnName !in existingColumns) {
            connection.createStatement().use { statement ->
                statement.execute("ALTER TABLE claim_permissions ADD COLUMN $columnName $columnDefinition")
            }
        }
    }

    private fun java.sql.Statement.executeBatch(sql: String) {
        sql.split(';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach(::addBatch)
        executeBatch()
    }
}
