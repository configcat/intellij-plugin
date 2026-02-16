package com.configcat.intellij.plugin.toolWindow.panel

import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.ErrorHandler
import com.configcat.intellij.plugin.actions.*
import com.configcat.intellij.plugin.messaging.ConfigChangeNotifier
import com.configcat.intellij.plugin.messaging.ProductsConfigsTreeChangeNotifier
import com.configcat.intellij.plugin.messaging.SettingsTreeChangeNotifier
import com.configcat.intellij.plugin.services.ConfigCatNodeDataService
import com.configcat.intellij.plugin.services.ConfigCatService
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.intellij.plugin.settings.ConfigCatConfigurable
import com.configcat.intellij.plugin.toolWindow.tree.FlagTreeStructure
import com.configcat.intellij.plugin.toolWindow.tree.ProductNode
import com.configcat.intellij.plugin.toolWindow.tree.ProductRootNode
import com.configcat.publicapi.java.client.ApiException
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
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
import com.intellij.util.ui.tree.TreeUtil
import com.intellij.util.ui.tree.TreeUtil.collectExpandedPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.awt.CardLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
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

    companion object{
        fun getInstance(project: Project): ProductsConfigsPanel =
            project.getService(ProductsConfigsPanel::class.java)
    }

    //TODO common tree panel?
    private val stateConfig: ConfigCatApplicationConfig.ConfigCatApplicationConfigSate = ConfigCatApplicationConfig.getInstance().state
    private val configCatNodeDataService: ConfigCatNodeDataService = ConfigCatNodeDataService.getInstance()
    private var tree: Tree? = null
    private var treeModel: StructureTreeModel<FlagTreeStructure>? = null
    private var expandedTreeNodes = mutableListOf<String>()

    init {
        //TODO common tree panel?
        setContent( initContent())
        val handleConfigChange = object : ConfigChangeNotifier {
            override fun notifyConfigChange() {
                setContent(initContent())
            }
        }
        ApplicationManager.getApplication().messageBus.connect()
            .subscribe(ConfigChangeNotifier.CONFIG_CHANGE_TOPIC, handleConfigChange)

        val handleTreeNotify = object : ProductsConfigsTreeChangeNotifier {
            override fun notifyTreeRefresh() {
                refreshTree()
            }

            override fun notifyTreeNodeRefresh(node: DefaultMutableTreeNode) {
                refreshTreeNode(node)
            }
        }
        ApplicationManager.getApplication().messageBus.connect()
            .subscribe(ProductsConfigsTreeChangeNotifier.TREE_REFRESH_TOPIC, handleTreeNotify)
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
            resetTreeView()
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

    private fun resetTreeView() {
        tree = null
        treeModel = null
        if(toolbar != null) {
            toolbar = null
        }
    }

    private fun initToolbar(panel: JComponent, tree: Tree) {
        val actionManager: ActionManager = ActionManager.getInstance()
        val toolbarActionGroup = DefaultActionGroup()
        val actionToolbar: ActionToolbar = actionManager.createActionToolbar("CONFIGCAT_PANEL_ACTION_TOOLBAR", toolbarActionGroup, false)
        actionToolbar.targetComponent = panel
        toolbar = actionToolbar.component

        val refreshAction = actionManager.getAction(ConfigRefreshAction.CONFIGCAT_CONFIG_REFRESH_ACTION_ID)
        val createAction = actionManager.getAction(ConfigCreateAction.CONFIGCAT_CONFIG_CREATE_ACTION_ID)
        val openDashboardAction = actionManager.getAction(ConfigOpenInBrowserAction.CONFIGCAT_OPEN_CONFIG_IN_BROWSER_ACTION_ID)
        val connectConfigAction = actionManager.getAction(ConfigConnectAction.CONFIGCAT_CONNECT_ACTION_ID)
        val openHelpAction = actionManager.getAction(HelpAction.CONFIGCAT_HELP_ACTION_ID)

        toolbarActionGroup.add(refreshAction)
        toolbarActionGroup.add(createAction)
        toolbarActionGroup.add(openDashboardAction)
        toolbarActionGroup.add(connectConfigAction)
        toolbarActionGroup.add(openHelpAction)

        val actionPopup = DefaultActionGroup()

        PopupHandler.installPopupMenu(
            tree,
            actionPopup.apply {
                add(refreshAction)
                add(createAction)
                add(openDashboardAction)
                add(connectConfigAction)
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
        configCatNodeDataService.resetProductConfigsData()

        val rootNode = ProductRootNode(products)
        val treeStructure = FlagTreeStructure(rootNode)
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
                    val productId = userObject.product.productId
                    cs.launch {
                        reload = configCatNodeDataService.checkAndLoadConfigs(productId)
                    }.invokeOnCompletion(
                        {
                            if(reload) {
                                refreshTreeNode(treeNode)
                            }
                        }
                    )
                }
            }

            override fun treeCollapsed(event: TreeExpansionEvent) {
            }
        })

        this.tree = tree
    }

    override fun dispose() {
    }

    private fun refreshTree() {
        tree?.let {
            it.invalidate()
            val collectExpandedPaths = collectExpandedPaths(it)
            expandedTreeNodes = mutableListOf()
            collectExpandedPaths.forEach { c ->
                c.lastPathComponent?.let { lastpC ->
                    val userObject = (lastpC as DefaultMutableTreeNode).userObject
                    if(userObject is ProductNode) {
                        expandedTreeNodes.add(userObject.product.productId.toString())
                    }
                }
            }
            val productsService = ConfigCatService.createProductsService(Constants.decodePublicApiConfiguration(stateConfig.authConfiguration), stateConfig.publicApiBaseUrl)
            val products = try {
                productsService.products
            } catch (exception: ApiException) {
                ErrorHandler.errorNotify(exception)
                return
            }
            val treeStructure = FlagTreeStructure(ProductRootNode(products))

            val treeModel = StructureTreeModel(treeStructure, this)
            val treeBuilder = AsyncTreeModel(treeModel, this)
            treeBuilder.addTreeModelListener( object : TreeModelListener {
                override fun treeNodesChanged(e: TreeModelEvent?) {
                }

                override fun treeNodesInserted(e: TreeModelEvent?) {
                    e?.children?.forEach { child ->
                        val childUserObject = (child as DefaultMutableTreeNode).userObject
                        if (childUserObject is ProductNode) {
                            if(expandedTreeNodes.contains(childUserObject.product.productId.toString())) {
                                TreeUtil.promiseExpand(it, TreeUtil.getPathFromRoot(child) )
                            }
                        }
                    }
                }

                override fun treeNodesRemoved(e: TreeModelEvent?) {
                }

                override fun treeStructureChanged(e: TreeModelEvent?) {
                }
            })
            this.treeModel = treeModel
            it.model = treeBuilder
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