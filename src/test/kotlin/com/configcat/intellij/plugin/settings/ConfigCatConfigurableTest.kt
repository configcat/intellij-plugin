package com.configcat.intellij.plugin.settings

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.Constants
import com.configcat.intellij.plugin.services.PublicApiConfiguration
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.testFramework.LightPlatformTestCase
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JTextField

class ConfigCatConfigurableTest : LightPlatformTestCase() {

    private lateinit var storedAuthConfiguration: String

    override fun setUp() {
        super.setUp()

        storedAuthConfiguration = ""
        mockkObject(PasswordSafe)
        val passwordSafe = mockk<PasswordSafe>(relaxed = true)
        every { PasswordSafe.instance } returns passwordSafe
        every { passwordSafe.getPassword(any()) } answers { storedAuthConfiguration }
        every { passwordSafe.set(any(), any()) } answers {
            storedAuthConfiguration = secondArg<Credentials?>()?.getPasswordAsString().orEmpty()
        }

        mockkObject(ConfigCatNotifier.Notify)
        every { ConfigCatNotifier.Notify.error(any<String>()) } just runs
        every { ConfigCatNotifier.Notify.error(any(), any()) } just runs
        every { ConfigCatNotifier.Notify.info(any()) } just runs
    }

    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    fun testGetDisplayName_returnsExpectedLabel() {
        val configurable = ConfigCatConfigurable()

        assertEquals("ConfigCat Plugin Settings", configurable.displayName)
    }

    fun testReset_updatesAuthenticationUiFromState_andIsModifiedIsFalse() {
        val configurable = ConfigCatConfigurable()
        val state = buildState()
        setStateConfig(configurable, state)
        configurable.createPanel()

        configurable.reset()

        assertEquals(PublicApiConfiguration("demo-user", "demo-pass"), credentials(configurable))
        assertEquals("Logged in as demo-user", label(configurable, "authStatusLabel").text)
        assertEquals("Unauthorize", button(configurable, "authActionButton").text)
        assertEquals("https://dashboard.example.com", textField(configurable, "dashboardBaseUrlField").text)
        assertEquals("https://api.example.com", textField(configurable, "publicApiBaseUrlField").text)
        assertFalse(configurable.isModified)
    }

    fun testIsModified_returnsTrueWhenCredentialOrUrlChanges() {
        val configurable = ConfigCatConfigurable()
        setStateConfig(configurable, buildState())
        configurable.reset()

        setCredentials(configurable, PublicApiConfiguration("changed-user", "demo-pass"))
        assertTrue(configurable.isModified)

        configurable.reset()
        textField(configurable, "dashboardBaseUrlField").text = "https://another.example.com"
        assertTrue(configurable.isModified)
    }

    fun testApply_emptyPublicApiUrl_notifiesError() {
        val configurable = ConfigCatConfigurable()
        setStateConfig(configurable, buildState(userName = "", password = ""))
        configurable.createPanel()
        configurable.reset()

        textField(configurable, "publicApiBaseUrlField").text = ""
        configurable.apply()

        verify(exactly = 1) { ConfigCatNotifier.Notify.error("Public API Base URL cannot be empty.") }
    }

    fun testApply_updatedCredentials_logsOutAndPersistsUpdatedAuthenticationState() {
        val configurable = ConfigCatConfigurable()
        val state = buildState()
        setStateConfig(configurable, state)
        configurable.createPanel()
        configurable.reset()

        setCredentials(configurable, PublicApiConfiguration("", ""))
        textField(configurable, "dashboardBaseUrlField").text = "https://dashboard.new.example.com"
        textField(configurable, "publicApiBaseUrlField").text = "https://api.new.example.com"

        configurable.apply()

        val savedAuth = Constants.decodePublicApiConfiguration(state.authConfiguration)
        assertEquals("", savedAuth.basicAuthUserName)
        assertEquals("", savedAuth.basicAuthPassword)
        assertEquals("Login to use ConfigCat Feature Flags", label(configurable, "authStatusLabel").text)
        assertEquals("Authorize", button(configurable, "authActionButton").text)
    }

    private fun buildState(
        userName: String = "demo-user",
        password: String = "demo-pass",
        dashboard: String = "https://dashboard.example.com",
        publicApi: String = "https://api.example.com",
    ): ConfigCatApplicationConfig.ConfigCatApplicationConfigState {
        val state = ConfigCatApplicationConfig.ConfigCatApplicationConfigState(
            dashboardBaseUrl = dashboard,
            publicApiBaseUrl = publicApi,
        )
        state.authConfiguration = Constants.encodePublicApiConfiguration(PublicApiConfiguration(userName, password))
        return state
    }

    private fun setStateConfig(
        configurable: ConfigCatConfigurable,
        state: ConfigCatApplicationConfig.ConfigCatApplicationConfigState,
    ) {
        val field = ConfigCatConfigurable::class.java.getDeclaredField("stateConfig")
        field.isAccessible = true
        field.set(configurable, state)
    }

    private fun textField(configurable: ConfigCatConfigurable, fieldName: String): JTextField {
        val field = ConfigCatConfigurable::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(configurable) as JTextField
    }

    private fun label(configurable: ConfigCatConfigurable, fieldName: String): JLabel {
        val field = ConfigCatConfigurable::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(configurable) as JLabel
    }

    private fun button(configurable: ConfigCatConfigurable, fieldName: String): JButton {
        val field = ConfigCatConfigurable::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(configurable) as JButton
    }

    private fun credentials(configurable: ConfigCatConfigurable): PublicApiConfiguration {
        val field = ConfigCatConfigurable::class.java.getDeclaredField("credentials")
        field.isAccessible = true
        return field.get(configurable) as PublicApiConfiguration
    }

    private fun setCredentials(configurable: ConfigCatConfigurable, credentials: PublicApiConfiguration) {
        val field = ConfigCatConfigurable::class.java.getDeclaredField("credentials")
        field.isAccessible = true
        field.set(configurable, credentials)
    }
}




