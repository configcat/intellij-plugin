package com.configcat.intellij.plugin.toolWindow.panel

import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.ErrorHandler
import com.configcat.intellij.plugin.actions.*
import com.configcat.intellij.plugin.messaging.ConfigChangeNotifier
import com.configcat.intellij.plugin.messaging.ConnectedConfigChangeNotifier
import com.configcat.intellij.plugin.messaging.TreeChangeNotifier
import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.configcat.intellij.plugin.services.ConfigCatPropertiesService
import com.configcat.intellij.plugin.services.ConfigCatService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.settings.ConfigCatConfigurable
import com.configcat.intellij.plugin.toolWindow.tree.ConfigRootNode
import com.configcat.intellij.plugin.toolWindow.tree.FlagTreeStructure
import com.configcat.publicapi.java.client.ApiException
import com.configcat.publicapi.java.client.model.ConfigModel
import com.configcat.publicapi.java.client.model.SettingModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.awt.CardLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.util.UUID
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel


@Service(Service.Level.PROJECT)
class SettingsPanel(
    private val cs: CoroutineScope,
) : SimpleToolWindowPanel(false, false), Disposable {

    companion object {
        fun getInstance(project: Project): SettingsPanel =
            project.getService(SettingsPanel::class.java)
    }

    private val stateConfig: ConfigCatApplicationConfig.ConfigCatApplicationConfigSate =
        ConfigCatApplicationConfig.getInstance().state

    private val configCatNodeDataService: ConfigCatNodeDataService = ConfigCatNodeDataService.getInstance()
    private val configCatPropertiesService = ConfigCatPropertiesService.getInstance()
    private var tree: Tree? = null
    private var treeModel: StructureTreeModel<FlagTreeStructure>? = null
    private var connectedConfig: ConfigModel? = null

    init {
        setContent(initContent())
        val handleConfigChange = object : ConfigChangeNotifier {
            override fun notifyConfigChange() {
                setContent(initContent())
            }
        }
        ApplicationManager.getApplication().messageBus.connect()
            .subscribe(ConfigChangeNotifier.CONFIG_CHANGE_TOPIC, handleConfigChange)

        val handleConnectedConfigChange = object : ConnectedConfigChangeNotifier {
            override fun notifyConnectedConfigChange() {
                setContent(initContent())
            }
        }
        ApplicationManager.getApplication().messageBus.connect()
            .subscribe(ConnectedConfigChangeNotifier.CONNECTED_CONFIG_CHANGE_TOPIC, handleConnectedConfigChange)

        val handleTreeNotify = object : TreeChangeNotifier {
            override fun notifyTreeRefresh() {
                refreshTree()
            }

            override fun notifyTreeNodeRefresh(node: DefaultMutableTreeNode) {
                refreshTreeNode(node)
            }
        }
        ApplicationManager.getApplication().messageBus.connect()
            .subscribe(TreeChangeNotifier.TREE_REFRESH_TOPIC, handleTreeNotify)
    }

    private fun initContent(): JComponent {
        val content: JComponent = JPanel(CardLayout())
        if (!stateConfig.isConfigured()) {
            content.add(
                //TODO this panel could be common stuff
                JPanel().apply {
                    layout = GridBagLayout()
                    val gbc = GridBagConstraints()
                    gbc.insets = JBUI.insets(1)
                    gbc.gridx = 0
                    gbc.gridy = 0
                    add(JLabel("Please configure the ConfigCat plugin."), gbc)
                    gbc.gridx = 0
                    gbc.gridy = 1

                    add(JButton("Settings").apply {
                        addActionListener {
                            ShowSettingsUtil.getInstance().showSettingsDialog(null, ConfigCatConfigurable::class.java)
                        }
                    }, gbc)
                }
            )
            resetTreeView()
            return content
        } else {
            val connectedConfig = loadConnectedConfig()
            if (connectedConfig == null) {
                content.add(
                    JPanel().apply {
                        layout = GridBagLayout()
                        val gbc = GridBagConstraints()
                        gbc.insets = JBUI.insets(1)
                        gbc.gridx = 0
                        gbc.gridy = 0
                        add(JLabel("Please connect a config on the 'Products & Configs' tab."), gbc)
                    }
                )
                return content
            } else {
                val featureFlagsSettingsService = ConfigCatService.createFeatureFlagsSettingsService(
                    Constants.decodePublicApiConfiguration(stateConfig.authConfiguration), stateConfig.publicApiBaseUrl
                )
                val settings = try {
                    featureFlagsSettingsService.getSettings(connectedConfig.configId)
                } catch (exception: ApiException) {
                    ErrorHandler.errorNotify(exception)
                    content.add(JLabel("ConfigCat Plugin - Load failed. Try to refresh."))
                    return content
                }
                initTree(settings, connectedConfig.name)
                tree?.let {
                    val scrollPanel = ScrollPaneFactory.createScrollPane(tree, true)
                    content.add(scrollPanel)
                    initToolbar(this, it)
                    return content
                }
            }

        }
        content.add(JLabel("ConfigCat Plugin - Loading..."))
        return content
    }

    private fun resetTreeView() {
        tree = null
        treeModel = null
        if (toolbar != null) {
            toolbar = null
        }
    }

    private fun initToolbar(panel: JComponent, tree: Tree) {
        val actionManager: ActionManager = ActionManager.getInstance()
        val toolbarActionGroup = DefaultActionGroup()
        val actionToolbar: ActionToolbar =
            actionManager.createActionToolbar("CONFIGCAT_PANEL_ACTION_TOOLBAR", toolbarActionGroup, false)
        actionToolbar.targetComponent = panel
        toolbar = actionToolbar.component

        val refreshAction = actionManager.getAction(RefreshAction.CONFIGCAT_REFRESH_ACTION_ID)
        val createAction = actionManager.getAction(FlagCreateAction.CONFIGCAT_FLAG_CREATE_ACTION_ID)
        val openDashboardAction =
            actionManager.getAction(FlagOpenInBrowserAction.CONFIGCAT_FLAG_OPEN_CONFIG_IN_BROWSER_ACTION_ID)
        val searchFlagKeyAction = actionManager.getAction(FlagKeySearchAction.CONFIGCAT_SEARCH_FLAG_KEY_ACTION_ID)
        val copyFlagKeyAction = actionManager.getAction(FlagKeyCopyAction.CONFIGCAT_COPY_FLAG_KEY_ACTION_ID)
        val openFeatureFlagAction = actionManager.getAction(FlagViewOpenAction.CONFIGCAT_OPEN_FF_ACTION_ID)
        val openHelpAction = actionManager.getAction(HelpAction.CONFIGCAT_HELP_ACTION_ID)

        toolbarActionGroup.add(refreshAction)
        toolbarActionGroup.add(createAction)
        toolbarActionGroup.add(openDashboardAction)
        toolbarActionGroup.add(searchFlagKeyAction)
        toolbarActionGroup.add(copyFlagKeyAction)
        toolbarActionGroup.add(openFeatureFlagAction)
        toolbarActionGroup.add(openHelpAction)

        val actionPopup = DefaultActionGroup()

        PopupHandler.installPopupMenu(
            tree,
            actionPopup.apply {
                add(refreshAction)
                add(createAction)
                add(openDashboardAction)
                add(searchFlagKeyAction)
                add(copyFlagKeyAction)
                add(openFeatureFlagAction)
            },
            ActionPlaces.POPUP
        )
    }

    private fun initTree(flags: List<SettingModel>, connectedConfigName: String) {

        configCatNodeDataService.resetConfigsFlags()

        val treeStructure = FlagTreeStructure(ConfigRootNode(flags, connectedConfigName))
        val treeModel: StructureTreeModel<FlagTreeStructure> = StructureTreeModel(treeStructure, this)
        this.treeModel = treeModel
        val treeBuilder = AsyncTreeModel(treeModel, this)
        val tree = Tree()
        tree.model = treeBuilder

        tree.setRootVisible(true)
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        this.tree = tree
    }

    override fun dispose() {
    }

    private fun refreshTree() {

        tree?.let {
            val connectedConfig = loadConnectedConfig()
            if (connectedConfig == null) {
                initContent()
                return
            }

            val featureFlagsSettingsService = ConfigCatService.createFeatureFlagsSettingsService(
                Constants.decodePublicApiConfiguration(stateConfig.authConfiguration),
                stateConfig.publicApiBaseUrl
            )
            val settings = try {
                featureFlagsSettingsService.getSettings(connectedConfig.configId)
            } catch (exception: ApiException) {

                ErrorHandler.errorNotify(exception)
                return
            }
            val treeStructure = FlagTreeStructure(ConfigRootNode(settings, connectedConfig.name))
            val treeModel = StructureTreeModel(treeStructure, this)
            val treeBuilder = AsyncTreeModel(treeModel, this)
            this.treeModel = treeModel
            it.model = treeBuilder
            it.invalidate()
        }
    }

    private fun refreshTreeNode(node: DefaultMutableTreeNode) {
        treeModel?.invalidate(TreePath(node), true)
    }

    fun loadConnectedConfig(): ConfigModel? {
        val connectedConfigId = configCatPropertiesService.getConnectedConfig()
        if(connectedConfigId == null) {
            return null
        }
        val configsService = ConfigCatService.createConfigsService(
            Constants.decodePublicApiConfiguration(stateConfig.authConfiguration),
            stateConfig.publicApiBaseUrl
        )
        try {
            connectedConfig = configsService.getConfig(UUID.fromString(connectedConfigId))
        } catch (exception: ApiException) {
            ErrorHandler.errorNotify(exception)
            return null
        }
        return connectedConfig
    }

    fun getConnectedConfig(): ConfigModel? {
        return connectedConfig
    }

    fun getSelectedNode(): DefaultMutableTreeNode? {
        val paths: Array<TreePath>? = tree?.selectionPaths
        if (paths == null || paths.size != 1) return null
        val treeNode = paths[0].lastPathComponent as DefaultMutableTreeNode
        return treeNode
    }
}