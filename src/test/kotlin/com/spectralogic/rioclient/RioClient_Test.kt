/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

import java.net.URL
import java.util.UUID

@Tag("test")
class RioClient_Test {

    private companion object {

        // TODO: private lateinit var bpClient: Ds3Client

        private lateinit var rioClient: RioClient
        private lateinit var spectraDeviceCreateRequest: SpectraDeviceCreateRequest
        private lateinit var testBroker: String
        private lateinit var testAgent: String

        private val testUuid = UUID.randomUUID().toString()
        // TODO: private var testBucket = "testBucket-$testUuid"
        private const val testBucket = "testBucket-rioclient"
        private val mapper = ObjectMapper()

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            rioClient = RioClient(URL(getenvValue("ESCAPEPOD_URL","https://localhost:5050")))
            spectraDeviceCreateRequest = SpectraDeviceCreateRequest(
                "rioclient_bp",
                getenvValue("MGMT_INTERFACE_URL", "https://sm25-2-mgmt.eng.sldomain.com"),
                "Administrator",
                "spectra"
                // getenvValue("DATA_PATH", "").ifBlank { null }
            )
            testBroker = getenvValue("DEFAULT_BROKER", "rioclient-broker")
            testAgent = getenvValue("DEFAULT_AGENT", "rioclient-agent")
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            // TODO clean up bucket
        }
    }

    @BeforeEach
    fun beforeEach() = blockingTest {
        rioClient.ensureSpectraDeviceExists(spectraDeviceCreateRequest)
    }

    @Test
    fun brokerTest() = blockingTest {
        try {
            if (rioClient.headBroker(testBroker)) {
                rioClient.deleteBroker(testBroker, true)
            }
            assertThat(rioClient.headBroker(testBroker)).isFalse

            val agentConfig = BpAgentConfig(testBucket, spectraDeviceCreateRequest.name, spectraDeviceCreateRequest.username)
            val createRequest = BrokerCreateRequest(testBroker, testAgent, agentConfig)

            rioClient.createBroker(createRequest)
            assertThat(rioClient.headBroker(testBroker)).isTrue

            val getBroker = rioClient.getBroker(testBroker)
            assertThat(getBroker.name).isEqualTo(testBroker)
            assertThat(getBroker.objectCount).isEqualTo(0)

            val listBrokers = rioClient.listBrokers()
            assertThat(listBrokers.objects).isNotEmpty
            assertThat(listBrokers.objects).contains(getBroker)

            val getWriteAgent = rioClient.getAgent(testBroker, testAgent)
            assertThat(getWriteAgent.name).isEqualTo(testAgent)
            assertThat(getWriteAgent.type).isEqualTo("bp_agent")
            assertThat(getWriteAgent.writable).isEqualTo(true)
            assertThat(getWriteAgent.agentConfig).isEqualTo(agentConfig.toImmutableMap())

            val listAgents = rioClient.listAgents(testBroker)
            assertThat(listAgents.agents).hasSize(1)
            assertThat(listAgents.agents.first()).isEqualTo(getWriteAgent)

            val readAgentName = "test-read-agent"
            assertThat(rioClient.headAgent(testBroker, readAgentName)).isFalse

            val agentCreateRequest = AgentCreateRequest(readAgentName, "bp_agent", agentConfig.toImmutableMap())
            val createAgent = rioClient.createAgent(testBroker, agentCreateRequest)
            assertThat(createAgent.name).isEqualTo(readAgentName)
            assertThat(createAgent.type).isEqualTo("bp_agent")
            assertThat(createAgent.writable).isEqualTo(false)
            assertThat(createAgent.agentConfig).isEqualTo(agentConfig.toImmutableMap())

            val getReadAgent = rioClient.getAgent(testBroker, readAgentName)
            assertThat(getReadAgent).isEqualTo(createAgent)
            assertThat(getReadAgent.lastIndexDate).isNull()

            delay(100)
            rioClient.indexAgent(testBroker, readAgentName, index = true)
            val getReindexedAgent = rioClient.getAgent(testBroker, readAgentName)
            assertThat(getReindexedAgent.lastIndexDate).isNotNull

            assertThat(rioClient.headAgent(testBroker, readAgentName)).isTrue
            rioClient.deleteAgent(testBroker, readAgentName, false)
            assertThat(rioClient.headAgent(testBroker, readAgentName)).isFalse

        } finally {
            if (rioClient.headBroker(testBroker)) {
                rioClient.deleteBroker(testBroker, true)
            }
        }
    }

    @Test
    fun listBrokers() = blockingTest {
        val response = rioClient.listBrokers()
        println("DWL: $response")
    }


    // listSpectraDevices
    @Test
    fun listSpectraDevices() = blockingTest {
        val response = rioClient.listSpectraDevices()
        println("DWL: $response")
    }

    fun blockingTest(test: suspend () -> Unit) {
        runBlocking { test() }
    }
}

private fun getenvValue(key: String, default: String): String =
    System.getenv(key) ?: default


