package place.block.landclaim.storage

import org.sqlite.SQLiteDataSource
import place.block.landclaim.Landclaim
import java.nio.file.Path
import java.sql.Connection

class DatabaseManager(
    private val plugin: Landclaim,
    private val initializer: DatabaseInitializer = DatabaseInitializer(),
) : AutoCloseable {
    private lateinit var dataSource: SQLiteDataSource
    val databasePath: Path
        get() = plugin.dataFolder.toPath().resolve(DATABASE_FILE_NAME)

    fun start() {
        plugin.dataFolder.mkdirs()

        dataSource = SQLiteDataSource().apply {
            url = "jdbc:sqlite:${databasePath.toAbsolutePath()}"
        }

        connection().use(initializer::initialize)
    }

    fun connection(): Connection {
        check(::dataSource.isInitialized) { "DatabaseManager has not been started." }
        return dataSource.connection.apply {
            autoCommit = true
        }
    }

    override fun close() {
        if (::dataSource.isInitialized) {
            dataSource = SQLiteDataSource()
        }
    }

    private companion object {
        const val DATABASE_FILE_NAME = "landclaim.db"
    }
}
