package com.configcat.intellij.plugin.services

import com.intellij.testFramework.LightPlatformTestCase

class ConfigCatPropertiesServiceTest : LightPlatformTestCase() {

    private lateinit var service: ConfigCatPropertiesService

    override fun setUp() {
        super.setUp()
        service = ConfigCatPropertiesService()
        // Ensure a clean state before every test
        service.propertiesComponent.unsetValue(ConfigCatPropertiesService.CONFIGCAT_CONNECTED_CONFIG_ID)
    }

    override fun tearDown() {
        // Clean up persisted value so other tests are not affected
        service.propertiesComponent.unsetValue(ConfigCatPropertiesService.CONFIGCAT_CONNECTED_CONFIG_ID)
        super.tearDown()
    }

    // -------------------------------------------------------------------------
    // getConnectedConfig
    // -------------------------------------------------------------------------

    fun testGetConnectedConfig_notSet_returnsNull() {
        val result = service.getConnectedConfig()

        assertNull("getConnectedConfig() must return null when nothing has been stored yet", result)
    }

    fun testGetConnectedConfig_afterSetConnectedConfig_returnsStoredValue() {
        val configId = "3fa85f64-5717-4562-b3fc-2c963f66afa6"

        service.setConnectedConfig(configId)
        val result = service.getConnectedConfig()

        assertEquals("getConnectedConfig() must return the value that was stored by setConnectedConfig()", configId, result)
    }

    fun testSetConnectedConfig_overwritesPreviousValue_getReturnsLatestValue() {
        val firstConfigId = "aaaaaaaa-0000-0000-0000-000000000001"
        val secondConfigId = "bbbbbbbb-0000-0000-0000-000000000002"

        service.setConnectedConfig(firstConfigId)
        service.setConnectedConfig(secondConfigId)
        val result = service.getConnectedConfig()

        assertEquals(
            "setConnectedConfig() must overwrite the previous value; getConnectedConfig() must return the latest one",
            secondConfigId,
            result
        )
    }

    fun testGetConnectedConfig_afterUnset_returnsNull() {
        service.setConnectedConfig("some-config-id")
        // Simulate a user disconnecting the config
        service.propertiesComponent.unsetValue(ConfigCatPropertiesService.CONFIGCAT_CONNECTED_CONFIG_ID)

        val result = service.getConnectedConfig()

        assertNull("getConnectedConfig() must return null after the stored value is cleared", result)
    }

    fun testSetConnectedConfig_usesCorrectPropertyKey() {
        val configId = "test-key-check"

        service.setConnectedConfig(configId)

        val rawValue = service.propertiesComponent.getValue(ConfigCatPropertiesService.CONFIGCAT_CONNECTED_CONFIG_ID)
        assertEquals(
            "setConnectedConfig() must persist the value under the CONFIGCAT_CONNECTED_CONFIG_ID key",
            configId,
            rawValue
        )
    }

    fun testSetConnectedConfig_multipleDistinctValues_storedIndependently() {
        val firstId = "config-id-alpha"
        val secondId = "config-id-beta"

        service.setConnectedConfig(firstId)
        assertEquals(firstId, service.getConnectedConfig())

        service.setConnectedConfig(secondId)
        assertEquals(secondId, service.getConnectedConfig())
    }
}


