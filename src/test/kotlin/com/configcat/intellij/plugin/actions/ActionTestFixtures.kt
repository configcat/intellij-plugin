package com.configcat.intellij.plugin.actions

import com.configcat.intellij.plugin.toolWindow.panel.ProductsConfigsPanel
import com.configcat.intellij.plugin.toolWindow.panel.SettingsPanel
import com.configcat.intellij.plugin.toolWindow.tree.ConfigNode
import com.configcat.intellij.plugin.toolWindow.tree.ConfigRootNode
import com.configcat.intellij.plugin.toolWindow.tree.FlagNode
import com.configcat.intellij.plugin.toolWindow.tree.ProductNode
import com.configcat.intellij.plugin.toolWindow.tree.ProductRootNode
import com.configcat.publicapi.java.client.model.ConfigModel
import com.configcat.publicapi.java.client.model.EvaluationVersion
import com.configcat.publicapi.java.client.model.OrganizationModel
import com.configcat.publicapi.java.client.model.ProductModel
import com.configcat.publicapi.java.client.model.SettingModel
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import javax.swing.tree.DefaultMutableTreeNode

object ActionTestFixtures {

    fun createProductsConfigsEvent(
        productsConfigsPanel: ProductsConfigsPanel,
        selectedNode: DefaultMutableTreeNode?
    ): AnActionEvent {
        val event = mockk<AnActionEvent>(relaxed = true)
        val mockProject = mockk<Project>(relaxed = true)
        every { event.project } returns mockProject
        every { mockProject.getService(ProductsConfigsPanel::class.java) } returns productsConfigsPanel
        every { productsConfigsPanel.getSelectedNode() } returns selectedNode
        return event
    }

    fun createSettingsEvent(
        settingsPanel: SettingsPanel,
        selectedNode: DefaultMutableTreeNode?,
        configModel: ConfigModel? = null
    ): AnActionEvent {
        val event = mockk<AnActionEvent>(relaxed = true)
        val mockProject = mockk<Project>(relaxed = true)
        every { event.project } returns mockProject
        every { mockProject.getService(SettingsPanel::class.java) } returns settingsPanel
        every { settingsPanel.getSelectedNode() } returns selectedNode
        every { settingsPanel.getConnectedConfig() } returns configModel
        return event
    }

    fun createProductNode(
        productId: UUID = UUID.randomUUID(),
        productName: String = "Product A",
        productDescription: String = "test product"
    ): ProductNode {
        val productModel = mockk<ProductModel>(relaxed = true)
        every { productModel.productId } returns productId
        every { productModel.description } returns productDescription
        every { productModel.name } returns productName
        return ProductNode(productModel, ProductRootNode(emptyList()))
    }

    fun createConfigNode(
        productId: UUID = UUID.randomUUID(),
        configId: UUID = UUID.randomUUID(),
        configName: String = "Config A",
        configDescription: String = "test config"
    ): ConfigNode {
        val productNode = createProductNode(productId = productId)
        val configModel = mockk<ConfigModel>(relaxed = true)
        every { configModel.configId } returns configId
        every { configModel.name } returns configName
        every { configModel.description } returns configDescription
        return ConfigNode(configModel, productNode)
    }

    fun createFlagTreeNode(
        settingId: Int,
        configId: UUID,
        key: String = "my_flag_key",
        name: String = "My Flag",
        hint: String = "",
        rootName: String = "Test"
    ): DefaultMutableTreeNode {
        val settingModel = mockk<SettingModel>(relaxed = true)
        every { settingModel.key } returns key
        every { settingModel.name } returns name
        every { settingModel.hint } returns hint
        every { settingModel.settingId } returns settingId
        every { settingModel.configId } returns configId
        return DefaultMutableTreeNode(FlagNode(settingModel, ConfigRootNode(emptyList(), rootName)))
    }

    fun createConnectedConfigModel(
        productId: UUID,
        configId: UUID,
        evaluationVersion: EvaluationVersion = EvaluationVersion.V2,
        orgId: UUID = UUID.randomUUID()
    ): ConfigModel {
        val orgModel = mockk<OrganizationModel>(relaxed = true)
        every { orgModel.organizationId } returns orgId

        val productModel = mockk<ProductModel>(relaxed = true)
        every { productModel.productId } returns productId
        every { productModel.organization } returns orgModel

        val configModel = mockk<ConfigModel>(relaxed = true)
        every { configModel.configId } returns configId
        every { configModel.product } returns productModel
        every { configModel.evaluationVersion } returns evaluationVersion
        return configModel
    }
}

