package place.block.landclaim.storage.sqlite

import place.block.landclaim.storage.ClaimRecord
import place.block.landclaim.storage.DatabaseManager
import place.block.landclaim.storage.repository.ClaimRepository
import java.sql.Statement
import java.time.Instant
import java.util.UUID

class SqliteClaimRepository(
    private val databaseManager: DatabaseManager,
) : ClaimRepository {
    override fun create(
        worldId: String,
        ownerUuid: UUID,
        minX: Int,
        maxX: Int,
        minZ: Int,
        maxZ: Int,
    ): ClaimRecord {
        val sql =
            """
            INSERT INTO claims (world_id, owner_uuid, min_x, max_x, min_z, max_z, allow_explosions, allow_pvp, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

        return databaseManager.connection().use { connection ->
            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { statement ->
                val createdAt = Instant.now()
                statement.setString(1, worldId)
                statement.setString(2, ownerUuid.toString())
                statement.setInt(3, minX)
                statement.setInt(4, maxX)
                statement.setInt(5, minZ)
                statement.setInt(6, maxZ)
                statement.setBoolean(7, true)
                statement.setBoolean(8, true)
                statement.setString(9, createdAt.toString())
                statement.executeUpdate()

                val claimId = statement.generatedKeys.use { generatedKeys ->
                    check(generatedKeys.next()) { "SQLite did not return a generated claim id." }
                    generatedKeys.getLong(1)
                }

                ClaimRecord(
                    id = claimId,
                    worldId = worldId,
                    ownerUuid = ownerUuid,
                    minX = minX,
                    maxX = maxX,
                    minZ = minZ,
                    maxZ = maxZ,
                    allowExplosions = true,
                    allowPvp = true,
                    createdAt = createdAt,
                )
            }
        }
    }

    override fun updateBounds(claimId: Long, minX: Int, maxX: Int, minZ: Int, maxZ: Int): Boolean {
        val sql =
            """
            UPDATE claims
            SET min_x = ?, max_x = ?, min_z = ?, max_z = ?
            WHERE id = ?
            """.trimIndent()

        return databaseManager.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setInt(1, minX)
                statement.setInt(2, maxX)
                statement.setInt(3, minZ)
                statement.setInt(4, maxZ)
                statement.setLong(5, claimId)
                statement.executeUpdate() > 0
            }
        }
    }

    override fun updateAttributes(claimId: Long, allowExplosions: Boolean, allowPvp: Boolean): Boolean {
        val sql =
            """
            UPDATE claims
            SET allow_explosions = ?, allow_pvp = ?
            WHERE id = ?
            """.trimIndent()

        return databaseManager.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setBoolean(1, allowExplosions)
                statement.setBoolean(2, allowPvp)
                statement.setLong(3, claimId)
                statement.executeUpdate() > 0
            }
        }
    }

    override fun delete(claimId: Long): Boolean {
        val sql = "DELETE FROM claims WHERE id = ?"
        return databaseManager.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, claimId)
                statement.executeUpdate() > 0
            }
        }
    }

    override fun countByOwner(ownerUuid: UUID): Int {
        val sql = "SELECT COUNT(*) FROM claims WHERE owner_uuid = ?"
        return databaseManager.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, ownerUuid.toString())
                statement.executeQuery().use { resultSet ->
                    check(resultSet.next()) { "COUNT query returned no rows." }
                    resultSet.getInt(1)
                }
            }
        }
    }

    override fun sumAreaByOwner(ownerUuid: UUID): Int {
        val sql =
            """
            SELECT COALESCE(SUM(((max_x - min_x) + 1) * ((max_z - min_z) + 1)), 0)
            FROM claims
            WHERE owner_uuid = ?
            """.trimIndent()

        return databaseManager.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, ownerUuid.toString())
                statement.executeQuery().use { resultSet ->
                    check(resultSet.next()) { "SUM query returned no rows." }
                    resultSet.getInt(1)
                }
            }
        }
    }

    override fun findAll(): List<ClaimRecord> {
        val sql =
            """
            SELECT id, world_id, owner_uuid, min_x, max_x, min_z, max_z, allow_explosions, allow_pvp, created_at
            FROM claims
            ORDER BY id
            """.trimIndent()

        return queryMany(sql) { }
    }

    override fun findContaining(worldId: String, x: Int, z: Int): ClaimRecord? {
        val sql =
            """
            SELECT id, world_id, owner_uuid, min_x, max_x, min_z, max_z, allow_explosions, allow_pvp, created_at
            FROM claims
            WHERE world_id = ?
              AND ? BETWEEN min_x AND max_x
              AND ? BETWEEN min_z AND max_z
            LIMIT 1
            """.trimIndent()

        return querySingle(sql) { statement ->
            statement.setString(1, worldId)
            statement.setInt(2, x)
            statement.setInt(3, z)
        }
    }

    override fun findById(claimId: Long): ClaimRecord? {
        val sql =
            """
            SELECT id, world_id, owner_uuid, min_x, max_x, min_z, max_z, allow_explosions, allow_pvp, created_at
            FROM claims
            WHERE id = ?
            """.trimIndent()

        return querySingle(sql) { statement ->
            statement.setLong(1, claimId)
        }
    }

    override fun findNear(worldId: String, x: Int, z: Int, radius: Int): List<ClaimRecord> {
        val sql =
            """
            SELECT id, world_id, owner_uuid, min_x, max_x, min_z, max_z, allow_explosions, allow_pvp, created_at
            FROM claims
            WHERE world_id = ?
              AND max_x >= ?
              AND min_x <= ?
              AND max_z >= ?
              AND min_z <= ?
            ORDER BY id
            """.trimIndent()

        return queryMany(sql) { statement ->
            statement.setString(1, worldId)
            statement.setInt(2, x - radius)
            statement.setInt(3, x + radius)
            statement.setInt(4, z - radius)
            statement.setInt(5, z + radius)
        }
    }

    override fun findOverlapping(
        worldId: String,
        minX: Int,
        maxX: Int,
        minZ: Int,
        maxZ: Int,
        ignoredClaimId: Long?,
    ): List<ClaimRecord> {
        val ignoredClause = if (ignoredClaimId != null) "AND id <> ?" else ""
        val sql =
            """
            SELECT id, world_id, owner_uuid, min_x, max_x, min_z, max_z, allow_explosions, allow_pvp, created_at
            FROM claims
            WHERE world_id = ?
              AND min_x <= ?
              AND max_x >= ?
              AND min_z <= ?
              AND max_z >= ?
              $ignoredClause
            ORDER BY id
            """.trimIndent()

        return queryMany(sql) { statement ->
            statement.setString(1, worldId)
            statement.setInt(2, maxX)
            statement.setInt(3, minX)
            statement.setInt(4, maxZ)
            statement.setInt(5, minZ)
            if (ignoredClaimId != null) {
                statement.setLong(6, ignoredClaimId)
            }
        }
    }

    private fun querySingle(
        sql: String,
        binder: (java.sql.PreparedStatement) -> Unit,
    ): ClaimRecord? {
        return databaseManager.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                binder(statement)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) resultSet.toClaimRecord() else null
                }
            }
        }
    }

    private fun queryMany(
        sql: String,
        binder: (java.sql.PreparedStatement) -> Unit,
    ): List<ClaimRecord> {
        return databaseManager.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                binder(statement)
                statement.executeQuery().use { resultSet ->
                    buildList {
                        while (resultSet.next()) {
                            add(resultSet.toClaimRecord())
                        }
                    }
                }
            }
        }
    }
}

private fun java.sql.ResultSet.toClaimRecord(): ClaimRecord {
    return ClaimRecord(
        id = getLong("id"),
        worldId = getString("world_id"),
        ownerUuid = UUID.fromString(getString("owner_uuid")),
        minX = getInt("min_x"),
        maxX = getInt("max_x"),
        minZ = getInt("min_z"),
        maxZ = getInt("max_z"),
        allowExplosions = getBoolean("allow_explosions"),
        allowPvp = getBoolean("allow_pvp"),
        createdAt = Instant.parse(getString("created_at")),
    )
}
