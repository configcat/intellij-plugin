package com.configcat.intellij.plugin.settings

import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConfigCatApplicationConfigTest {

	private lateinit var passwordSafe: PasswordSafe
	private var storedAuthConfiguration: String = ""

	@Before
	fun setUp() {
		storedAuthConfiguration = ""
		mockkObject(PasswordSafe)
		passwordSafe = mockk(relaxed = true)

		every { PasswordSafe.instance } returns passwordSafe
		every { passwordSafe.getPassword(any()) } answers { storedAuthConfiguration }
		every { passwordSafe.set(any(), any()) } answers {
			storedAuthConfiguration = secondArg<Credentials?>()?.getPasswordAsString().orEmpty()
		}
	}

	@After
	fun tearDown() {
		unmockkObject(PasswordSafe)
	}

	@Test
	fun `state defaults should match constant urls`() {
		val state = ConfigCatApplicationConfig.ConfigCatApplicationConfigState()

		assertEquals(DEFAULT_DASHBOARD_BASE_URL, state.dashboardBaseUrl)
		assertEquals(DEFAULT_PUBLIC_API_BASE_URL, state.publicApiBaseUrl)
		assertFalse(state.isConfigured())
	}

	@Test
	fun `loadState should replace app state returned by getState`() {
		val config = ConfigCatApplicationConfig()
		val newState = ConfigCatApplicationConfig.ConfigCatApplicationConfigState(
			dashboardBaseUrl = "https://dashboard.example.com",
			publicApiBaseUrl = "https://api.example.com",
		)

		config.loadState(newState)

		assertEquals(newState, config.state)
		assertEquals(newState, config.getState())
	}

	@Test
	fun `authConfiguration should persist through password safe`() {
		val state = ConfigCatApplicationConfig.ConfigCatApplicationConfigState()

		state.authConfiguration = "{\"token\":\"abc\"}"

		assertEquals("{\"token\":\"abc\"}", state.authConfiguration)
	}

	@Test
	fun `isConfigured should return true for non-empty auth and urls`() {
		val state = ConfigCatApplicationConfig.ConfigCatApplicationConfigState(
			dashboardBaseUrl = "https://dashboard.example.com",
			publicApiBaseUrl = "https://api.example.com",
		)
		state.authConfiguration = "{\"token\":\"abc\"}"

		assertTrue(state.isConfigured())
	}

	@Test
	fun `isConfigured should return false for empty credentials constant`() {
		val state = ConfigCatApplicationConfig.ConfigCatApplicationConfigState(
			dashboardBaseUrl = "https://dashboard.example.com",
			publicApiBaseUrl = "https://api.example.com",
		)
		state.authConfiguration = EMPTY_CREDENTIALS

		assertFalse(state.isConfigured())
	}

	@Test
	fun `isConfigured should return false when required values are missing`() {
		val missingAuth = ConfigCatApplicationConfig.ConfigCatApplicationConfigState(
			dashboardBaseUrl = "https://dashboard.example.com",
			publicApiBaseUrl = "https://api.example.com",
		)
		assertFalse(missingAuth.isConfigured())

		val missingDashboard = ConfigCatApplicationConfig.ConfigCatApplicationConfigState(
			dashboardBaseUrl = "",
			publicApiBaseUrl = "https://api.example.com",
		)
		missingDashboard.authConfiguration = "{\"token\":\"abc\"}"
		assertFalse(missingDashboard.isConfigured())

		val missingPublicApi = ConfigCatApplicationConfig.ConfigCatApplicationConfigState(
			dashboardBaseUrl = "https://dashboard.example.com",
			publicApiBaseUrl = "",
		)
		missingPublicApi.authConfiguration = "{\"token\":\"abc\"}"
		assertFalse(missingPublicApi.isConfigured())
	}
}
