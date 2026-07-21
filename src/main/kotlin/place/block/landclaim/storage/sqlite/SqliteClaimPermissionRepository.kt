package place.block.landclaim.storage.sqlite

import place.block.landclaim.storage.ClaimPermissionRecord
import place.block.landclaim.storage.DatabaseManager
import place.block.landclaim.storage.repository.ClaimPermissionRepository
import java.util.UUID

class SqliteClaimPermissionRepository(
    private val databaseManager: DatabaseManager,
) : ClaimPermissionRepository {
    override fun listByClaimId(claimId: Long): List<ClaimPermissionRecord> {
        val sql =
            """
            SELECT claim_id, player_uuid, block_mutation, block_use, entity_damage
            FROM claim_permissions
            WHERE claim_id = ?
            ORDER BY player_uuid
            """.trimIndent()

        return databaseManager.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, claimId)

                statement.executeQuery().use { resultSet ->
                    buildList {
                        while (resultSet.next()) {
                            add(resultSet.toPermissionRecord())
                        }
                    }
                }
            }
        }
    }

    override fun findByClaimIdAndPlayerUuid(claimId: Long, playerUuid: UUID): ClaimPermissionRecord? {
        val sql =
            """
            SELECT claim_id, player_uuid, block_mutation, block_use, entity_damage
            FROM claim_permissions
            WHERE claim_id = ? AND player_uuid = ?
            """.trimIndent()

        return databaseManager.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, claimId)
                statement.setString(2, playerUuid.toString())

                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) resultSet.toPermissionRecord() else null
                }
            }
        }
    }

    override fun upsert(permission: ClaimPermissionRecord) {
        val sql =
            """
            INSERT INTO claim_permissions (claim_id, player_uuid, block_mutation, block_use, entity_damage)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(claim_id, player_uuid) DO UPDATE SET
                block_mutation = excluded.block_mutation,
                block_use = excluded.block_use,
                entity_damage = excluded.entity_damage
            """.trimIndent()

        databaseManager.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, permission.claimId)
                statement.setString(2, permission.playerUuid.toString())
                statement.setBoolean(3, permission.blockMutation)
                statement.setBoolean(4, permission.blockUse)
                statement.setBoolean(5, permission.entityDamage)
                statement.executeUpdate()
            }
        }
    }

    override fun delete(claimId: Long, playerUuid: UUID): Boolean {
        val sql =
            """
            DELETE FROM claim_permissions
            WHERE claim_id = ? AND player_uuid = ?
            """.trimIndent()

        return databaseManager.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, claimId)
                statement.setString(2, playerUuid.toString())
                statement.executeUpdate() > 0
            }
        }
    }
}

private fun java.sql.ResultSet.toPermissionRecord(): ClaimPermissionRecord {
    return ClaimPermissionRecord(
        claimId = getLong("claim_id"),
        playerUuid = UUID.fromString(getString("player_uuid")),
        blockMutation = getBoolean("block_mutation"),
        blockUse = getBoolean("block_use"),
        entityDamage = getBoolean("entity_damage"),
    )
}
