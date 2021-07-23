/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.URI
import java.net.URL
import java.nio.file.Path
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
            rioClient = RioClient(URL(getenvValue("ESCAPEPOD_URL", "https://localhost:5050")))
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

        /*@JvmStatic
        @AfterAll
        fun afterAll() =  {
        }*/
    }

    @BeforeEach
    fun beforeEach() = blockingTest {
        rioClient.ensureSpectraDeviceExists(spectraDeviceCreateRequest)
    }

    @Test
    fun spectraDeviceTest() = blockingTest {

        val newDeviceName = "bp-${uuid()}"
        val newDeviceRequest = spectraDeviceCreateRequest.copy(name = newDeviceName)
        val spectraDeviceResponse = rioClient.createSpectraDevice(newDeviceRequest)
        assertThat(spectraDeviceResponse.name).isEqualTo(newDeviceName)

        var spectraDeviceList = rioClient.listSpectraDevices()
        val spectraDeviceTotal = spectraDeviceList.page.totalItems
        assertThat(spectraDeviceList.devices.map { it.name }).contains(spectraDeviceCreateRequest.name)
        assertThat(spectraDeviceList.devices.map { it.name }).contains(newDeviceName)
        assertThat(spectraDeviceTotal).isGreaterThanOrEqualTo(2)

        assertThat(rioClient.headSpectraDevice(newDeviceName)).isTrue

        val getSpectraDevice = rioClient.getSpectraDevice(newDeviceName)
        assertThat(getSpectraDevice.name).isEqualTo(newDeviceRequest.name)
        assertThat(getSpectraDevice.mgmtInterface).isEqualTo(newDeviceRequest.mgmtInterface)
        assertThat(getSpectraDevice.username).isEqualTo(newDeviceRequest.username)

        rioClient.deleteSpectraDevice(newDeviceName)
        assertThat(rioClient.headSpectraDevice(newDeviceName)).isFalse

        spectraDeviceList = rioClient.listSpectraDevices()
        assertThat(spectraDeviceList.devices.map { it.name }).doesNotContain(newDeviceName)
        assertThat(rioClient.headSpectraDevice(newDeviceName)).isFalse
    }

    // TODO: Flashnet, TBPFR

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
        assertThat(endpointList.devices.map { it.name }).contains(ftpName)
        assertThat(endpointList.devices.map { it.name }).contains(uriName)
        assertThat(endpointCount).isGreaterThanOrEqualTo(2)

        assertThat(rioClient.headEndpointDevice(ftpName)).isTrue
        assertThat(rioClient.headEndpointDevice(uriName)).isTrue

        rioClient.deleteEndpointDevice(ftpName)
        rioClient.deleteEndpointDevice(uriName)

        assertThat(rioClient.headEndpointDevice(ftpName)).isFalse
        assertThat(rioClient.headEndpointDevice(uriName)).isFalse

        endpointList = rioClient.listEndpointDevices()
        assertThat(endpointList.page.totalItems).isEqualTo(endpointCount - 2)
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

            val listBrokers = rioClient.listBrokers()
            assertThat(listBrokers.objects).isNotEmpty
            assertThat(listBrokers.objects.map { it.name }).contains(testBroker)

            val getWriteAgent = rioClient.getAgent(testBroker, testAgent)
            assertThat(getWriteAgent.name).isEqualTo(testAgent)
            assertThat(getWriteAgent.type).isEqualTo("bp_agent")
            assertThat(getWriteAgent.writable).isEqualTo(true)
            assertThat(getWriteAgent.agentConfig).isEqualTo(agentConfig.toMap())

            val listAgents = rioClient.listAgents(testBroker)
            assertThat(listAgents.agents).hasSize(1)
            assertThat(listAgents.agents.first().name).isEqualTo(getWriteAgent.name)

            val readAgentName = "test-read-agent"
            assertThat(rioClient.headAgent(testBroker, readAgentName)).isFalse

            val agentCreateRequest = AgentCreateRequest(readAgentName, "bp_agent", agentConfig.toMap())
            val createAgent = rioClient.createAgent(testBroker, agentCreateRequest)
            assertThat(createAgent.name).isEqualTo(readAgentName)
            assertThat(createAgent.type).isEqualTo("bp_agent")
            assertThat(createAgent.writable).isEqualTo(false)
            assertThat(createAgent.agentConfig).isEqualTo(agentConfig.toMap())

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
            val archiveJob = rioClient.archiveFile(testBroker, archiveRequest)
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
            val restoreJob = rioClient.restoreFile(testBroker, restoreRequest)
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
            val archiveJob = rioClient.archiveFile(testBroker, archiveRequest)

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
        } finally {
            removeBroker()
        }
    }

    @Test
    fun logTest() = blockingTest {

        var listLogs = rioClient.listLogs()
        val totalLogs = listLogs.page.totalItems

        val newLog = rioClient.newLog()

        var i = 25
        var getLog = rioClient.getLogset(newLog.id)
        while (getLog.status != "COMPLETE" && --i > 0) {
            delay(250)
            println("DWL: ${getLog.status}")
            getLog = rioClient.getLogset(newLog.id)
        }
        assertThat(getLog.status).isEqualTo("COMPLETE")

        assertThat(rioClient.headLogset(UUID.fromString(newLog.id))).isTrue

        listLogs = rioClient.listLogs()
        assertThat(listLogs.page.totalItems).isEqualTo(totalLogs + 1)
        assertThat(listLogs.logs.map { it.id }).contains(newLog.id)

        rioClient.deleteLogset(UUID.fromString(newLog.id))
        assertThat(rioClient.headLogset(UUID.fromString(newLog.id))).isFalse

        listLogs = rioClient.listLogs()
        assertThat(listLogs.page.totalItems).isEqualTo(totalLogs)
    }

    @Test
    fun systemTest() = blockingTest {
        rioClient.systemInfo()
    }

    private suspend fun ensureBrokerExists() {
        if (!rioClient.headBroker(testBroker)) {
            val agentConfig = BpAgentConfig(testBucket, spectraDeviceCreateRequest.name, spectraDeviceCreateRequest.username)
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
