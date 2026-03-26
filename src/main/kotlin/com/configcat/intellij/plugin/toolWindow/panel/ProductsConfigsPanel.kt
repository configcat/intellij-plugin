package com.configcat.intellij.plugin.toolWindow.panel

import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.ErrorHandler
import com.configcat.intellij.plugin.actions.*
import com.configcat.intellij.plugin.messaging.ConfigChangeNotifier
import com.configcat.intellij.plugin.messaging.ProductsConfigsTreeChangeNotifier
import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.configcat.intellij.plugin.services.ConfigCatService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.toolWindow.tree.FlagTreeStructure
import com.configcat.intellij.plugin.toolWindow.tree.ProductNode
import com.configcat.intellij.plugin.toolWindow.tree.ProductRootNode
import com.configcat.publicapi.java.client.ApiException
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.tree.TreeUtil
import com.intellij.util.ui.tree.TreeUtil.collectExpandedPaths
import kotlinx.coroutines.*
import java.awt.CardLayout
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

@Service(Service.Level.PROJECT)
class ProductsConfigsPanel(
    private val cs: CoroutineScope,
) : SimpleToolWindowPanel(false, false), Disposable {

    companion object {
        fun getInstance(project: Project): ProductsConfigsPanel = project.getService(ProductsConfigsPanel::class.java)
    }

    private val stateConfig: ConfigCatApplicationConfig.ConfigCatApplicationConfigState =
        ConfigCatApplicationConfig.getInstance().state
    private val configCatNodeDataService: ConfigCatNodeDataService = ConfigCatNodeDataService.getInstance()
    private var tree: Tree? = null
    private var treeModel: StructureTreeModel<FlagTreeStructure>? = null
    private var expandedTreeNodes = mutableListOf<String>()
    private val toolbarActionGroup = DefaultActionGroup()
    private val actionPopup = DefaultActionGroup()

    init {
        initToolbar(this)
        initContent()

        //Configure Notifiers
        val handleConfigChange = object : ConfigChangeNotifier {
            override fun notifyConfigChange() {
                initContent()
            }
        }
        ApplicationManager.getApplication().messageBus.connect(this)
            .subscribe(ConfigChangeNotifier.CONFIG_CHANGE_TOPIC, handleConfigChange)

        val handleTreeNotify = object : ProductsConfigsTreeChangeNotifier {
            override fun notifyTreeRefresh() {
                refreshTree()
            }

            override fun notifyTreeNodeRefresh(node: DefaultMutableTreeNode) {
                refreshTreeNode(node)
            }
        }
        ApplicationManager.getApplication().messageBus.connect(this)
            .subscribe(ProductsConfigsTreeChangeNotifier.TREE_REFRESH_TOPIC, handleTreeNotify)
    }

    private fun initContent() {
        val centeredInfoPanel = JPanel(GridBagLayout())
        val infoPanel = panel {
            row {
                icon(AllIcons.General.Information)
                label("ConfigCat Plugin is loading.")
            }
        }
        centeredInfoPanel.add(infoPanel)
        setContent(centeredInfoPanel)

        if (!stateConfig.isConfigured()) {
            setContent(ConfigurePluginPanel())
            resetTreeView()
        } else {
            initTreeContent()
        }
    }

    private fun initTreeContent() {
        cs.launch(Dispatchers.Default) {
            tree = initTree()

            cs.launch(Dispatchers.EDT) {
                val loadedContent: JComponent = JPanel(CardLayout())
                if (tree != null) {
                    // add action popup to the tree
                    PopupHandler.installPopupMenu(
                        tree!!, actionPopup, ActionPlaces.POPUP
                    )
                    loadedContent.add(ScrollPaneFactory.createScrollPane(tree, true))
                } else {
                    val centeredErrorPanel = JPanel(GridBagLayout())
                    val errorPanel = panel {
                        row {
                            icon(AllIcons.General.Error)
                            label("Something went wrong!")
                        }
                        row {
                            text("Try to refresh the panel. For more information check the logs.")
                        }
                    }
                    centeredErrorPanel.add(errorPanel)
                    loadedContent.add(centeredErrorPanel)
                }
                setContent(loadedContent)
            }

        }
    }

    private fun initTree(): Tree? {
        val productsService = ConfigCatService.createProductsService(
            Constants.decodePublicApiConfiguration(stateConfig.authConfiguration), stateConfig.publicApiBaseUrl
        )
        val products = try {
                productsService.products
        } catch (exception: ApiException) {
            ErrorHandler.errorNotify(exception)
            return null
        }

        configCatNodeDataService.resetProductConfigsData()

        val tree = Tree()
        val treeStructure = FlagTreeStructure(ProductRootNode(products))
        val treeModel: StructureTreeModel<FlagTreeStructure> = StructureTreeModel(treeStructure, this)
        val treeBuilder = AsyncTreeModel(treeModel, this)

        // add TreeModelListener.treeNodesInserted to expand nodes that should be expanded after loading data
        treeBuilder.addTreeModelListener(object : TreeModelListener {
            override fun treeNodesChanged(e: TreeModelEvent?) {
            }

            override fun treeNodesInserted(e: TreeModelEvent?) {
                e?.children?.forEach { child ->
                    val childUserObject = (child as DefaultMutableTreeNode).userObject
                    if (childUserObject is ProductNode) {
                        if (expandedTreeNodes.contains(childUserObject.product.productId.toString())) {
                            TreeUtil.promiseExpand(tree, TreeUtil.getPathFromRoot(child))
                        }
                    }
                }
            }

            override fun treeNodesRemoved(e: TreeModelEvent?) {
            }

            override fun treeStructureChanged(e: TreeModelEvent?) {
            }
        })

        tree.model = treeBuilder
        tree.setRootVisible(true)
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION

        // add TreeExpansionListener.treeExpanded to load configs of the expanded product node if not loaded yet, and refresh the node to show the configs
        tree.addTreeExpansionListener(object : TreeExpansionListener {
            override fun treeExpanded(event: TreeExpansionEvent) {
                val treeNode = event.path.lastPathComponent as DefaultMutableTreeNode
                val userObject = treeNode.userObject

                if (userObject is ProductNode) {
                    val productId = userObject.product.productId
                    cs.launch(Dispatchers.Default) {
                        val reload = configCatNodeDataService.checkAndLoadConfigs(productId)
                        if (reload) {
                            withContext(Dispatchers.EDT) {
                                refreshTreeNode(treeNode)
                            }
                        }
                    }
                }
            }

            override fun treeCollapsed(event: TreeExpansionEvent) {
            }
        })

        // set treeModel - used to invalidate the tree
        this.treeModel = treeModel
        return tree
    }

    private fun initToolbar(panel: JComponent) {
        val actionManager: ActionManager = ActionManager.getInstance()
        val actionToolbar: ActionToolbar =
            actionManager.createActionToolbar("CONFIGCAT_PANEL_ACTION_TOOLBAR", toolbarActionGroup, false)
        actionToolbar.targetComponent = panel
        toolbar = actionToolbar.component

        val refreshAction = actionManager.getAction(ConfigRefreshAction.CONFIGCAT_CONFIG_REFRESH_ACTION_ID)
        val createAction = actionManager.getAction(ConfigCreateAction.CONFIGCAT_CONFIG_CREATE_ACTION_ID)
        val openDashboardAction =
            actionManager.getAction(ConfigOpenInBrowserAction.CONFIGCAT_OPEN_CONFIG_IN_BROWSER_ACTION_ID)
        val connectConfigAction = actionManager.getAction(ConfigConnectAction.CONFIGCAT_CONNECT_ACTION_ID)
        val openHelpAction = actionManager.getAction(HelpAction.CONFIGCAT_HELP_ACTION_ID)

        toolbarActionGroup.add(refreshAction)
        toolbarActionGroup.add(createAction)
        toolbarActionGroup.add(openDashboardAction)
        toolbarActionGroup.add(connectConfigAction)
        toolbarActionGroup.add(openHelpAction)

        actionPopup.apply {
            add(refreshAction)
            add(createAction)
            add(openDashboardAction)
            add(connectConfigAction)
        }
    }

    override fun dispose() {
    }

    fun getSelectedNode(): DefaultMutableTreeNode? {
        val paths: Array<TreePath>? = tree?.selectionPaths
        if (paths == null || paths.size != 1) return null
        val treeNode = paths[0].lastPathComponent as DefaultMutableTreeNode
        return treeNode
    }

    private fun resetTreeView() {
        tree = null
        treeModel = null
        if (toolbar != null) {
            toolbar = null
        }
    }

    private fun refreshTree() {
        // if the Tree is not initialized yet, do nothing, the tree will be loaded with the latest data when it's initialized
        tree?.let {
            // invalidate the tree and collect the expanded nodes before refreshing to keep the same nodes expanded after refresh
            it.invalidate()
            val collectExpandedPaths = collectExpandedPaths(it)
            expandedTreeNodes = mutableListOf()
            collectExpandedPaths.forEach { c ->
                c.lastPathComponent?.let { lastPathComponent ->
                    val userObject = (lastPathComponent as DefaultMutableTreeNode).userObject
                    if (userObject is ProductNode) {
                        expandedTreeNodes.add(userObject.product.productId.toString())
                    }
                }
            }
        }
        initTreeContent()
    }

    private fun refreshTreeNode(node: DefaultMutableTreeNode) {
        treeModel?.invalidate(TreePath(node), true)
    }
}