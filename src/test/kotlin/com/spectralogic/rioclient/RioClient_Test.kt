/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.time.ZonedDateTime
import java.util.UUID

@Tag("test")
class RioClient_Test {

    private companion object {

        private lateinit var rioClient: RioClient
        private lateinit var spectraDeviceCreateRequest: SpectraDeviceCreateRequest
        private lateinit var spectraDeviceName: String
        private lateinit var spectraDeviceMgmtInterfaceUrl: String
        private lateinit var spectraDeviceUsername: String
        private lateinit var spectraDevicePassword: String
        private lateinit var spectraDeviceMonitorUsername: String
        private lateinit var spectraDeviceMonitorPassword: String

        private lateinit var testBroker: String
        private lateinit var testAgent: String
        private lateinit var brokerBucket: String

        private lateinit var divaEndpoint: String
        private lateinit var divaUsername: String
        private lateinit var divaPassword: String
        private lateinit var divaCategory: String

        private const val username = "spectra"
        private const val password = "spectra"

        private const val invalidNameMsg = "names can only contain the characters: [a-z], [0-9], '-' and '_'"

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            rioClient = RioClient(URL(getenvValue("ESCAPEPOD_URL", "https://localhost:5050")), username, password)

            spectraDeviceName = getenvValue("BP_DEVICE_NAME", "rioclient_bp")
            spectraDeviceMgmtInterfaceUrl = getenvValue("MGMT_INTERFACE_URL", "https://sm25-2-mgmt.eng.sldomain.com")
            spectraDeviceUsername = getenvValue("BP_DEVICE_USERNAME", "Administrator")
            spectraDevicePassword = getenvValue("BP_DEVICE_PASSWORD", "spectra")
            spectraDeviceMonitorUsername = getenvValue("BP_DEVICE_MONITOR_USERNAME", "monitor")
            spectraDeviceMonitorPassword = getenvValue("BP_DEVICE_MONITOR_PASSWORD", "monitor")
            spectraDeviceCreateRequest = SpectraDeviceCreateRequest(
                spectraDeviceName,
                spectraDeviceMgmtInterfaceUrl,
                spectraDeviceUsername,
                spectraDevicePassword
            )

            testBroker = getenvValue("DEFAULT_BROKER", "rioclient-broker")
            testAgent = getenvValue("DEFAULT_AGENT", "rioclient-agent")
            brokerBucket = getenvValue("DEFAULT_BUCKET", "rioclient-testbucket")

            divaEndpoint = getenvValue("DIVA_ENDPOINT", "http://10.85.41.92:9763/services/DIVArchiveWS_SOAP_2.1?wsdl")
            divaUsername = getenvValue("DIVA_USERNAME", "user")
            divaPassword = getenvValue("DIVA_PASSWORD", "pass")
            divaCategory = getenvValue("DIVA_CATEGORY", "DVT-10")
        }
    }

    @BeforeEach
    fun beforeEach() = blockingTest {
        rioClient.ensureSpectraDeviceExists(spectraDeviceCreateRequest)
    }

    @Test
    fun spectraDeviceTest() = blockingTest {
        val spectraDeviceName = "bp-${uuid()}"
        val spectraDeviceRequest = spectraDeviceCreateRequest.copy(name = spectraDeviceName)
        val spectraDeviceResponse = rioClient.createSpectraDevice(spectraDeviceRequest)
        assertThat(spectraDeviceResponse.name).isEqualTo(spectraDeviceName)

        var spectraDeviceList = rioClient.listSpectraDevices()
        val spectraDeviceTotal = spectraDeviceList.page.totalItems
        assertThat(spectraDeviceList.objects.map { it.name }).contains(spectraDeviceCreateRequest.name)
        assertThat(spectraDeviceList.objects.map { it.name }).contains(spectraDeviceName)
        assertThat(spectraDeviceTotal).isGreaterThanOrEqualTo(2)

        assertThat(rioClient.headSpectraDevice(spectraDeviceName)).isTrue
        assertThat(rioClient.headDevice("spectra", spectraDeviceName)).isTrue

        val getSpectraDevice = rioClient.getSpectraDevice(spectraDeviceName)
        assertThat(getSpectraDevice.name).isEqualTo(spectraDeviceRequest.name)
        assertThat(getSpectraDevice.mgmtInterface).isEqualTo(spectraDeviceRequest.mgmtInterface)
        assertThat(getSpectraDevice.username).isEqualTo(spectraDeviceRequest.username)

        rioClient.deleteDevice("spectra", spectraDeviceName)
        assertThat(rioClient.headSpectraDevice(spectraDeviceName)).isFalse
        assertThat(rioClient.headDevice("spectra", spectraDeviceName)).isFalse

        spectraDeviceList = rioClient.listSpectraDevices()
        assertThat(spectraDeviceList.objects.map { it.name }).doesNotContain(spectraDeviceName)
        assertThat(rioClient.headSpectraDevice(spectraDeviceName)).isFalse

        // TODO: spectra device update after ESCP-3519

        // device unhappy path testing
        listOf(
            Pair(spectraDeviceCreateRequest.copy(name = "Bad&Name"), invalidNameMsg),
            Pair(spectraDeviceCreateRequest.copy(mgmtInterface = "https://badhost.eng.sldomain.com"), "unknown_host"),
            Pair(spectraDeviceCreateRequest.copy(username = "bad-username"), "invalid_credentials"),
            Pair(spectraDeviceCreateRequest.copy(password = "bad-password"), "invalid_credentials")
        ).forEach { (request, message) ->
            val error = catchThrowableOfType(
                {
                    runBlocking {
                        rioClient.createSpectraDevice(request)
                    }
                },
                RioHttpException::class.java
            )
            assertThat(error.payload()).contains(message)
        }
    }

    // TODO: vailDeviceTest
    // TODO: flashnetDeviceTest
    // TODO: tbpfrDeviceTest

    @Test
    fun divaTest() = blockingTest {
        ensureBrokerExists()
        val divaDeviceName = "diva-device-${uuid()}"
        val divaAgentName = "diva-agent-${uuid()}"
        try {
            var divaDeviceList = rioClient.listDivaDevices()
            val totalDivaDevices = divaDeviceList.page.totalItems

            val divaDeviceRequest = DivaDeviceCreateRequest(divaDeviceName, divaEndpoint, divaUsername, divaPassword)
            val divaDeviceResponse = rioClient.createDivaDevice(divaDeviceRequest)
            assertThat(divaDeviceResponse.name).isEqualTo(divaDeviceName)
            assertThat(divaDeviceResponse.endpoint).isEqualTo(divaEndpoint)
            assertThat(divaDeviceResponse.username).isEqualTo(divaUsername)

            assertThat(rioClient.headDivaDevice(divaDeviceName)).isTrue
            assertThat(rioClient.headDevice("diva", divaDeviceName)).isTrue

            val getDivaDevice = rioClient.getDivaDevice(divaDeviceName)
            assertThat(getDivaDevice).isEqualTo(divaDeviceResponse)

            divaDeviceList = rioClient.listDivaDevices()
            assertThat(divaDeviceList.page.totalItems).isEqualTo(totalDivaDevices + 1)

            val divaAgentConfig = DivaAgentConfig(divaDeviceName, divaCategory, null, null)
            val divaAgentRequest = AgentCreateRequest(divaAgentName, "diva_agent", divaAgentConfig.toConfigMap())
            val divaAgentCreate = rioClient.createAgent(testBroker, divaAgentRequest)
            assertThat(divaAgentCreate.name).isEqualTo(divaAgentRequest.name)
            assertThat(divaAgentCreate.writable).isFalse
            assertThat(divaAgentCreate.agentConfig).isEqualTo(divaAgentConfig.toConfigMap())

            assertThat(rioClient.headAgent(testBroker, divaAgentName)).isTrue

            var divaAgent = rioClient.getAgent(testBroker, divaAgentName)
            assertThat(divaAgent).isEqualTo(divaAgentCreate)
            assertThat(divaAgent.lastIndexDate).isNull()

            var i = 25
            while (divaAgent.indexState != "COMPLETE" && --i > 0) {
                delay(3000)
                divaAgent = rioClient.getAgent(testBroker, divaAgentName, true)
            }
            assertThat(divaAgent.indexState).isEqualTo("COMPLETE")
            assertThat(divaAgent.lastIndexDate).isNotNull

            rioClient.deleteAgent(testBroker, divaAgentName, true)
            assertThat(rioClient.headAgent(testBroker, divaAgentName)).isFalse

            // TODO: diva device update after ESCP-3519

            // TODO agent unhappy path testing

            rioClient.deleteDivaDevice(divaDeviceName)
            assertThat(rioClient.headDivaDevice(divaDeviceName)).isFalse
            assertThat(rioClient.headDevice("diva", divaDeviceName)).isFalse

            divaDeviceList = rioClient.listDivaDevices()
            assertThat(divaDeviceList.page.totalItems).isEqualTo(totalDivaDevices)

            // device unhappy path testing
            listOf(
                Pair(divaDeviceRequest.copy(name = "BAD&NAME"), invalidNameMsg),
                Pair(divaDeviceRequest.copy(endpoint = "https://badhost.eng.sldomain.com"), "Downstream service is unavailable")
            ).forEach { (request, message) ->
                val error = catchThrowableOfType(
                    {
                        runBlocking {
                            rioClient.createDivaDevice(request)
                        }
                    },
                    RioHttpException::class.java
                )
                assertThat(error).isNotNull
                assertThat(error.payload()).contains(message)
            }
        } finally {
            if (rioClient.headAgent(testBroker, divaAgentName)) {
                rioClient.deleteAgent(testBroker, divaAgentName, true)
            }
            if (rioClient.headDivaDevice(divaDeviceName)) {
                rioClient.deleteDivaDevice(divaDeviceName)
            }
        }
    }

    @Test
    fun endPointTest(@TempDir uriDir: Path) = blockingTest {

        val ftpName = "ftp-${uuid()}"
        val ftpRequest = FtpEndpointDeviceCreateRequest(ftpName, "ftp://ftp.test.com", "user", "pass")
        val ftpResponse = rioClient.createFtpEndpointDevice(ftpRequest)
        assertThat(ftpResponse.name).isEqualTo(ftpName)
        assertThat(ftpResponse.endpoint).isEqualTo(ftpRequest.endpoint)
        assertThat(ftpResponse.username).isEqualTo(ftpRequest.username)
        assertThat(ftpResponse.type).isEqualTo(ftpRequest.type)

        val getFtp = rioClient.getFtpEndpointDevice(ftpName)
        assertThat(getFtp).isEqualTo(ftpResponse)

        val genericFtp = rioClient.getEndpointDevice(ftpName)
        assertThat(genericFtp.name).isEqualTo(ftpName)
        assertThat(genericFtp.type).isEqualTo(ftpRequest.type)

        val uriName = "uri-${uuid()}"
        val uriRequest = UriEndpointDeviceCreateRequest(uriName, uriDir.toUri().toString())
        val uriResponse = rioClient.createUriEndpointDevice(uriRequest)
        assertThat(uriResponse.name).isEqualTo(uriName)
        assertThat(uriResponse.endpoint).isEqualTo(uriRequest.endpoint)
        assertThat(uriResponse.type).isEqualTo(uriRequest.type)

        val getUri = rioClient.getUriEndpointDevice(uriName)
        assertThat(getUri).isEqualTo(uriResponse)

        val genericUri = rioClient.getEndpointDevice(uriName)
        assertThat(genericUri.name).isEqualTo(uriName)
        assertThat(genericUri.type).isEqualTo(uriRequest.type)

        var endpointList = rioClient.listEndpointDevices()
        val endpointCount = endpointList.page.totalItems
        assertThat(endpointList.objects.map { it.name }).contains(ftpName)
        assertThat(endpointList.objects.map { it.name }).contains(uriName)
        assertThat(endpointCount).isGreaterThanOrEqualTo(2)

        assertThat(rioClient.headEndpointDevice(ftpName)).isTrue
        assertThat(rioClient.headEndpointDevice(uriName)).isTrue

        rioClient.deleteEndpointDevice(ftpName)
        rioClient.deleteEndpointDevice(uriName)

        assertThat(rioClient.headEndpointDevice(ftpName)).isFalse
        assertThat(rioClient.headEndpointDevice(uriName)).isFalse

        endpointList = rioClient.listEndpointDevices()
        assertThat(endpointList.page.totalItems).isEqualTo(endpointCount - 2)

        // TODO: endpoint unhappy path testing
    }

    @Test
    fun brokerTest() = blockingTest {
        try {
            if (rioClient.headBroker(testBroker)) {
                rioClient.deleteBroker(testBroker, true)
            }
            assertThat(rioClient.headBroker(testBroker)).isFalse

            val agentConfig = BpAgentConfig(brokerBucket, spectraDeviceCreateRequest.name, spectraDeviceCreateRequest.username)
            val createRequest = BrokerCreateRequest(testBroker, testAgent, agentConfig)

            rioClient.createBroker(createRequest)
            assertThat(rioClient.headBroker(testBroker)).isTrue

            val getBroker = rioClient.getBroker(testBroker)
            assertThat(getBroker.name).isEqualTo(testBroker)

            val listBrokers = rioClient.listBrokers()
            assertThat(listBrokers.objects).isNotEmpty
            assertThat(listBrokers.objects.map { it.name }).contains(testBroker)

            val getWriteAgent = rioClient.getAgent(testBroker, testAgent)
            assertThat(getWriteAgent.name).isEqualTo(testAgent)
            assertThat(getWriteAgent.type).isEqualTo("bp_agent")
            assertThat(getWriteAgent.writable).isEqualTo(true)
            assertThat(getWriteAgent.agentConfig).isEqualTo(agentConfig.toConfigMap())

            val listAgents = rioClient.listAgents(testBroker)
            assertThat(listAgents.objects).hasSize(1)
            assertThat(listAgents.objects.first().name).isEqualTo(getWriteAgent.name)

            val readAgentName = "test-read-agent"
            assertThat(rioClient.headAgent(testBroker, readAgentName)).isFalse

            val agentCreateRequest = AgentCreateRequest(readAgentName, "bp_agent", agentConfig.toConfigMap())
            val createAgent = rioClient.createAgent(testBroker, agentCreateRequest)
            assertThat(createAgent.name).isEqualTo(readAgentName)
            assertThat(createAgent.type).isEqualTo("bp_agent")
            assertThat(createAgent.writable).isEqualTo(false)
            assertThat(createAgent.agentConfig).isEqualTo(agentConfig.toConfigMap())

            var getReadAgent = rioClient.getAgent(testBroker, readAgentName)
            assertThat(getReadAgent).isEqualTo(createAgent)
            assertThat(getReadAgent.lastIndexDate).isNull()

            var i = 25
            while (getReadAgent.indexState != "COMPLETE" && --i > 0) {
                delay(100)
                getReadAgent = rioClient.getAgent(testBroker, readAgentName, true)
            }
            assertThat(getReadAgent.lastIndexDate).isNotNull
            assertThat(getReadAgent.indexState).isEqualTo("COMPLETE")

            rioClient.indexAgent(testBroker, readAgentName, index = true)

            assertThat(rioClient.headAgent(testBroker, readAgentName)).isTrue
            rioClient.deleteAgent(testBroker, readAgentName, true)
            assertThat(rioClient.headAgent(testBroker, readAgentName)).isFalse

            // TODO broker unhappy path testing
            // TODO agent unhappy path testing
        } finally {
            removeBroker()
        }
    }

    @Test
    fun jobTest() = blockingTest {
        try {
            ensureBrokerExists()

            val archiveJobName = "archive-job-${uuid()}"
            val metadata = mapOf(Pair("key1", "val1"), Pair("key2", "val2"))
            val archiveRequest = ArchiveRequest(
                archiveJobName,
                listOf(
                    FileToArchive(uuid(), URI("aToZSequence://file"), 1024L, metadata),
                    FileToArchive(uuid(), URI("aToZSequence://file"), 2048L, metadata),
                )
            )
            val archiveJob = rioClient.createArchiveJob(testBroker, archiveRequest)
            assertThat(archiveJob.name).isEqualTo(archiveJobName)
            assertThat(archiveJob.numberOfFiles).isEqualTo(2)
            assertThat(archiveJob.totalSizeInBytes).isEqualTo(3072)

            assertThat(rioClient.headJob(archiveJob.id.toString())).isTrue

            var i = 25
            var archiveJobStatus = rioClient.jobStatus(archiveJob.id)
            while (archiveJobStatus.status.status == "ACTIVE" && --i > 0) {
                delay(100)
                archiveJobStatus = rioClient.jobStatus(archiveJob.id)
            }
            assertThat(archiveJobStatus.status.status).isEqualTo("COMPLETED")
            assertThat(archiveJobStatus.filesTransferred).isEqualTo(2)
            assertThat(archiveJobStatus.progress).isEqualTo(1.0f)

            val archiveFilesStatus = rioClient.fileStatus(archiveJob.id)
            assertThat(archiveFilesStatus.fileStatus).hasSize(6)

            val archiveFileStatus = rioClient.fileStatus(archiveJob.id, archiveRequest.files[0].name)
            assertThat(archiveFileStatus.fileStatus).hasSize(3)

            val restoreJobName = "restore-job-${uuid()}"
            val restoreRequest = RestoreRequest(
                restoreJobName,
                listOf(
                    FileToRestore(archiveRequest.files[0].name, URI("null://name1")),
                    FileToRestore(archiveRequest.files[1].name, URI("null://name2"))
                )
            )
            val restoreJob = rioClient.createRestoreJob(testBroker, restoreRequest)
            assertThat(restoreJob.name).isEqualTo(restoreJobName)
            assertThat(restoreJob.numberOfFiles).isEqualTo(2)

            assertThat(rioClient.headJob(restoreJob.id.toString())).isTrue

            i = 25
            var restoreJobStatus = rioClient.jobStatus(restoreJob.id)
            while (restoreJobStatus.status.status == "ACTIVE" && --i > 0) {
                delay(100)
                restoreJobStatus = rioClient.jobStatus(restoreJob.id)
            }
            assertThat(restoreJobStatus.status.status).isEqualTo("COMPLETED")
            assertThat(restoreJobStatus.filesTransferred).isEqualTo(2)
            assertThat(restoreJobStatus.progress).isEqualTo(1.0f)

            val restoreFilesStatus = rioClient.fileStatus(restoreJob.id)
            assertThat(restoreFilesStatus.fileStatus).hasSize(6)

            val restoreFileStatus = rioClient.fileStatus(restoreJob.id, restoreRequest.files[1].name)
            assertThat(restoreFileStatus.fileStatus).hasSize(3)

            var jobList = rioClient.listJobs(broker = testBroker, jobStatus = "COMPLETED")
            assertThat(jobList.jobs).isNotEmpty
            assertThat(jobList.jobs.map { it.id }).contains(archiveJob.id)
            assertThat(jobList.jobs.map { it.id }).contains(restoreJob.id)

            val totalJobs = jobList.page.totalItems
            rioClient.deleteJob(archiveJob.id)
            rioClient.deleteJob(restoreJob.id)

            assertThat(rioClient.headJob(archiveJob.id.toString())).isFalse
            assertThat(rioClient.headJob(restoreJob.id.toString())).isFalse

            jobList = rioClient.listJobs(broker = testBroker, jobStatus = "COMPLETED")
            assertThat(jobList.page.totalItems).isEqualTo(totalJobs - 2)

            // TODO job unhappy path testing
        } finally {
            removeBroker()
        }
    }

    @Test
    fun objectTest() = blockingTest {
        try {
            ensureBrokerExists()

            var listObjects = rioClient.listObjects(testBroker)
            val totalObjects = listObjects.page.totalItems

            val objectName = "object-${uuid()}"
            val metadata = mapOf(Pair("key1", "val1"))
            val archiveRequest = ArchiveRequest(
                "archive-job-${uuid()}",
                listOf(
                    FileToArchive(objectName, URI("aToZSequence://file"), 1024L, metadata)
                )
            )
            val archiveJob = rioClient.createArchiveJob(testBroker, archiveRequest)

            var i = 25
            var archiveJobStatus = rioClient.jobStatus(archiveJob.id)
            while (archiveJobStatus.status.status == "ACTIVE" && --i > 0) {
                delay(100)
                archiveJobStatus = rioClient.jobStatus(archiveJob.id)
            }
            assertThat(archiveJobStatus.status.status).isEqualTo("COMPLETED")

            listObjects = rioClient.listObjects(testBroker)
            assertThat(listObjects.page.totalItems).isEqualTo(totalObjects + 1)

            val getObject = rioClient.getObject(testBroker, objectName)
            assertThat(getObject.broker).isEqualTo(testBroker)
            assertThat(getObject.name).isEqualTo(objectName)
            assertThat(getObject.size).isEqualTo(archiveRequest.files[0].size)
            assertThat(getObject.metadata).isEqualTo(metadata)

            val newMetadata = mapOf(Pair("key9", "val9"))
            val updateObject = rioClient.updateObject(testBroker, objectName, newMetadata)
            assertThat(updateObject).isEqualTo(getObject.copy(metadata = newMetadata))

            assertThat(rioClient.objectExists(testBroker, objectName)).isTrue

            rioClient.deleteObject(testBroker, objectName)
            assertThat(rioClient.objectExists(testBroker, objectName)).isFalse

            listObjects = rioClient.listObjects(testBroker)
            assertThat(listObjects.page.totalItems).isEqualTo(totalObjects)

            // TODO: object unhappy path testing
        } finally {
            removeBroker()
        }
    }

    @Test
    fun logTest() = blockingTest {

        var listLogs = rioClient.listLogsets()
        val totalLogs = listLogs.page.totalItems

        val newLog = rioClient.createLogset()

        var i = 25
        var getLog = rioClient.getLogset(newLog.id)
        while (getLog.status != "COMPLETE" && --i > 0) {
            delay(1000)
            getLog = rioClient.getLogset(newLog.id)
        }
        assertThat(getLog.status).isEqualTo("COMPLETE")

        assertThat(rioClient.headLogset(UUID.fromString(newLog.id))).isTrue

        listLogs = rioClient.listLogsets()
        assertThat(listLogs.page.totalItems).isEqualTo(totalLogs + 1)
        assertThat(listLogs.objects.map { it.id }).contains(newLog.id)

        rioClient.deleteLogset(UUID.fromString(newLog.id))
        assertThat(rioClient.headLogset(UUID.fromString(newLog.id))).isFalse

        listLogs = rioClient.listLogsets()
        assertThat(listLogs.page.totalItems).isEqualTo(totalLogs)

        // TODO: log unhappy path testing
    }

    @Test
    fun systemTest() = blockingTest {
        rioClient.systemInfo()
    }

    @Test
    fun keysTest() = blockingTest {
        var listTokens = rioClient.listTokenKeys()
        val totalTokens = listTokens.page.totalItems

        val createToken = rioClient.createApiToken(TokenCreateRequest())
        assertThat(createToken.userName).isEqualTo(username)
        assertThat(createToken.creationDate).isNotNull
        assertThat(createToken.id).isNotNull
        assertThat(createToken.expirationDate).isNull()

        val expireZdt = ZonedDateTime.now().plusDays(2)
        val longToken = rioClient.createApiToken(TokenCreateRequest(expireZdt.toString()))
        assertThat(longToken.userName).isEqualTo(username)
        assertThat(longToken.expirationDate).isNotNull
        val expirationDate = ZonedDateTime.parse(longToken.expirationDate)
        assertThat(expirationDate).isBefore(expireZdt.plusMinutes(1))
        assertThat(expirationDate).isAfter(expireZdt.plusMinutes(-1))

        val getToken = rioClient.getApiToken(createToken.id)
        assertThat(getToken.id).isEqualTo(createToken.id)
        assertThat(getToken.userName).isEqualTo(createToken.userName)
        assertThat(getToken.creationDate).isEqualTo(createToken.creationDate)
        assertThat(getToken.expirationDate).isEqualTo(createToken.expirationDate)

        listTokens = rioClient.listTokenKeys()
        assertThat(listTokens.page.totalItems).isEqualTo(totalTokens + 2)
        assertThat(listTokens.objects.map { it.id }).containsAll(listOf(createToken.id, longToken.id))

        assertThat(rioClient.headApiToken(createToken.id)).isTrue
        rioClient.deleteApiToken(createToken.id)
        assertThat(rioClient.headApiToken(createToken.id)).isFalse

        assertThat(rioClient.headApiToken(longToken.id)).isTrue
        rioClient.deleteApiToken(longToken.id)
        assertThat(rioClient.headApiToken(longToken.id)).isFalse

        listTokens = rioClient.listTokenKeys()
        assertThat(listTokens.page.totalItems).isEqualTo(totalTokens)

        // TODO: keys unhappy path testing
    }

    private suspend fun ensureBrokerExists() {
        if (!rioClient.headBroker(testBroker)) {
            val agentConfig = BpAgentConfig(brokerBucket, spectraDeviceCreateRequest.name, spectraDeviceCreateRequest.username)
            val createRequest = BrokerCreateRequest(testBroker, testAgent, agentConfig)
            rioClient.createBroker(createRequest)
        }
    }

    private suspend fun removeBroker() {
        if (rioClient.headBroker(testBroker)) {
            rioClient.deleteBroker(testBroker, true)
        }
    }

    private fun uuid(): String = UUID.randomUUID().toString()

    fun blockingTest(test: suspend () -> Unit) {
        runBlocking { test() }
    }
}

private fun getenvValue(key: String, default: String): String =
    System.getenv(key) ?: default
