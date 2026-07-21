package place.block.landclaim

import org.bukkit.plugin.java.JavaPlugin
import place.block.landclaim.claim.access.ClaimAccessService
import place.block.landclaim.claim.ClaimValidator
import place.block.landclaim.claim.budget.ClaimBlockBudgetService
import place.block.landclaim.claim.budget.ClaimOperationPreviewService
import place.block.landclaim.claim.create.ClaimCreationService
import place.block.landclaim.claim.resize.ClaimResizeService
import place.block.landclaim.claim.session.ClaimSessionManager
import place.block.landclaim.claim.session.OwnedClaimCornerResolver
import place.block.landclaim.command.ClaimCommandExecutor
import place.block.landclaim.command.ClaimCullingService
import place.block.landclaim.command.ClaimCommandService
import place.block.landclaim.config.ConfigLoader
import place.block.landclaim.config.InvalidPluginConfigException
import place.block.landclaim.config.LandclaimConfig
import place.block.landclaim.config.PluginReloadService
import place.block.landclaim.listener.ClaimCreationListener
import place.block.landclaim.listener.ClaimManagementUiListener
import place.block.landclaim.listener.ClaimProtectionListener
import place.block.landclaim.listener.ClaimVisualizationListener
import place.block.landclaim.playtime.PlayerPlaytimeService
import place.block.landclaim.storage.DatabaseManager
import place.block.landclaim.storage.StorageBootstrapException
import place.block.landclaim.storage.repository.ClaimPermissionRepository
import place.block.landclaim.storage.repository.ClaimRepository
import place.block.landclaim.storage.sqlite.SqliteClaimPermissionRepository
import place.block.landclaim.storage.sqlite.SqliteClaimRepository
import place.block.landclaim.ui.ClaimManagementUiService
import place.block.landclaim.ui.UiIllegalItemService
import place.block.landclaim.visualization.ClaimOperationHudService
import place.block.landclaim.visualization.ClaimVisualizationService

class Landclaim : JavaPlugin() {
    lateinit var landclaimConfig: LandclaimConfig
        private set
    lateinit var databaseManager: DatabaseManager
        private set
    lateinit var claimRepository: ClaimRepository
        private set
    lateinit var claimPermissionRepository: ClaimPermissionRepository
        private set
    lateinit var claimValidator: ClaimValidator
        private set
    lateinit var claimSessionManager: ClaimSessionManager
        private set
    lateinit var ownedClaimCornerResolver: OwnedClaimCornerResolver
        private set
    lateinit var claimCreationService: ClaimCreationService
        private set
    lateinit var claimAccessService: ClaimAccessService
        private set
    lateinit var claimCommandService: ClaimCommandService
        private set
    lateinit var claimResizeService: ClaimResizeService
        private set
    lateinit var claimVisualizationService: ClaimVisualizationService
        private set
    lateinit var claimCullingService: ClaimCullingService
        private set
    lateinit var playerPlaytimeService: PlayerPlaytimeService
        private set
    lateinit var claimBlockBudgetService: ClaimBlockBudgetService
        private set
    lateinit var claimOperationPreviewService: ClaimOperationPreviewService
        private set
    lateinit var claimOperationHudService: ClaimOperationHudService
        private set
    lateinit var claimManagementUiService: ClaimManagementUiService
        private set
    lateinit var uiIllegalItemService: UiIllegalItemService
        private set
    lateinit var pluginReloadService: PluginReloadService
        private set
    lateinit var claimCreationListener: ClaimCreationListener
        private set

    fun applyReloadedUiConfig(reloadedConfig: LandclaimConfig) {
        claimBlockBudgetService.updateTiers(reloadedConfig.claimBlockTiers)
        claimValidator.updateLimits(
            maxClaimsPerPlayer = reloadedConfig.maxClaims,
            maxClaimWidth = reloadedConfig.maxClaimWidth,
            maxClaimDepth = reloadedConfig.maxClaimDepth,
            maxClaimArea = reloadedConfig.maxClaimArea,
        )
        claimCreationListener.updateHeldItem(reloadedConfig.heldItem)
        claimVisualizationService.updateConfig(
            heldItem = reloadedConfig.heldItem,
            claimVisualizationConfig = reloadedConfig.claimVisualization,
        )
        claimManagementUiService.updateConfig(reloadedConfig.claimManagementUi)
        uiIllegalItemService.updateIllegalMaterials(reloadedConfig.claimManagementUi.illegalMaterials)
        landclaimConfig = reloadedConfig
        claimVisualizationService.refreshAllPlayers(forceResend = true)
    }

    override fun onEnable() {
        landclaimConfig = try {
            ConfigLoader(this).load()
        } catch (exception: InvalidPluginConfigException) {
            logger.severe("Failed to load configuration: ${exception.message}")
            server.pluginManager.disablePlugin(this)
            return
        }

        logger.info(
            "Loaded configuration: held_item_id=${landclaimConfig.heldItem.key}, " +
                "max_claims=${landclaimConfig.maxClaims}, " +
                "max_claim_width=${landclaimConfig.maxClaimWidth ?: "disabled"}, " +
                "max_claim_depth=${landclaimConfig.maxClaimDepth ?: "disabled"}, " +
                "max_claim_area=${landclaimConfig.maxClaimArea ?: "disabled"}, " +
                "claim_block_tiers=${landclaimConfig.claimBlockTiers.size}",
        )

        try {
            databaseManager = DatabaseManager(this).also(DatabaseManager::start)
            claimRepository = SqliteClaimRepository(databaseManager)
            claimPermissionRepository = SqliteClaimPermissionRepository(databaseManager)
            playerPlaytimeService = PlayerPlaytimeService(server)
            claimBlockBudgetService = ClaimBlockBudgetService(
                claimRepository = claimRepository,
                playerPlaytimeService = playerPlaytimeService,
                tiers = landclaimConfig.claimBlockTiers,
            )
            claimValidator = ClaimValidator(
                claimRepository = claimRepository,
                claimBlockBudgetService = claimBlockBudgetService,
                maxClaimsPerPlayer = landclaimConfig.maxClaims,
                maxClaimWidth = landclaimConfig.maxClaimWidth,
                maxClaimDepth = landclaimConfig.maxClaimDepth,
                maxClaimArea = landclaimConfig.maxClaimArea,
            )
            claimSessionManager = ClaimSessionManager()
            claimOperationPreviewService = ClaimOperationPreviewService(
                claimSessionManager = claimSessionManager,
                claimRepository = claimRepository,
                claimValidator = claimValidator,
                claimBlockBudgetService = claimBlockBudgetService,
            )
            ownedClaimCornerResolver = OwnedClaimCornerResolver(claimRepository)
            claimCreationService = ClaimCreationService(
                claimRepository = claimRepository,
                claimValidator = claimValidator,
                claimSessionManager = claimSessionManager,
                claimBlockBudgetService = claimBlockBudgetService,
                server = server,
            )
            claimAccessService = ClaimAccessService(
                claimRepository = claimRepository,
                claimPermissionRepository = claimPermissionRepository,
                server = server,
            )
            claimResizeService = ClaimResizeService(
                claimRepository = claimRepository,
                claimValidator = claimValidator,
                claimSessionManager = claimSessionManager,
                ownedClaimCornerResolver = ownedClaimCornerResolver,
                claimBlockBudgetService = claimBlockBudgetService,
                server = server,
                logger = logger,
            )
            claimCullingService = ClaimCullingService(
                claimRepository = claimRepository,
                playerPlaytimeService = playerPlaytimeService,
            )
            claimCommandService = ClaimCommandService(
                claimRepository = claimRepository,
                claimPermissionRepository = claimPermissionRepository,
                claimSessionManager = claimSessionManager,
                server = server,
                claimCullingService = claimCullingService,
                claimBlockBudgetService = claimBlockBudgetService,
            )
            claimVisualizationService = ClaimVisualizationService(
                plugin = this,
                heldItem = landclaimConfig.heldItem,
                claimVisualizationConfig = landclaimConfig.claimVisualization,
                claimRepository = claimRepository,
                claimSessionManager = claimSessionManager,
            )
            claimOperationHudService = ClaimOperationHudService(
                plugin = this,
                claimSessionManager = claimSessionManager,
                previewService = claimOperationPreviewService,
            )
            claimManagementUiService = ClaimManagementUiService(
                claimRepository = claimRepository,
                claimPermissionRepository = claimPermissionRepository,
                claimCommandService = claimCommandService,
                server = server,
                uiConfig = landclaimConfig.claimManagementUi,
            )
            uiIllegalItemService = UiIllegalItemService(
                illegalMaterials = landclaimConfig.claimManagementUi.illegalMaterials,
            )
            pluginReloadService = PluginReloadService(this)
        } catch (exception: Exception) {
            val wrapped = StorageBootstrapException("Failed to initialize SQLite storage.", exception)
            logger.severe("${wrapped.message} ${exception.message}")
            server.pluginManager.disablePlugin(this)
            return
        }

        claimCreationListener = ClaimCreationListener(
            heldItem = landclaimConfig.heldItem,
            claimCreationService = claimCreationService,
            claimResizeService = claimResizeService,
            claimSessionManager = claimSessionManager,
            claimVisualizationService = claimVisualizationService,
            claimOperationHudService = claimOperationHudService,
        )
        server.pluginManager.registerEvents(
            claimCreationListener,
            this,
        )
        server.pluginManager.registerEvents(
            ClaimProtectionListener(
                claimAccessService = claimAccessService,
            ),
            this,
        )
        server.pluginManager.registerEvents(
            ClaimVisualizationListener(
                claimVisualizationService = claimVisualizationService,
            ),
            this,
        )
        server.pluginManager.registerEvents(
            ClaimManagementUiListener(
                plugin = this,
                claimManagementUiService = claimManagementUiService,
                uiIllegalItemService = uiIllegalItemService,
            ),
            this,
        )
        val claimCommand = getCommand("claim")
        if (claimCommand == null) {
            logger.severe("Failed to register /claim command.")
            server.pluginManager.disablePlugin(this)
            return
        }
        val claimCommandExecutor = ClaimCommandExecutor(
            claimCommandService,
            claimOperationHudService,
            claimManagementUiService,
            pluginReloadService,
        )
        claimCommand.setExecutor(claimCommandExecutor)
        claimCommand.tabCompleter = claimCommandExecutor
        claimVisualizationService.start()
        claimOperationHudService.start()

        logger.info("SQLite storage initialized at ${databaseManager.databasePath.toAbsolutePath()}")
    }

    override fun onDisable() {
        if (::claimVisualizationService.isInitialized) {
            claimVisualizationService.stop()
        }

        if (::claimOperationHudService.isInitialized) {
            claimOperationHudService.stop()
        }

        if (::claimSessionManager.isInitialized) {
            claimSessionManager.clearAll()
        }

        if (::databaseManager.isInitialized) {
            databaseManager.close()
        }

        if (::landclaimConfig.isInitialized) {
            logger.info("Landclaim disabled.")
        }
    }
}
