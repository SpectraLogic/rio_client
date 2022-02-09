/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import io.ktor.http.HttpStatusCode
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
        private lateinit var spectraDeviceAltUsername: String
        private lateinit var spectraDeviceAltPassword: String

        private lateinit var testBroker: String
        private lateinit var testAgent: String
        private lateinit var brokerBucket: String
        private lateinit var brokerObjectBucket: String

        private lateinit var divaEndpoint: String
        private lateinit var divaUsername: String
        private lateinit var divaPassword: String
        private lateinit var divaCategory: String

        private const val username = "spectra"
        private const val password = "spectra"

        private const val deviceResourceErrorFmt = "Resource of type DEVICE and name %s does not exist"
        private const val invalidNameMsg = "names can only contain the characters: [a-z], [0-9], '-' and '_'"
        private const val uriPathFormatErrorFmt = "URI is not properly formatted (Illegal character in path at index %s: %s)"
        private const val uriAuthFormatErrorFmt = "URI is not properly formatted (Illegal character in authority at index %s: %s)"
        private const val emptyError = "cannot be empty or consist only of whitespace"

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            rioClient = RioClient(URL(getenvValue("ESCAPEPOD_URL", "https://localhost:5050")), username, password)

            spectraDeviceName = getenvValue("BP_DEVICE_NAME", "rioclient_bp")
            spectraDeviceMgmtInterfaceUrl = getenvValue("MGMT_INTERFACE_URL", "https://sm25-2-mgmt.eng.sldomain.com")
            spectraDeviceUsername = getenvValue("BP_DEVICE_USERNAME", "Administrator")
            spectraDevicePassword = getenvValue("BP_DEVICE_PASSWORD", "spectra")
            spectraDeviceAltUsername = getenvValue("BP_DEVICE_ALT_USERNAME", "monitor")
            spectraDeviceAltPassword = getenvValue("BP_DEVICE_ALT_PASSWORD", "monitor")
            spectraDeviceCreateRequest = SpectraDeviceCreateRequest(
                spectraDeviceName,
                spectraDeviceMgmtInterfaceUrl,
                spectraDeviceUsername,
                spectraDevicePassword
            )

            testBroker = getenvValue("DEFAULT_BROKER", "rioclient-broker")
            testAgent = getenvValue("DEFAULT_AGENT", "rioclient-agent")
            brokerBucket = getenvValue("DEFAULT_BUCKET", "rioclient-testbucket")
            brokerObjectBucket = getenvValue("DEFAULT_OBJECT_BUCKET", "rioclient-objectbucket")

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
        val nameBaseError = RioValidationMessage("name", "string", "")
        val nameMissingError = nameBaseError.copy(errorType = "missing")
        val nameInvalidError = nameBaseError.copy(errorType = "invalid_device_name", reason = invalidNameMsg)
        val mgmtBaseError = RioValidationMessage("mgmtInterface", "URI", "")
        val mgmtHostError = mgmtBaseError.copy(errorType = "unknown_host")
        val mgmtUriError = mgmtBaseError.copy(errorType = "invalid_format")
        val userBaseError = RioValidationMessage("username", "string", "")
        val userCredsError = userBaseError.copy(errorType = "invalid_credentials")
        val passBaseError = RioValidationMessage("password", "password", "")
        val passCredsError = passBaseError.copy(errorType = "invalid_credentials")

        val spectraDeviceName = "bp-${uuid()}"
        val createRequest = spectraDeviceCreateRequest.copy(name = spectraDeviceName)
        val createResponse = rioClient.createSpectraDevice(createRequest)
        assertThat(createResponse.statusCode).isEqualTo(HttpStatusCode.Created)
        assertThat(createResponse.name).isEqualTo(spectraDeviceName)

        var listResponse = rioClient.listSpectraDevices()
        assertThat(listResponse.statusCode).isEqualTo(HttpStatusCode.OK)
        val spectraDeviceTotal = listResponse.page.totalItems
        assertThat(listResponse.objects.map { it.name }).contains(spectraDeviceName)
        assertThat(spectraDeviceTotal).isGreaterThanOrEqualTo(2)

        assertThat(rioClient.headSpectraDevice(spectraDeviceName)).isTrue
        assertThat(rioClient.headDevice("spectra", spectraDeviceName)).isTrue

        var getResponse = rioClient.getSpectraDevice(spectraDeviceName)
        assertThat(getResponse.statusCode).isEqualTo(HttpStatusCode.OK)
        assertThat(getResponse.name).isEqualTo(createRequest.name)
        assertThat(getResponse.mgmtInterface).isEqualTo(createRequest.mgmtInterface)
        assertThat(getResponse.username).isEqualTo(createRequest.username)

        val updateRequest = SpectraDeviceUpdateRequest(
            createRequest.mgmtInterface,
            spectraDeviceAltUsername,
            spectraDeviceAltPassword,
            createRequest.dataPath
        )
        val updateResponse = rioClient.updateSpectraDevice(spectraDeviceName, updateRequest)
        assertThat(updateResponse.statusCode).isEqualTo(HttpStatusCode.OK)
        assertThat(updateResponse.name).isEqualTo(spectraDeviceName)
        assertThat(updateResponse.username).isEqualTo(spectraDeviceAltUsername)

        getResponse = rioClient.getSpectraDevice(spectraDeviceName)
        assertThat(getResponse.statusCode).isEqualTo(HttpStatusCode.OK)
        assertThat(getResponse.name).isEqualTo(spectraDeviceName)
        assertThat(getResponse.mgmtInterface).isEqualTo(createRequest.mgmtInterface)
        assertThat(getResponse.username).isEqualTo(spectraDeviceAltUsername)

        // update unhappy path
        listOf(
            Pair(
                updateRequest.copy(mgmtInterface = "bad uri"),
                listOf(mgmtUriError.copy(value = "bad uri", reason = uriPathFormatErrorFmt.format("3", "bad uri")))
            ),
            Pair(
                updateRequest.copy(mgmtInterface = "badscheme://bad value"),
                listOf(mgmtUriError.copy(value = "badscheme://bad value", reason = uriAuthFormatErrorFmt.format("12", "badscheme://bad value")))
            ),
            Pair(
                updateRequest.copy(mgmtInterface = "https://badhost.eng.sldomain.com"),
                listOf(mgmtHostError.copy(value = "https://badhost.eng.sldomain.com"))
            ),
            Pair(
                updateRequest.copy(username = ""),
                listOf(userCredsError, passCredsError)
            ),
            Pair(
                updateRequest.copy(username = "bad-username"),
                listOf(userCredsError, passCredsError)
            ),
            Pair(
                updateRequest.copy(username = "bad-username", password = "bad-password"),
                listOf(userCredsError, passCredsError)
            ),
            Pair(
                updateRequest.copy(password = "bad-password"),
                listOf(userCredsError, passCredsError)
            )
        ).forEach { (request, expected) ->
            assertSpectraDeviceUpdateError(spectraDeviceName, request, expected)
        }

        rioClient.deleteDevice("spectra", spectraDeviceName)
        assertThat(rioClient.headSpectraDevice(spectraDeviceName)).isFalse
        assertThat(rioClient.headDevice("spectra", spectraDeviceName)).isFalse

        listResponse = rioClient.listSpectraDevices()
        assertThat(listResponse.statusCode).isEqualTo(HttpStatusCode.OK)
        assertThat(listResponse.objects.map { it.name }).doesNotContain(spectraDeviceName)
        assertThat(rioClient.headSpectraDevice(spectraDeviceName)).isFalse

        // create unhappy path
        listOf(
            Pair(
                createRequest.copy(name = ""),
                listOf(nameMissingError)
            ),
            Pair(
                createRequest.copy(name = "  "),
                listOf(nameInvalidError.copy(value = "  "))
            ),
            Pair(
                createRequest.copy(name = "bad name"),
                listOf(nameInvalidError.copy(value = "bad name"))
            ),
            Pair(
                createRequest.copy(name = "Bad&Name"),
                listOf(nameInvalidError.copy(value = "Bad&Name"))
            ),
            Pair(
                createRequest.copy(mgmtInterface = "bad uri"),
                listOf(mgmtUriError.copy(value = "bad uri", reason = uriPathFormatErrorFmt.format("3", "bad uri")))
            ),
            Pair(
                createRequest.copy(mgmtInterface = "badscheme://bad value"),
                listOf(mgmtUriError.copy(value = "badscheme://bad value", reason = uriAuthFormatErrorFmt.format("12", "badscheme://bad value")))
            ),
            Pair(
                createRequest.copy(mgmtInterface = "https://badhost.eng.sldomain.com"),
                listOf(mgmtHostError.copy(value = "https://badhost.eng.sldomain.com"))
            ),
            Pair(
                createRequest.copy(username = ""),
                listOf(userCredsError, passCredsError)
            ),
            Pair(
                createRequest.copy(username = "bad-username"),
                listOf(userCredsError, passCredsError)
            ),
            Pair(
                createRequest.copy(username = "bad-username", password = "bad-password"),
                listOf(userCredsError, passCredsError)
            ),
            Pair(
                createRequest.copy(password = "bad-password"),
                listOf(userCredsError, passCredsError)
            ),
            Pair(
                createRequest.copy(name = "bad name", mgmtInterface = "bad uri"),
                listOf(mgmtUriError.copy(value = "bad uri", reason = uriPathFormatErrorFmt.format("3", "bad uri")))
            ),
            Pair(
                createRequest.copy(name = "bad name", mgmtInterface = "https://badhost.eng.sldomain.com"),
                listOf(nameInvalidError.copy(value = "bad name"))
            )
        ).forEach { (request, expectedErrors) ->
            assertSpectraDeviceCreateError(request, expectedErrors)
        }

        // update non-existing device
        val ex = catchThrowableOfType(
            {
                runBlocking {
                    rioClient.updateSpectraDevice("bad-name", updateRequest)
                }
            },
            RioHttpException::class.java
        )
        assertRioResourceError(ex, RioResourceErrorMessage(deviceResourceErrorFmt.format("bad-name"), 404, "bad-name", "DEVICE"))
    }

    private fun assertSpectraDeviceCreateError(request: SpectraDeviceCreateRequest, expected: List<RioValidationMessage>) {
        val ex = catchThrowableOfType(
            {
                runBlocking {
                    rioClient.createSpectraDevice(request)
                }
            },
            RioHttpException::class.java
        )
        assertRioValidationError(ex, expected)
    }

    private fun assertSpectraDeviceUpdateError(name: String, request: SpectraDeviceUpdateRequest, expected: List<RioValidationMessage>) {
        val ex = catchThrowableOfType(
            {
                runBlocking {
                    rioClient.updateSpectraDevice(name, request)
                }
            },
            RioHttpException::class.java
        )
        assertRioValidationError(ex, expected)
    }

    // TODO: vailDeviceTest
    // TODO: flashnetDeviceTest
    // TODO: tbpfrDeviceTest

    @Test
    fun divaTest() = blockingTest {
        val nameBaseError = RioValidationMessage("name", "string", "")
        val nameMissingError = nameBaseError.copy(errorType = "missing")
        val nameInvalidError = nameBaseError.copy(errorType = "invalid_device_name", reason = invalidNameMsg)
        val endpointBaseError = RioValidationMessage("endpoint", "URI", "")
        val endpointMissingError = endpointBaseError.copy(errorType = "missing", fieldType = "string", reason = emptyError)
        val endpointUriError = endpointBaseError.copy(errorType = "invalid_uri")

        ensureBrokerExists()
        val divaDeviceName = "diva-device-${uuid()}"
        val divaAgentName = "diva-agent-${uuid()}"
        try {
            var listResponse = rioClient.listDivaDevices()
            assertThat(listResponse.statusCode).isEqualTo(HttpStatusCode.OK)
            val totalDivaDevices = listResponse.page.totalItems

            val createRequest = DivaDeviceCreateRequest(divaDeviceName, divaEndpoint, divaUsername, divaPassword)
            val createResponse = rioClient.createDivaDevice(createRequest)
            assertThat(createResponse.statusCode).isEqualTo(HttpStatusCode.Created)
            assertThat(createResponse.name).isEqualTo(divaDeviceName)
            assertThat(createResponse.endpoint).isEqualTo(divaEndpoint)
            assertThat(createResponse.username).isEqualTo(divaUsername)

            assertThat(rioClient.headDivaDevice(divaDeviceName)).isTrue
            assertThat(rioClient.headDevice("diva", divaDeviceName)).isTrue

            var getResponse = rioClient.getDivaDevice(divaDeviceName)
            assertThat(getResponse.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(getResponse).isEqualTo(createResponse)

            listResponse = rioClient.listDivaDevices()
            assertThat(listResponse.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(listResponse.page.totalItems).isEqualTo(totalDivaDevices + 1)

            val divaAgentConfig = DivaAgentConfig(divaDeviceName, divaCategory, null, null)
            val createAgentRequest = AgentCreateRequest(divaAgentName, "diva_agent", divaAgentConfig.toConfigMap())
            val createAgentResponse = rioClient.createAgent(testBroker, createAgentRequest)
            assertThat(createAgentResponse.statusCode).isEqualTo(HttpStatusCode.Created)
            assertThat(createAgentResponse.name).isEqualTo(createAgentRequest.name)
            assertThat(createAgentResponse.writable).isFalse
            assertThat(createAgentResponse.agentConfig).isEqualTo(divaAgentConfig.toConfigMap())

            assertThat(rioClient.headAgent(testBroker, divaAgentName)).isTrue

            var getAgentResponse = rioClient.getAgent(testBroker, divaAgentName)
            assertThat(getAgentResponse.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(getAgentResponse).isEqualTo(createAgentResponse)
            assertThat(getAgentResponse.lastIndexDate).isNull()

            var i = 25
            while (getAgentResponse.indexState != "COMPLETE" && --i > 0) {
                delay(3000)
                getAgentResponse = rioClient.getAgent(testBroker, divaAgentName, true)
            }
            assertThat(getAgentResponse.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(getAgentResponse.indexState).isEqualTo("COMPLETE")
            assertThat(getAgentResponse.lastIndexDate).isNotNull

            assertThat(rioClient.deleteAgent(testBroker, divaAgentName, true)).isTrue
            assertThat(rioClient.headAgent(testBroker, divaAgentName)).isFalse

            val updateRequest = DivaDeviceUpdateRequest(divaEndpoint, divaUsername.uppercase(), divaPassword.uppercase())
            val updateResponse = rioClient.updateDivaDevice(divaDeviceName, updateRequest)
            assertThat(updateResponse.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(updateResponse.name).isEqualTo(divaDeviceName)
            assertThat(updateResponse.username).isEqualTo(divaUsername.uppercase())

            getResponse = rioClient.getDivaDevice(divaDeviceName)
            assertThat(getResponse.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(getResponse.name).isEqualTo(divaDeviceName)
            assertThat(getResponse.username).isEqualTo(divaUsername.uppercase())

            // update unhappy path
            listOf(
                Pair(
                    updateRequest.copy(endpoint = ""),
                    listOf(endpointMissingError)
                ),
                Pair(
                    updateRequest.copy(endpoint = "  "),
                    listOf(endpointMissingError)
                ),
                Pair(
                    updateRequest.copy(endpoint = "bad endpoint"),
                    listOf(endpointUriError.copy(value = "bad endpoint"))
                ),
                Pair(
                    updateRequest.copy(endpoint = "badscheme://bad auth"),
                    listOf(endpointUriError.copy(value = "badscheme://bad auth"))
                )
            ).forEach { (request, expected) ->
                assertDivaDeviceUpdateError(divaDeviceName, request, expected)
            }

            // TODO agent unhappy path testing

            rioClient.deleteDivaDevice(divaDeviceName)
            assertThat(rioClient.headDivaDevice(divaDeviceName)).isFalse
            assertThat(rioClient.headDevice("diva", divaDeviceName)).isFalse

            listResponse = rioClient.listDivaDevices()
            assertThat(listResponse.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(listResponse.page.totalItems).isEqualTo(totalDivaDevices)

            // create unhappy path
            listOf(
                Pair(
                    createRequest.copy(name = ""),
                    listOf(nameMissingError)
                ),
                Pair(
                    createRequest.copy(name = "  "),
                    listOf(nameInvalidError.copy(value = "  "))
                ),
                Pair(
                    createRequest.copy(name = "bad name"),
                    listOf(nameInvalidError.copy(value = "bad name"))
                ),
                Pair(
                    createRequest.copy(name = "Bad&Name"),
                    listOf(nameInvalidError.copy(value = "Bad&Name"))
                ),
                Pair(
                    createRequest.copy(endpoint = ""),
                    listOf(endpointMissingError)
                ),
                Pair(
                    createRequest.copy(endpoint = "  "),
                    listOf(endpointMissingError)
                ),
                Pair(
                    createRequest.copy(name = "", endpoint = ""),
                    listOf(nameMissingError)
                )
            ).forEach { (request, expected) ->
                assertDivaDeviceCreateError(request, expected)
            }

            // update non-existing device
            val ex = catchThrowableOfType(
                {
                    runBlocking {
                        rioClient.updateDivaDevice("bad-name", updateRequest)
                    }
                },
                RioHttpException::class.java
            )
            assertRioResourceError(ex, RioResourceErrorMessage(deviceResourceErrorFmt.format("bad-name"), 404, "bad-name", "DEVICE"))
        } finally {
            if (rioClient.headAgent(testBroker, divaAgentName)) {
                rioClient.deleteAgent(testBroker, divaAgentName, true)
            }
            if (rioClient.headDivaDevice(divaDeviceName)) {
                rioClient.deleteDivaDevice(divaDeviceName)
            }
        }
    }

    private fun assertDivaDeviceCreateError(request: DivaDeviceCreateRequest, expected: List<RioValidationMessage>) {
        val ex = catchThrowableOfType(
            {
                runBlocking {
                    rioClient.createDivaDevice(request)
                }
            },
            RioHttpException::class.java
        )
        assertRioValidationError(ex, expected)
    }

    private fun assertDivaDeviceUpdateError(name: String, request: DivaDeviceUpdateRequest, expected: List<RioValidationMessage>) {
        val ex = catchThrowableOfType(
            {
                runBlocking {
                    rioClient.updateDivaDevice(name, request)
                }
            },
            RioHttpException::class.java
        )
        assertRioValidationError(ex, expected)
    }

    @Test
    fun endPointTest(@TempDir uriDir: Path) = blockingTest {

        val ftpName = "ftp-${uuid()}"
        val ftpRequest = FtpEndpointDeviceCreateRequest(ftpName, "ftp://ftp.test.com", "user", "pass")
        val ftpResponse = rioClient.createFtpEndpointDevice(ftpRequest)
        assertThat(ftpResponse.statusCode).isEqualTo(HttpStatusCode.Created)
        assertThat(ftpResponse.name).isEqualTo(ftpName)
        assertThat(ftpResponse.endpoint).isEqualTo(ftpRequest.endpoint)
        assertThat(ftpResponse.username).isEqualTo(ftpRequest.username)
        assertThat(ftpResponse.type).isEqualTo(ftpRequest.type)

        val getFtp = rioClient.getFtpEndpointDevice(ftpName)
        assertThat(getFtp.statusCode).isEqualTo(HttpStatusCode.OK)
        assertThat(getFtp).isEqualTo(ftpResponse)

        val genericFtp = rioClient.getEndpointDevice(ftpName)
        assertThat(genericFtp.statusCode).isEqualTo(HttpStatusCode.OK)
        assertThat(genericFtp.name).isEqualTo(ftpName)
        assertThat(genericFtp.type).isEqualTo(ftpRequest.type)

        val uriName = "uri-${uuid()}"
        val uriRequest = UriEndpointDeviceCreateRequest(uriName, uriDir.toUri().toString())
        val uriResponse = rioClient.createUriEndpointDevice(uriRequest)
        assertThat(uriResponse.statusCode).isEqualTo(HttpStatusCode.Created)
        assertThat(uriResponse.name).isEqualTo(uriName)
        assertThat(uriResponse.endpoint).isEqualTo(uriRequest.endpoint)
        assertThat(uriResponse.type).isEqualTo(uriRequest.type)

        val getUri = rioClient.getUriEndpointDevice(uriName)
        assertThat(getUri.statusCode).isEqualTo(HttpStatusCode.OK)
        assertThat(getUri).isEqualTo(uriResponse)

        val genericUri = rioClient.getEndpointDevice(uriName)
        assertThat(genericFtp.statusCode).isEqualTo(HttpStatusCode.OK)
        assertThat(genericUri.name).isEqualTo(uriName)
        assertThat(genericUri.type).isEqualTo(uriRequest.type)

        var endpointList = rioClient.listEndpointDevices()
        assertThat(endpointList.statusCode).isEqualTo(HttpStatusCode.OK)
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

            val createBroker = rioClient.createBroker(createRequest)
            assertThat(createBroker.statusCode).isEqualTo(HttpStatusCode.Created)
            assertThat(rioClient.headBroker(testBroker)).isTrue

            val getBroker = rioClient.getBroker(testBroker)
            assertThat(getBroker.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(getBroker.name).isEqualTo(testBroker)

            val listBrokers = rioClient.listBrokers()
            assertThat(listBrokers.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(listBrokers.objects).isNotEmpty
            assertThat(listBrokers.objects.map { it.name }).contains(testBroker)

            val getWriteAgent = rioClient.getAgent(testBroker, testAgent)
            assertThat(getWriteAgent.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(getWriteAgent.name).isEqualTo(testAgent)
            assertThat(getWriteAgent.type).isEqualTo("bp_agent")
            assertThat(getWriteAgent.writable).isEqualTo(true)
            assertThat(getWriteAgent.agentConfig).isEqualTo(agentConfig.toConfigMap())

            val listAgents = rioClient.listAgents(testBroker)
            assertThat(listAgents.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(listAgents.objects).hasSize(1)
            assertThat(listAgents.objects.first().name).isEqualTo(getWriteAgent.name)

            val readAgentName = "test-read-agent"
            assertThat(rioClient.headAgent(testBroker, readAgentName)).isFalse

            val agentCreateRequest = AgentCreateRequest(readAgentName, "bp_agent", agentConfig.toConfigMap())
            val createAgent = rioClient.createAgent(testBroker, agentCreateRequest)
            assertThat(createAgent.statusCode).isEqualTo(HttpStatusCode.Created)
            assertThat(createAgent.name).isEqualTo(readAgentName)
            assertThat(createAgent.type).isEqualTo("bp_agent")
            assertThat(createAgent.writable).isEqualTo(false)
            assertThat(createAgent.agentConfig).isEqualTo(agentConfig.toConfigMap())

            var getReadAgent = rioClient.getAgent(testBroker, readAgentName)
            assertThat(getReadAgent.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(getReadAgent).isEqualTo(createAgent)
            assertThat(getReadAgent.lastIndexDate).isNull()

            var i = 20
            while (getReadAgent.indexState != "COMPLETE" && --i > 0) {
                delay(1000)
                getReadAgent = rioClient.getAgent(testBroker, readAgentName, true)
            }
            assertThat(getReadAgent.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(getReadAgent.indexState).isEqualTo("COMPLETE")
            assertThat(getReadAgent.lastIndexDate).isNotNull

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
            assertThat(archiveJob.statusCode).isEqualTo(HttpStatusCode.Created)
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
            assertThat(archiveJobStatus.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(archiveJobStatus.status.status).isEqualTo("COMPLETED")
            assertThat(archiveJobStatus.filesTransferred).isEqualTo(2)
            assertThat(archiveJobStatus.progress).isEqualTo(1.0f)

            val archiveFilesStatus = rioClient.fileStatus(archiveJob.id)
            assertThat(archiveFilesStatus.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(archiveFilesStatus.fileStatus).hasSize(6)

            val archiveFileStatus = rioClient.fileStatus(archiveJob.id, archiveRequest.files[0].name)
            assertThat(archiveFileStatus.statusCode).isEqualTo(HttpStatusCode.OK)
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
            assertThat(restoreJob.statusCode).isEqualTo(HttpStatusCode.Created)
            assertThat(restoreJob.name).isEqualTo(restoreJobName)
            assertThat(restoreJob.numberOfFiles).isEqualTo(2)

            assertThat(rioClient.headJob(restoreJob.id.toString())).isTrue

            i = 25
            var restoreJobStatus = rioClient.jobStatus(restoreJob.id)
            while (restoreJobStatus.status.status == "ACTIVE" && --i > 0) {
                delay(100)
                restoreJobStatus = rioClient.jobStatus(restoreJob.id)
            }
            assertThat(restoreJobStatus.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(restoreJobStatus.status.status).isEqualTo("COMPLETED")
            assertThat(restoreJobStatus.filesTransferred).isEqualTo(2)
            assertThat(restoreJobStatus.progress).isEqualTo(1.0f)

            val restoreFilesStatus = rioClient.fileStatus(restoreJob.id)
            assertThat(restoreFilesStatus.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(restoreFilesStatus.fileStatus).hasSize(6)

            val restoreFileStatus = rioClient.fileStatus(restoreJob.id, restoreRequest.files[1].name)
            assertThat(restoreFileStatus.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(restoreFileStatus.fileStatus).hasSize(3)

            var jobList = rioClient.listJobs(broker = testBroker, jobStatus = "COMPLETED")
            assertThat(jobList.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(jobList.jobs).isNotEmpty
            assertThat(jobList.jobs.map { it.id }).contains(archiveJob.id)
            assertThat(jobList.jobs.map { it.id }).contains(restoreJob.id)

            val totalJobs = jobList.page.totalItems
            rioClient.deleteJob(archiveJob.id)
            rioClient.deleteJob(restoreJob.id)

            assertThat(rioClient.headJob(archiveJob.id.toString())).isFalse
            assertThat(rioClient.headJob(restoreJob.id.toString())).isFalse

            jobList = rioClient.listJobs(broker = testBroker, jobStatus = "COMPLETED")
            assertThat(jobList.statusCode).isEqualTo(HttpStatusCode.OK)
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
            assertThat(archiveJob.statusCode).isEqualTo(HttpStatusCode.Created)

            var i = 25
            var archiveJobStatus = rioClient.jobStatus(archiveJob.id)
            assertThat(archiveJobStatus.statusCode).isEqualTo(HttpStatusCode.OK)
            while (archiveJobStatus.status.status == "ACTIVE" && --i > 0) {
                delay(100)
                archiveJobStatus = rioClient.jobStatus(archiveJob.id)
            }
            assertThat(archiveJobStatus.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(archiveJobStatus.status.status).isEqualTo("COMPLETED")

            listObjects = rioClient.listObjects(testBroker)
            assertThat(listObjects.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(listObjects.page.totalItems).isEqualTo(totalObjects + 1)

            val getObject = rioClient.getObject(testBroker, objectName)
            assertThat(getObject.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(getObject.broker).isEqualTo(testBroker)
            assertThat(getObject.name).isEqualTo(objectName)
            assertThat(getObject.size).isEqualTo(archiveRequest.files[0].size)
            assertThat(getObject.metadata).isEqualTo(metadata)

            val newMetadata = mapOf(Pair("key9", "val9"))
            val updateObject = rioClient.updateObject(testBroker, objectName, newMetadata)
            assertThat(updateObject.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(updateObject).isEqualTo(getObject.copy(metadata = newMetadata))

            assertThat(rioClient.objectExists(testBroker, objectName)).isTrue

            rioClient.deleteObject(testBroker, objectName)
            assertThat(rioClient.objectExists(testBroker, objectName)).isFalse

            listObjects = rioClient.listObjects(testBroker)
            assertThat(listObjects.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(listObjects.page.totalItems).isEqualTo(totalObjects)

            // TODO: object unhappy path testing
        } finally {
            removeBroker()
        }
    }

    @Test
    fun objectMetadataTest() = blockingTest {
        val uuid = uuid()
        val fileList: List<String> = (1..10).map { "metaObject-$uuid-$it" }

        val metadata1 = mapOf(Pair("m1", "v1"), Pair("m2", "v2"), Pair("m3", "v3"))
        val metadata2 = mapOf(Pair("m1-a", "v1-a"), Pair("m2-b", "v2-b"), Pair("m3-c", "v3-c"))
        val internalMetadata1 = mapOf(Pair("i1", "y1"), Pair("i2", "y2"))
        val internalMetadata2 = mapOf(Pair("i1-x", "y1-x"), Pair("i2-z", "y2-z"))

        val metadata1UpdateRequest: List<ObjectUpdateRequest> = fileList.map {
            ObjectUpdateRequest(it, metadata1)
        }
        val metadata2UpdateRequest: List<ObjectUpdateRequest> = fileList.map {
            ObjectUpdateRequest(it, metadata2)
        }
        val internalMetadata1UpdateRequest: List<ObjectUpdateRequest> = fileList.map {
            ObjectUpdateRequest(it, internalMetadata1)
        }
        val internalMetadata2UpdateRequest: List<ObjectUpdateRequest> = fileList.map {
            ObjectUpdateRequest(it, internalMetadata2)
        }

        val objectBroker = "object-broker"

        try {
            if (!rioClient.headBroker(objectBroker)) {
                val agentConfig = BpAgentConfig(brokerObjectBucket, spectraDeviceCreateRequest.name, spectraDeviceCreateRequest.username)
                val createRequest = BrokerCreateRequest(objectBroker, "agent-name", agentConfig)
                val createResponse = rioClient.createBroker(createRequest)
                assertThat(createResponse.statusCode).isEqualTo(HttpStatusCode.Created)
            }

            val filesToArchive: List<FileToArchive> = fileList.map {
                FileToArchive(it, URI("aToZSequence://$it"), 1024L)
            }
            val archiveRequest = ArchiveRequest("archive-meta-${uuid()}", filesToArchive)
            val archiveJob = rioClient.createArchiveJob(objectBroker, archiveRequest)
            assertThat(archiveJob.statusCode).isEqualTo(HttpStatusCode.Created)
            var i = 25
            var archiveJobStatus = rioClient.jobStatus(archiveJob.id)
            while (archiveJobStatus.status.status == "ACTIVE" && --i > 0) {
                delay(1000)
                archiveJobStatus = rioClient.jobStatus(archiveJob.id)
            }
            assertThat(archiveJobStatus.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(archiveJobStatus.status.status).isEqualTo("COMPLETED")

            val metadata1Request = ObjectBatchUpdateRequest(metadata1UpdateRequest)
            val metadata1Response = rioClient.updateObjects(objectBroker, metadata1Request)
            assertThat(metadata1Response.statusCode).isEqualTo(HttpStatusCode.OK)

            fileList.forEach { fileName ->
                val getObject = rioClient.getObject(objectBroker, fileName, includeInternalMetadata = true)
                assertThat(getObject.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(getObject.metadata).isEqualTo(metadata1)
                assertThat(getObject.internalMetadata).isNullOrEmpty()
            }

            val internalMetadata1Request = ObjectBatchUpdateRequest(internalMetadata1UpdateRequest)
            val internalMetadata1Response = rioClient.updateObjects(objectBroker, internalMetadata1Request, internalData = true)
            assertThat(internalMetadata1Response.statusCode).isEqualTo(HttpStatusCode.OK)

            fileList.forEach { fileName ->
                val getObject = rioClient.getObject(objectBroker, fileName, includeInternalMetadata = true)
                assertThat(getObject.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(getObject.metadata).isEqualTo(metadata1)
                assertThat(getObject.internalMetadata).isEqualTo(internalMetadata1)
            }

            val metadata2Request = ObjectBatchUpdateRequest(metadata2UpdateRequest)
            val metadata2Response = rioClient.updateObjects(objectBroker, metadata2Request, merge = true)
            assertThat(metadata2Response.statusCode).isEqualTo(HttpStatusCode.OK)

            val combinedMetadata = metadata1.plus(metadata2)
            fileList.forEach { fileName ->
                val getObject = rioClient.getObject(objectBroker, fileName, includeInternalMetadata = true)
                assertThat(getObject.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(getObject.metadata).isEqualTo(combinedMetadata)
                assertThat(getObject.internalMetadata).isEqualTo(internalMetadata1)
            }

            val internalMetadata2Request = ObjectBatchUpdateRequest(internalMetadata2UpdateRequest)
            val internalMetadata2Response = rioClient.updateObjects(objectBroker, internalMetadata2Request, internalData = true, merge = true)
            assertThat(internalMetadata2Response.statusCode).isEqualTo(HttpStatusCode.OK)

            val combinedInternalMetadata = internalMetadata1.plus(internalMetadata2)
            fileList.forEach { fileName ->
                val getObject = rioClient.getObject(objectBroker, fileName, includeInternalMetadata = true)
                assertThat(getObject.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(getObject.metadata).isEqualTo(combinedMetadata)
                assertThat(getObject.internalMetadata).isEqualTo(combinedInternalMetadata)
            }

            val metadataResponse = rioClient.updateObjects(objectBroker, metadata1Request)
            assertThat(metadataResponse.statusCode).isEqualTo(HttpStatusCode.OK)

            fileList.forEach { fileName ->
                val getObject = rioClient.getObject(objectBroker, fileName, includeInternalMetadata = true)
                assertThat(getObject.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(getObject.metadata).isEqualTo(metadata1)
                assertThat(getObject.internalMetadata).isEqualTo(combinedInternalMetadata)
            }

            val internalMetadataResponse = rioClient.updateObjects(objectBroker, internalMetadata1Request, internalData = true)
            assertThat(internalMetadataResponse.statusCode).isEqualTo(HttpStatusCode.OK)

            fileList.forEach { fileName ->
                val getObject = rioClient.getObject(objectBroker, fileName, includeInternalMetadata = true)
                assertThat(getObject.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(getObject.metadata).isEqualTo(metadata1)
                assertThat(getObject.internalMetadata).isEqualTo(internalMetadata1)
            }
        } finally {
            // TODO: remove bucket or objects from bucket
            if (rioClient.headBroker(objectBroker)) {
                rioClient.deleteBroker(objectBroker, true)
            }
        }
    }

    @Test
    fun logTest() = blockingTest {

        var listLogs = rioClient.listLogsets()
        val totalLogs = listLogs.page.totalItems

        val newLog = rioClient.createLogset()
        assertThat(newLog.statusCode).isEqualTo(HttpStatusCode.Accepted)

        var i = 25
        var getLog = rioClient.getLogset(newLog.id)
        while (getLog.status != "COMPLETE" && --i > 0) {
            delay(1000)
            getLog = rioClient.getLogset(newLog.id)
        }
        assertThat(getLog.statusCode).isEqualTo(HttpStatusCode.OK)
        assertThat(getLog.status).isEqualTo("COMPLETE")

        assertThat(rioClient.headLogset(UUID.fromString(newLog.id))).isTrue

        listLogs = rioClient.listLogsets()
        assertThat(listLogs.statusCode).isEqualTo(HttpStatusCode.OK)
        assertThat(listLogs.page.totalItems).isEqualTo(totalLogs + 1)
        assertThat(listLogs.objects.map { it.id }).contains(newLog.id)

        rioClient.deleteLogset(UUID.fromString(newLog.id))
        assertThat(rioClient.headLogset(UUID.fromString(newLog.id))).isFalse

        listLogs = rioClient.listLogsets()
        assertThat(listLogs.statusCode).isEqualTo(HttpStatusCode.OK)
        assertThat(listLogs.page.totalItems).isEqualTo(totalLogs)

        // TODO: log unhappy path testing
    }

    @Test
    fun systemTest() = blockingTest {
        val systemResponse = rioClient.systemInfo()
        assertThat(systemResponse.statusCode).isEqualTo(HttpStatusCode.OK)
    }

    @Test
    fun keysTest() = blockingTest {
        var listTokens = rioClient.listTokenKeys()
        val totalTokens = listTokens.page.totalItems

        val createToken = rioClient.createApiToken(TokenCreateRequest())
        assertThat(createToken.statusCode).isEqualTo(HttpStatusCode.Created)
        assertThat(createToken.userName).isEqualTo(username)
        assertThat(createToken.creationDate).isNotNull
        assertThat(createToken.id).isNotNull
        assertThat(createToken.expirationDate).isNull()

        val expireZdt = ZonedDateTime.now().plusDays(2)
        val longToken = rioClient.createApiToken(TokenCreateRequest(expireZdt.toString()))
        assertThat(longToken.statusCode).isEqualTo(HttpStatusCode.Created)
        assertThat(longToken.userName).isEqualTo(username)
        assertThat(longToken.expirationDate).isNotNull
        val expirationDate = ZonedDateTime.parse(longToken.expirationDate)
        assertThat(expirationDate).isBefore(expireZdt.plusMinutes(1))
        assertThat(expirationDate).isAfter(expireZdt.plusMinutes(-1))

        val getToken = rioClient.getApiToken(createToken.id)
        assertThat(getToken.statusCode).isEqualTo(HttpStatusCode.OK)
        assertThat(getToken.id).isEqualTo(createToken.id)
        assertThat(getToken.userName).isEqualTo(createToken.userName)
        assertThat(getToken.creationDate).isEqualTo(createToken.creationDate)
        assertThat(getToken.expirationDate).isEqualTo(createToken.expirationDate)

        listTokens = rioClient.listTokenKeys()
        assertThat(listTokens.statusCode).isEqualTo(HttpStatusCode.OK)
        assertThat(listTokens.page.totalItems).isEqualTo(totalTokens + 2)
        assertThat(listTokens.objects.map { it.id }).containsAll(listOf(createToken.id, longToken.id))

        assertThat(rioClient.headApiToken(createToken.id)).isTrue
        rioClient.deleteApiToken(createToken.id)
        assertThat(rioClient.headApiToken(createToken.id)).isFalse

        assertThat(rioClient.headApiToken(longToken.id)).isTrue
        rioClient.deleteApiToken(longToken.id)
        assertThat(rioClient.headApiToken(longToken.id)).isFalse

        listTokens = rioClient.listTokenKeys()
        assertThat(listTokens.statusCode).isEqualTo(HttpStatusCode.OK)
        assertThat(listTokens.page.totalItems).isEqualTo(totalTokens)

        // TODO: keys unhappy path testing
    }

    private suspend fun ensureBrokerExists() {
        if (!rioClient.headBroker(testBroker)) {
            val agentConfig = BpAgentConfig(brokerBucket, spectraDeviceCreateRequest.name, spectraDeviceCreateRequest.username)
            val createRequest = BrokerCreateRequest(testBroker, testAgent, agentConfig)
            val createResponse = rioClient.createBroker(createRequest)
            assertThat(createResponse.statusCode).isEqualTo(HttpStatusCode.Created)
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

    private fun assertRioResourceError(ex: RioHttpException, expected: RioResourceErrorMessage) {
        assertThat(ex).isNotNull
        assertThat(ex.errorMessage()).isNotNull.isInstanceOf(RioResourceErrorMessage::class.java)

        val rioResourceErrorMessage = ex.errorMessage() as RioResourceErrorMessage
        assertThat(rioResourceErrorMessage.message).isEqualTo(expected.message)
        assertThat(rioResourceErrorMessage.statusCode).isEqualTo(expected.statusCode)
        assertThat(rioResourceErrorMessage.resourceName).isEqualTo(expected.resourceName)
        assertThat(rioResourceErrorMessage.resourceType).isEqualTo(expected.resourceType)
    }

    private fun assertRioValidationError(ex: RioHttpException, expected: List<RioValidationMessage>) {
        assertThat(ex).isNotNull
        assertThat(ex.errorMessage()).isNotNull.isInstanceOf(RioValidationErrorMessage::class.java)

        val rioValidationErrorMessage = ex.errorMessage() as RioValidationErrorMessage
        assertThat(rioValidationErrorMessage.message).isEqualTo("Validation Failed")
        assertThat(rioValidationErrorMessage.statusCode).isEqualTo(422)
        assertThat(rioValidationErrorMessage.errors)
            .hasSize(expected.size)
            .containsExactlyInAnyOrderElementsOf(expected)
    }
}

private fun getenvValue(key: String, default: String): String =
    System.getenv(key) ?: default
