package com.configcat.intellij.plugin.toolWindow

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.ErrorHandler
import com.configcat.intellij.plugin.actions.*
import com.configcat.intellij.plugin.messaging.ConfigChangeNotifier
import com.configcat.intellij.plugin.messaging.TreeChangeNotifier
import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.configcat.intellij.plugin.services.ConfigCatService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.settings.ConfigCatConfigurable
import com.configcat.publicapi.java.client.ApiException
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import java.awt.CardLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel


@Service(Service.Level.PROJECT)
class ConfigCatPanel : SimpleToolWindowPanel(false, false), Disposable {

    companion object{
        fun getInstance(project: Project): ConfigCatPanel =
            project.getService(ConfigCatPanel::class.java)
    }


    private val stateConfig: ConfigCatApplicationConfig.ConfigCatApplicationConfigSate = ConfigCatApplicationConfig.getInstance().state
    private val configCatNodeDataService: ConfigCatNodeDataService = ConfigCatNodeDataService.getInstance()
    private var tree: Tree? = null
    private var treeModel: StructureTreeModel<FlagTreeStructure>? = null


    init {
        setContent( initContent())
        val handleConfigChange = object : ConfigChangeNotifier {
            override fun notifyConfigChange() {
                setContent(initContent())
            }
        }
        ApplicationManager.getApplication().messageBus.connect()
            .subscribe(ConfigChangeNotifier.CONFIG_CHANGE_TOPIC, handleConfigChange)

        val handleTreeNotify = object : TreeChangeNotifier {
            override fun notifyTreeRefresh() {
                refreshTree()
            }

            override fun notifyTreeNodeRefresh(node: DefaultMutableTreeNode) {
                refreshTreeNode(node)
            }
        }
        ApplicationManager.getApplication().messageBus.connect()
            .subscribe(TreeChangeNotifier.TREE_CHANGE_TOPIC, handleTreeNotify)
    }

    private fun initContent() : JComponent {
        val content: JComponent = JPanel(CardLayout())
        if(!stateConfig.isConfigured()) {
            content.add(
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
                    },  gbc)
                }
            )
            return content
        } else {
            initTree()
            tree?.let{
                val scrollPanel = ScrollPaneFactory.createScrollPane(tree, true)
                content.add(scrollPanel)
                initToolbar(this, it)
                return content
            }
        }
        content.add(JLabel("ConfigCat Plugin - Loading..."))
        return content
    }

    private fun initToolbar(panel: JComponent, tree: Tree){
        val actionManager: ActionManager = ActionManager.getInstance()
        val toolbarActionGroup = DefaultActionGroup()
        val actionToolbar: ActionToolbar = actionManager.createActionToolbar("CONFIGCAT_PANEL_ACTION_TOOLBAR", toolbarActionGroup, false)
        actionToolbar.targetComponent = panel
        toolbar = actionToolbar.component

        val refreshAction = actionManager.getAction(RefreshAction.CONFIGCAT_REFRESH_ACTION_ID)
        val createAction = actionManager.getAction(CreateAction.CONFIGCAT_CREATE_ACTION_ID)
        val openDashboardAction = actionManager.getAction(OpenInBrowserAction.CONFIGCAT_OPEN_IN_BROWSER_ACTION_ID)
        val searchFlagKeyAction = actionManager.getAction(SearchFlagKeyAction.CONFIGCAT_SEARCH_FLAG_KEY_ACTION_ID)
        val copyFlagKeyAction = actionManager.getAction(CopyFlagKeyAction.CONFIGCAT_COPY_FLAG_KEY_ACTION_ID)

        toolbarActionGroup.add(refreshAction)
        toolbarActionGroup.add(createAction)
        toolbarActionGroup.add(openDashboardAction)
        toolbarActionGroup.add(searchFlagKeyAction)
        toolbarActionGroup.add(copyFlagKeyAction)

        val actionPopup = DefaultActionGroup()

        PopupHandler.installPopupMenu(
            tree,
            actionPopup.apply {
                add(refreshAction)
                add(createAction)
                add(openDashboardAction)
                add(searchFlagKeyAction)
                add(copyFlagKeyAction)
            },
            ActionPlaces.POPUP
        )
    }


    private fun initTree() {
        val productsService = ConfigCatService.createProductsService(Constants.decodePublicApiConfiguration(stateConfig.authConfiguration), stateConfig.publicApiBaseUrl)
        val products = try {
            productsService.products
        } catch (exception: ApiException) {
            ErrorHandler.errorNotify(exception)
            return
        }
        configCatNodeDataService.resetData()

        val treeStructure = FlagTreeStructure(RootNode(products))
        val treeModel: StructureTreeModel<FlagTreeStructure> = StructureTreeModel(treeStructure, this)
        this.treeModel = treeModel
        val treeBuilder = AsyncTreeModel(treeModel, this)
        val tree = Tree()
        tree.model = treeBuilder

        tree.setRootVisible(true)
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.addTreeExpansionListener( object : TreeExpansionListener {
            override fun treeExpanded(event: TreeExpansionEvent) {
                val treeNode = event.path.lastPathComponent as DefaultMutableTreeNode
                val userObject = treeNode.userObject
                var reload = false
                if (userObject is ProductNode) {
                    reload = configCatNodeDataService.checkAndLoadConfigs(userObject.product.productId)
                }
                if (userObject is ConfigNode) {
                    reload = configCatNodeDataService.checkAndLoadFlags(userObject.config.configId)
                }
                if(reload) {
                    refreshTreeNode(treeNode)
                }

            }

            override fun treeCollapsed(event: TreeExpansionEvent?) {
            }

        })
        this.tree = tree
    }

    override fun dispose() {
    }

    private fun refreshTree() {
        tree?.let {
            val productsService = ConfigCatService.createProductsService(Constants.decodePublicApiConfiguration(stateConfig.authConfiguration), stateConfig.publicApiBaseUrl)
            val products = try {
                productsService.products
            } catch (exception: ApiException) {
                ErrorHandler.errorNotify(exception)
                return
            }
            val treeStructure = FlagTreeStructure(RootNode(products))
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

    fun getSelectedNode(): DefaultMutableTreeNode? {
        val paths: Array<TreePath>? = tree?.selectionPaths
        if (paths == null || paths.size != 1) return null
        val treeNode = paths[0].lastPathComponent as DefaultMutableTreeNode
        return treeNode
    }
}