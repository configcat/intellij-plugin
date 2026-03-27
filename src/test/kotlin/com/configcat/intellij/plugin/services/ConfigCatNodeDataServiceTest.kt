package com.configcat.intellij.plugin.services

import com.configcat.intellij.plugin.ConfigCatNotifier
import com.configcat.intellij.plugin.ErrorHandler
import com.configcat.intellij.plugin.settings.ConfigCatApplicationConfig
import com.configcat.publicapi.java.client.ApiException
import com.configcat.publicapi.java.client.api.ConfigsApi
import com.configcat.publicapi.java.client.api.FeatureFlagsSettingsApi
import com.configcat.publicapi.java.client.model.ConfigModel
import com.configcat.publicapi.java.client.model.SettingModel
import com.intellij.testFramework.LoggedErrorProcessor
import com.intellij.testFramework.LightPlatformTestCase
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.Runs
import io.mockk.unmockkAll
import io.mockk.verify
import java.lang.reflect.Field
import java.util.UUID

@Suppress("UnstableApiUsage")
class ConfigCatNodeDataServiceTest : LightPlatformTestCase() {

    private lateinit var mockState: ConfigCatApplicationConfig.ConfigCatApplicationConfigState
    private lateinit var mockConfigsApi: ConfigsApi
    private lateinit var mockFeatureFlagsApi: FeatureFlagsSettingsApi

    private val productId: UUID = UUID.randomUUID()
    private val configId: UUID = UUID.randomUUID()

    override fun setUp() {
        super.setUp()

        mockState = mockk(relaxed = true)
        mockConfigsApi = mockk(relaxed = true)
        mockFeatureFlagsApi = mockk(relaxed = true)

        val mockConfig = mockk<ConfigCatApplicationConfig>(relaxed = true)
        every { mockConfig.state } returns mockState
        every { mockState.authConfiguration } returns """{"basicAuthUserName":"user","basicAuthPassword":"pass"}"""
        every { mockState.publicApiBaseUrl } returns "https://api.example.com"

        mockkObject(ConfigCatApplicationConfig.Companion)
        every { ConfigCatApplicationConfig.getInstance() } returns mockConfig

        mockkObject(ConfigCatService.Companion)
        every { ConfigCatService.createConfigsService(any(), any()) } returns mockConfigsApi
        every { ConfigCatService.createFeatureFlagsSettingsService(any(), any()) } returns mockFeatureFlagsApi

        mockkObject(ErrorHandler)
        every { ErrorHandler.errorNotify(any()) } just Runs

        mockkObject(ConfigCatNotifier.Notify)
        every { ConfigCatNotifier.Notify.error(any()) } just Runs
    }

    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    // -------------------------------------------------------------------------
    // checkAndLoadConfigs
    // -------------------------------------------------------------------------

    fun testCheckAndLoadConfigs_nullProductId_returnsFalseAndNotifiesError() {
        val service = ConfigCatNodeDataService()
        var result = false

        // checkAndLoadConfigs(null) calls thisLogger().error() which LightPlatformTestCase
        // normally converts into a test failure; suppress it for this expected-error scenario.
        suppressLogErrors { result = service.checkAndLoadConfigs(null) }

        assertFalse("checkAndLoadConfigs(null) must return false", result)
        verify { ConfigCatNotifier.Notify.error(any()) }
    }

    fun testCheckAndLoadConfigs_nullProductId_doesNotCallConfigsApi() {
        val service = ConfigCatNodeDataService()

        suppressLogErrors { service.checkAndLoadConfigs(null) }

        verify(exactly = 0) { mockConfigsApi.getConfigs(any()) }
    }

    fun testCheckAndLoadConfigs_configsNotYetLoaded_callsLoadConfigsAndReturnsTrue() {
        every { mockConfigsApi.getConfigs(productId) } returns emptyList()
        val service = ConfigCatNodeDataService()

        val result = service.checkAndLoadConfigs(productId)

        assertTrue("checkAndLoadConfigs() must return true for a valid product ID", result)
        verify(exactly = 1) { mockConfigsApi.getConfigs(productId) }
    }

    fun testCheckAndLoadConfigs_configsAlreadyLoaded_skipsApiCallAndReturnsTrue() {
        every { mockConfigsApi.getConfigs(productId) } returns emptyList()
        val service = ConfigCatNodeDataService()
        service.checkAndLoadConfigs(productId) // first call – loads

        val result = service.checkAndLoadConfigs(productId) // second call – should skip API

        assertTrue("checkAndLoadConfigs() must still return true when configs are already cached", result)
        // The API must have been called exactly once (during the first call)
        verify(exactly = 1) { mockConfigsApi.getConfigs(productId) }
    }

    fun testCheckAndLoadConfigs_differentProductIds_loadsEachSeparately() {
        val anotherProductId = UUID.randomUUID()
        every { mockConfigsApi.getConfigs(any()) } returns emptyList()
        val service = ConfigCatNodeDataService()

        service.checkAndLoadConfigs(productId)
        service.checkAndLoadConfigs(anotherProductId)

        verify(exactly = 1) { mockConfigsApi.getConfigs(productId) }
        verify(exactly = 1) { mockConfigsApi.getConfigs(anotherProductId) }
    }

    // -------------------------------------------------------------------------
    // loadConfigs
    // -------------------------------------------------------------------------

    fun testLoadConfigs_success_storesReturnedConfigs() {
        val configA = mockk<ConfigModel>(relaxed = true)
        val configB = mockk<ConfigModel>(relaxed = true)
        every { mockConfigsApi.getConfigs(productId) } returns listOf(configA, configB)
        val service = ConfigCatNodeDataService()

        service.loadConfigs(productId)

        val stored = service.getProductConfigs(productId)
        assertNotNull("loadConfigs() must populate the product-configs cache", stored)
        assertEquals("Cached list must contain all configs returned by the API", 2, stored!!.size)
        assertSame(configA, stored[0])
        assertSame(configB, stored[1])
    }

    fun testLoadConfigs_emptyList_storesEmptyList() {
        every { mockConfigsApi.getConfigs(productId) } returns emptyList()
        val service = ConfigCatNodeDataService()

        service.loadConfigs(productId)

        val stored = service.getProductConfigs(productId)
        assertNotNull("loadConfigs() must cache an empty list when the API returns no configs", stored)
        assertTrue("Cached list must be empty", stored!!.isEmpty())
    }

    fun testLoadConfigs_api401Unauthorized_doesNotCacheAndNotifiesError() {
        every { mockConfigsApi.getConfigs(productId) } throws ApiException(401, "Unauthorized")
        val service = ConfigCatNodeDataService()

        service.loadConfigs(productId)

        assertNull("Configs must not be cached after a 401 error", service.getProductConfigs(productId))
        verify { ErrorHandler.errorNotify(any<ApiException>()) }
    }

    fun testLoadConfigs_api429TooManyRequests_doesNotCacheAndNotifiesError() {
        every { mockConfigsApi.getConfigs(productId) } throws ApiException(429, "Too Many Requests")
        val service = ConfigCatNodeDataService()

        service.loadConfigs(productId)

        assertNull("Configs must not be cached after a 429 error", service.getProductConfigs(productId))
        verify { ErrorHandler.errorNotify(any<ApiException>()) }
    }

    fun testLoadConfigs_api500ServerError_doesNotCacheAndNotifiesError() {
        every { mockConfigsApi.getConfigs(productId) } throws ApiException(500, "Internal Server Error")
        val service = ConfigCatNodeDataService()

        service.loadConfigs(productId)

        assertNull("Configs must not be cached after a 500 error", service.getProductConfigs(productId))
        verify { ErrorHandler.errorNotify(any<ApiException>()) }
    }

    fun testLoadConfigs_api503ServiceUnavailable_doesNotCacheAndNotifiesError() {
        every { mockConfigsApi.getConfigs(productId) } throws ApiException(503, "Service Unavailable")
        val service = ConfigCatNodeDataService()

        service.loadConfigs(productId)

        assertNull("Configs must not be cached after a 503 error", service.getProductConfigs(productId))
        verify { ErrorHandler.errorNotify(any<ApiException>()) }
    }

    // -------------------------------------------------------------------------
    // getProductConfigs
    // -------------------------------------------------------------------------

    fun testGetProductConfigs_nothingLoaded_returnsNull() {
        val service = ConfigCatNodeDataService()

        val result = service.getProductConfigs(productId)

        assertNull("getProductConfigs() must return null before any configs are loaded", result)
    }

    fun testGetProductConfigs_afterSuccessfulLoad_returnsCachedList() {
        val config = mockk<ConfigModel>(relaxed = true)
        every { mockConfigsApi.getConfigs(productId) } returns listOf(config)
        val service = ConfigCatNodeDataService()
        service.loadConfigs(productId)

        val result = service.getProductConfigs(productId)

        assertNotNull("getProductConfigs() must return the cached list after a successful load", result)
        assertEquals(1, result!!.size)
        assertSame(config, result[0])
    }

    fun testGetProductConfigs_unknownProductId_returnsNull() {
        val knownProductId = productId
        val unknownProductId = UUID.randomUUID()
        every { mockConfigsApi.getConfigs(knownProductId) } returns emptyList()
        val service = ConfigCatNodeDataService()
        service.loadConfigs(knownProductId)

        val result = service.getProductConfigs(unknownProductId)

        assertNull("getProductConfigs() must return null for a product ID that was never loaded", result)
    }

    // -------------------------------------------------------------------------
    // loadFlags
    // -------------------------------------------------------------------------

    fun testLoadFlags_success_storesFlagsForConfigId() {
        val flagA = mockk<SettingModel>(relaxed = true)
        val flagB = mockk<SettingModel>(relaxed = true)
        every { mockFeatureFlagsApi.getSettings(configId) } returns listOf(flagA, flagB)
        val service = ConfigCatNodeDataService()

        service.loadFlags(configId)

        val stored = configFlagsField(service)[configId]
        assertNotNull("loadFlags() must populate the flags cache", stored)
        assertEquals("Cached flags list must contain all settings returned by the API", 2, stored!!.size)
        assertSame(flagA, stored[0])
        assertSame(flagB, stored[1])
    }

    fun testLoadFlags_emptyList_storesEmptyList() {
        every { mockFeatureFlagsApi.getSettings(configId) } returns emptyList()
        val service = ConfigCatNodeDataService()

        service.loadFlags(configId)

        val stored = configFlagsField(service)[configId]
        assertNotNull("loadFlags() must cache an empty list when the API returns no flags", stored)
        assertTrue("Cached flags list must be empty", stored!!.isEmpty())
    }

    fun testLoadFlags_api401Unauthorized_doesNotCacheFlagsAndNotifiesError() {
        every { mockFeatureFlagsApi.getSettings(configId) } throws ApiException(401, "Unauthorized")
        val service = ConfigCatNodeDataService()

        service.loadFlags(configId)

        assertNull("Flags must not be cached after a 401 error", configFlagsField(service)[configId])
        verify { ErrorHandler.errorNotify(any<ApiException>()) }
    }

    fun testLoadFlags_api429TooManyRequests_doesNotCacheFlagsAndNotifiesError() {
        every { mockFeatureFlagsApi.getSettings(configId) } throws ApiException(429, "Too Many Requests")
        val service = ConfigCatNodeDataService()

        service.loadFlags(configId)

        assertNull("Flags must not be cached after a 429 error", configFlagsField(service)[configId])
        verify { ErrorHandler.errorNotify(any<ApiException>()) }
    }

    fun testLoadFlags_api500ServerError_doesNotCacheFlagsAndNotifiesError() {
        every { mockFeatureFlagsApi.getSettings(configId) } throws ApiException(500, "Internal Server Error")
        val service = ConfigCatNodeDataService()

        service.loadFlags(configId)

        assertNull("Flags must not be cached after a 500 error", configFlagsField(service)[configId])
        verify { ErrorHandler.errorNotify(any<ApiException>()) }
    }

    fun testLoadFlags_api503ServiceUnavailable_doesNotCacheFlagsAndNotifiesError() {
        every { mockFeatureFlagsApi.getSettings(configId) } throws ApiException(503, "Service Unavailable")
        val service = ConfigCatNodeDataService()

        service.loadFlags(configId)

        assertNull("Flags must not be cached after a 503 error", configFlagsField(service)[configId])
        verify { ErrorHandler.errorNotify(any<ApiException>()) }
    }

    // -------------------------------------------------------------------------
    // resetConfigsFlags
    // -------------------------------------------------------------------------

    fun testResetConfigsFlags_removesAllCachedFlags() {
        val anotherConfigId = UUID.randomUUID()
        every { mockFeatureFlagsApi.getSettings(any()) } returns listOf(mockk(relaxed = true))
        val service = ConfigCatNodeDataService()
        service.loadFlags(configId)
        service.loadFlags(anotherConfigId)
        assertFalse("Pre-condition: flags cache must be populated", configFlagsField(service).isEmpty())

        service.resetConfigsFlags()

        assertTrue("resetConfigsFlags() must clear the entire flags cache", configFlagsField(service).isEmpty())
    }

    fun testResetConfigsFlags_onEmptyCache_isIdempotent() {
        val service = ConfigCatNodeDataService()
        assertTrue("Pre-condition: flags cache must be empty initially", configFlagsField(service).isEmpty())

        service.resetConfigsFlags()

        assertTrue("resetConfigsFlags() on an already-empty cache must leave it empty", configFlagsField(service).isEmpty())
    }

    fun testResetConfigsFlags_subsequentLoadAfterReset_repopulatesCache() {
        every { mockFeatureFlagsApi.getSettings(configId) } returns listOf(mockk(relaxed = true))
        val service = ConfigCatNodeDataService()
        service.loadFlags(configId)
        service.resetConfigsFlags()

        service.loadFlags(configId)

        assertNotNull(
            "loadFlags() after resetConfigsFlags() must repopulate the cache",
            configFlagsField(service)[configId]
        )
        verify(exactly = 2) { mockFeatureFlagsApi.getSettings(configId) }
    }

    // -------------------------------------------------------------------------
    // resetProductConfigsData
    // -------------------------------------------------------------------------

    fun testResetProductConfigsData_removesAllCachedConfigs() {
        val anotherProductId = UUID.randomUUID()
        every { mockConfigsApi.getConfigs(any()) } returns listOf(mockk(relaxed = true))
        val service = ConfigCatNodeDataService()
        service.loadConfigs(productId)
        service.loadConfigs(anotherProductId)
        assertNotNull("Pre-condition: configs must be cached for productId", service.getProductConfigs(productId))
        assertNotNull("Pre-condition: configs must be cached for anotherProductId", service.getProductConfigs(anotherProductId))

        service.resetProductConfigsData()

        assertNull("resetProductConfigsData() must remove cached configs for productId", service.getProductConfigs(productId))
        assertNull("resetProductConfigsData() must remove cached configs for anotherProductId", service.getProductConfigs(anotherProductId))
    }

    fun testResetProductConfigsData_onEmptyCache_isIdempotent() {
        val service = ConfigCatNodeDataService()

        service.resetProductConfigsData()

        assertNull("resetProductConfigsData() on empty cache must leave productId absent", service.getProductConfigs(productId))
    }

    fun testResetProductConfigsData_subsequentLoadAfterReset_repopulatesCache() {
        every { mockConfigsApi.getConfigs(productId) } returns listOf(mockk(relaxed = true))
        val service = ConfigCatNodeDataService()
        service.loadConfigs(productId)
        service.resetProductConfigsData()

        service.loadConfigs(productId)

        assertNotNull(
            "loadConfigs() after resetProductConfigsData() must repopulate the cache",
            service.getProductConfigs(productId)
        )
        verify(exactly = 2) { mockConfigsApi.getConfigs(productId) }
    }

    fun testResetProductConfigsData_doesNotAffectFlagsCache() {
        every { mockConfigsApi.getConfigs(productId) } returns emptyList()
        every { mockFeatureFlagsApi.getSettings(configId) } returns listOf(mockk(relaxed = true))
        val service = ConfigCatNodeDataService()
        service.loadConfigs(productId)
        service.loadFlags(configId)
        assertFalse("Pre-condition: flags cache must be populated", configFlagsField(service).isEmpty())

        service.resetProductConfigsData()

        assertFalse("resetProductConfigsData() must not clear the flags cache", configFlagsField(service).isEmpty())
    }

    fun testResetConfigsFlags_doesNotAffectProductConfigsCache() {
        every { mockConfigsApi.getConfigs(productId) } returns listOf(mockk(relaxed = true))
        every { mockFeatureFlagsApi.getSettings(configId) } returns emptyList()
        val service = ConfigCatNodeDataService()
        service.loadConfigs(productId)
        service.loadFlags(configId)
        assertNotNull("Pre-condition: product-configs cache must be populated", service.getProductConfigs(productId))

        service.resetConfigsFlags()

        assertNotNull("resetConfigsFlags() must not clear the product-configs cache", service.getProductConfigs(productId))
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Runs [action] with a no-op [LoggedErrorProcessor] so that expected
     * `thisLogger().error(...)` calls inside production code do not convert
     * into test failures under [LightPlatformTestCase].
     */
    private fun suppressLogErrors(action: () -> Unit) {
        val noOpProcessor = object : LoggedErrorProcessor() {
            override fun processError(
                category: String,
                message: String,
                details: Array<String>,
                t: Throwable?,
            ): Set<LoggedErrorProcessor.Action> = emptySet()
        }
        LoggedErrorProcessor.executeWith<Throwable>(noOpProcessor) { action() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun configFlagsField(service: ConfigCatNodeDataService): Map<UUID, List<SettingModel>> {
        val field: Field = ConfigCatNodeDataService::class.java.getDeclaredField("configFlags")
        field.isAccessible = true
        return field.get(service) as Map<UUID, List<SettingModel>>
    }
}






