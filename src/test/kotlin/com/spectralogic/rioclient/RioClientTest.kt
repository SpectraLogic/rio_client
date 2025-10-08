/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import io.ktor.client.request.request
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.time.Instant
import java.time.ZonedDateTime
import java.util.UUID

@Tag("RioClientTest")
class RioClientTest {
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

        private lateinit var ldapHost: String
        private lateinit var ldapDomain: String

        private lateinit var username: String
        private lateinit var password: String

        private const val DEVICE_RESOURCE_ERROR_FMT = "Resource of type DEVICE and name %s does not exist"
        private const val INVALID_NAME_MSG_FMT = "names can only contain the characters: [a-z], [0-9], '-' and '_'"
        private const val URI_PATH_FORMAT_ERROR_FMT = "Illegal character in path at index %s: %s"
        private const val URI_AUTH_FORMAT_ERROR_FMT = "Illegal character in authority at index %s: %s"
        private const val EMPTY_ERROR = "cannot be empty or consist only of whitespace"

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            username = getenvValue("ESCAPEPOD_LOGIN", "spectra")
            password = getenvValue("ESCAPEPOD_PASSWORD", "spectra")
            rioClient = RioClient(URL(getenvValue("ESCAPEPOD_URL", "https://localhost:5050")), username, password)

            spectraDeviceName = getenvValue("BP_DEVICE_NAME", "rioclient_bp")
            spectraDeviceMgmtInterfaceUrl = getenvValue("MGMT_INTERFACE_URL", "https://sm25-2-mgmt.eng.sldomain.com")
            spectraDeviceUsername = getenvValue("BP_DEVICE_USERNAME", "Administrator")
            spectraDevicePassword = getenvValue("BP_DEVICE_PASSWORD", "spectra")
            spectraDeviceAltUsername = getenvValue("BP_DEVICE_ALT_USERNAME", "monitor")
            spectraDeviceAltPassword = getenvValue("BP_DEVICE_ALT_PASSWORD", "monitor")
            spectraDeviceCreateRequest =
                SpectraDeviceCreateRequest(
                    spectraDeviceName,
                    spectraDeviceMgmtInterfaceUrl,
                    spectraDeviceUsername,
                    spectraDevicePassword,
                )

            testBroker = getenvValue("DEFAULT_BROKER", "rioclient-broker")
            testAgent = getenvValue("DEFAULT_AGENT", "rioclient-agent")
            brokerBucket = getenvValue("DEFAULT_BUCKET", "rioclient-testbucket")
            brokerObjectBucket = getenvValue("DEFAULT_OBJECT_BUCKET", "rioclient-objectbucket")

            divaEndpoint = getenvValue("DIVA_ENDPOINT", "http://10.85.45.164:9763/services/DIVArchiveWS_SOAP_2.1?wsdl")
            divaUsername = getenvValue("DIVA_USERNAME", "user")
            divaPassword = getenvValue("DIVA_PASSWORD", "pass")
            divaCategory = getenvValue("DIVA_CATEGORY", "DVT-10")

            ldapHost = getenvValue("LDAP_HOST", "6285bou-dc01.sldomain.com")
            ldapDomain = getenvValue("LDAP_DOMAIN", "sldomain.com")
        }
    }

    @BeforeEach
    fun beforeEach() =
        blockingTest {
            rioClient.ensureSpectraDeviceExists(spectraDeviceCreateRequest)
        }

    @Test
    fun spectraDeviceTest() =
        blockingTest {
            val nameBaseError = RioValidationMessage("name", "string", "")
            val nameBlankError = nameBaseError.copy(fieldType = "DEVICE", errorType = "invalid_device_name", reason = EMPTY_ERROR)
            val nameInvalidError =
                nameBaseError.copy(
                    fieldType = "DEVICE",
                    errorType = "invalid_device_name",
                    reason = INVALID_NAME_MSG_FMT,
                )
            val mgmtBaseError = RioValidationMessage("mgmtInterface", "URI", "")
            val mgmtHostError = mgmtBaseError.copy(errorType = "unknown_host")
            val mgmtUriError = mgmtBaseError.copy(errorType = "invalid_uri", reason = "Invalid Format")
            val mgmtUsernameError = mgmtBaseError.copy("username", "string", errorType = "invalid_string_value")
            val mgmtPasswordError = mgmtBaseError.copy("password", "password", errorType = "invalid_credentials")
            val credsUserError = RioValidationMessage("username", "string", "invalid_string_value", "", EMPTY_ERROR)
            val credsPassError = RioValidationMessage("password", "password", errorType = "invalid_credentials")

            var testNum = 0
            val testFmt = "spectraDeviceTest-%d"

            val spectraDeviceName = "bp-${uuid()}"
            val createRequest = spectraDeviceCreateRequest.copy(name = spectraDeviceName)
            val createResponse = rioClient.createSpectraDevice(createRequest)
            assertThat(createResponse.statusCode).describedAs(testFmt.format(++testNum)).isEqualTo(HttpStatusCode.Created)
            assertThat(createResponse.name).describedAs(testFmt.format(++testNum)).isEqualTo(spectraDeviceName)

            var listResponse = rioClient.listSpectraDevices()
            assertThat(listResponse.statusCode).describedAs(testFmt.format(++testNum)).isEqualTo(HttpStatusCode.OK)
            val spectraDeviceTotal = listResponse.page.totalItems
            assertThat(listResponse.devices.map { it.name }).describedAs(testFmt.format(++testNum)).contains(spectraDeviceName)
            assertThat(spectraDeviceTotal).describedAs(testFmt.format(++testNum)).isGreaterThanOrEqualTo(2)

            assertRioListResponse("spectra devices", listResponse.page.totalItems) { page, perPage ->
                rioClient.listSpectraDevices(page, perPage)
            }

            assertThat(rioClient.headSpectraDevice(spectraDeviceName)).describedAs(testFmt.format(++testNum)).isTrue
            assertThat(rioClient.headDevice("spectra", spectraDeviceName)).describedAs(testFmt.format(++testNum)).isTrue

            var getResponse = rioClient.getSpectraDevice(spectraDeviceName)
            assertThat(getResponse.statusCode).describedAs(testFmt.format(++testNum)).isEqualTo(HttpStatusCode.OK)
            assertThat(getResponse.name).describedAs(testFmt.format(++testNum)).isEqualTo(createRequest.name)
            assertThat(getResponse.mgmtInterface).describedAs(testFmt.format(++testNum)).isEqualTo(createRequest.mgmtInterface)
            assertThat(getResponse.username).describedAs(testFmt.format(++testNum)).isEqualTo(createRequest.username)

            val updateRequest =
                SpectraDeviceUpdateRequest(
                    createRequest.mgmtInterface,
                    spectraDeviceAltUsername,
                    spectraDeviceAltPassword,
                    createRequest.dataPath,
                )
            val updateResponse = rioClient.updateSpectraDevice(spectraDeviceName, updateRequest)
            assertThat(updateResponse.statusCode).describedAs(testFmt.format(++testNum)).isEqualTo(HttpStatusCode.OK)
            assertThat(updateResponse.name).describedAs(testFmt.format(++testNum)).isEqualTo(spectraDeviceName)
            assertThat(updateResponse.username).describedAs(testFmt.format(++testNum)).isEqualTo(spectraDeviceAltUsername)

            getResponse = rioClient.getSpectraDevice(spectraDeviceName)
            assertThat(getResponse.statusCode).describedAs(testFmt.format(++testNum)).isEqualTo(HttpStatusCode.OK)
            assertThat(getResponse.name).describedAs(testFmt.format(++testNum)).isEqualTo(spectraDeviceName)
            assertThat(getResponse.mgmtInterface).describedAs(testFmt.format(++testNum)).isEqualTo(createRequest.mgmtInterface)
            assertThat(getResponse.username).describedAs(testFmt.format(++testNum)).isEqualTo(spectraDeviceAltUsername)

            // update unhappy path
            testNum = 100
            listOf(
                Pair(
                    updateRequest.copy(mgmtInterface = "bad uri"),
                    listOf(mgmtUriError.copy(value = "bad uri")),
                ),
                Pair(
                    updateRequest.copy(mgmtInterface = "badscheme://bad value"),
                    listOf(
                        mgmtUriError.copy(
                            value = "badscheme",
                            errorType = "invalid_scheme",
                            reason = "URI scheme must be HTTPS",
                        ),
                    ),
                ),
                Pair(
                    updateRequest.copy(mgmtInterface = "https://badhost.eng.sldomain.com"),
                    listOf(mgmtHostError.copy(value = "https://badhost.eng.sldomain.com")),
                ),
                Pair(
                    updateRequest.copy(username = ""),
                    listOf(
                        credsUserError,
                        // credsPassError,
                    ),
                ),
                /*Pair(
                    updateRequest.copy(username = "bad-username"),
                    listOf(
                        credsUserError,
                        credsPassError,
                    ),
                ),
                Pair(
                    updateRequest.copy(username = "bad-username", password = "bad-password"),
                    listOf(
                        credsUserError,
                        credsPassError,
                    ),
                ),
                Pair(
                    updateRequest.copy(password = "bad-password"),
                    listOf(
                        credsUserError,
                        credsPassError,
                    ),
                ),*/
            ).forEach { (request, expected) ->
                assertSpectraDeviceUpdateError(spectraDeviceName, request, expected, ++testNum)
            }

            val deleteResponse = rioClient.deleteDevice("spectra", spectraDeviceName)
            assertThat(deleteResponse.statusCode).describedAs(testFmt.format(++testNum)).isEqualTo(HttpStatusCode.NoContent)
            assertThat(rioClient.headSpectraDevice(spectraDeviceName)).describedAs(testFmt.format(++testNum)).isFalse
            assertThat(rioClient.headDevice("spectra", spectraDeviceName)).describedAs(testFmt.format(++testNum)).isFalse

            listResponse = rioClient.listSpectraDevices()
            assertThat(listResponse.statusCode).describedAs(testFmt.format(++testNum)).isEqualTo(HttpStatusCode.OK)
            assertThat(listResponse.devices.map { it.name }).describedAs(testFmt.format(++testNum)).doesNotContain(spectraDeviceName)
            assertThat(rioClient.headSpectraDevice(spectraDeviceName)).describedAs(testFmt.format(++testNum)).isFalse

            // create unhappy path
            testNum = 200
            listOf(
                Pair(
                    createRequest.copy(name = ""),
                    listOf(nameBlankError.copy(value = "")),
                ),
                Pair(
                    createRequest.copy(name = "  "),
                    listOf(nameBlankError.copy(value = "  ")),
                ),
                Pair(
                    createRequest.copy(name = "bad name"),
                    listOf(nameInvalidError.copy(value = "bad name")),
                ),
                Pair(
                    createRequest.copy(name = "Bad&Name"),
                    listOf(nameInvalidError.copy(value = "Bad&Name")),
                ),
                Pair(
                    createRequest.copy(mgmtInterface = "bad uri"),
                    listOf(mgmtUriError.copy(value = "bad uri")), // reason = URI_PATH_FORMAT_ERROR_FMT.format("3", "bad uri"))),
                ),
                Pair(
                    createRequest.copy(mgmtInterface = "badscheme://bad value"),
                    listOf(
                        mgmtUriError.copy(
                            value = "badscheme",
                            errorType = "invalid_scheme",
                            reason = "URI scheme must be HTTPS",
                        ),
                    ),
                ),
                Pair(
                    createRequest.copy(mgmtInterface = "https://badhost.eng.sldomain.com"),
                    listOf(mgmtHostError.copy(value = "https://badhost.eng.sldomain.com")),
                ),
                Pair(
                    createRequest.copy(username = ""),
                    listOf(mgmtUsernameError.copy(value = "", reason = EMPTY_ERROR)),
                ),
                /*Pair(
                    createRequest.copy(username = "bad-username"),
                    listOf(mgmtUsernameError),
                ),
                Pair(
                    createRequest.copy(username = "bad-username", password = "bad-password"),
                    listOf(mgmtUsernameError, mgmtPasswordError),
                ),
                Pair(
                    createRequest.copy(password = "bad-password"),
                    listOf(mgmtUsernameError, mgmtPasswordError),
                ),
                Pair(
                    createRequest.copy(name = "bad name", mgmtInterface = "bad uri"),
                    listOf(mgmtUriError.copy(value = "bad uri")) //, reason = URI_PATH_FORMAT_ERROR_FMT.format("3", "bad uri"))),
                ),
                Pair(
                    createRequest.copy(name = "bad name", mgmtInterface = "https://badhost.eng.sldomain.com"),
                    listOf(nameInvalidError.copy(value = "bad name")),
                ),*/
            ).forEach { (request, expectedErrors) ->
                assertSpectraDeviceCreateError(request, expectedErrors, ++testNum)
            }

            // update non-existing device
            val ex =
                catchThrowableOfType(
                    {
                        runBlocking {
                            rioClient.updateSpectraDevice("bad-name", updateRequest)
                        }
                    },
                    RioHttpException::class.java,
                )
            assertRioResourceError(ex, RioResourceErrorMessage(DEVICE_RESOURCE_ERROR_FMT.format("bad-name"), 404, "bad-name", "DEVICE"))
        }

    private fun assertSpectraDeviceCreateError(
        request: SpectraDeviceCreateRequest,
        expected: List<RioValidationMessage>,
        testNum: Int,
    ) {
        val ex =
            catchThrowableOfType(
                {
                    runBlocking {
                        rioClient.createSpectraDevice(request)
                    }
                },
                RioHttpException::class.java,
            )
        assertRioValidationError(ex, expected, testNum)
    }

    private fun assertSpectraDeviceUpdateError(
        name: String,
        request: SpectraDeviceUpdateRequest,
        expected: List<RioValidationMessage>,
        testNum: Int,
    ) {
        val ex =
            catchThrowableOfType(
                {
                    runBlocking {
                        rioClient.updateSpectraDevice(name, request)
                    }
                },
                RioHttpException::class.java,
            )
        assertRioValidationError(ex, expected, testNum)
    }

    // TODO: s3cDeviceTest
    // TODO: vailDeviceTest
    // TODO: flashnetDeviceTest
    // TODO: tbpfrDeviceTest

    @Test
    fun divaTest() =
        blockingTest {
            val nameBaseError = RioValidationMessage("name", "string", "")
            val nameInvalidError = nameBaseError.copy(fieldType = "DEVICE", errorType = "invalid_device_name")
            val endpointBaseError = RioValidationMessage("endpoint", "URL", "invalid_url_value")
            var testNum = 0

            ensureBrokerExists()
            val divaDeviceName = "diva-device-${uuid()}"
            val divaAgentName = "diva-agent-${uuid()}"
            try {
                var listResponse = rioClient.listDivaDevices()
                assertThat(listResponse.statusCode).isEqualTo(HttpStatusCode.OK)
                val totalDivaDevices = listResponse.page.totalItems

                assertRioListResponse("diva devices", totalDivaDevices) { page, perPage ->
                    rioClient.listDivaDevices(page, perPage)
                }

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
                val createAgentRequest = AgentCreateRequest(divaAgentName, "diva_agent", divaAgentConfig.toConfigMap(), false)
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

                val deleteAgentResponse = rioClient.deleteAgent(testBroker, divaAgentName, true)
                assertThat(deleteAgentResponse.statusCode).isEqualTo(HttpStatusCode.NoContent)
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
                        listOf(endpointBaseError.copy(value = "", reason = EMPTY_ERROR)),
                    ),
                    Pair(
                        updateRequest.copy(endpoint = "  "),
                        listOf(endpointBaseError.copy(value = "  ", reason = EMPTY_ERROR)),
                    ),
                    Pair(
                        updateRequest.copy(endpoint = "bad endpoint"),
                        listOf(
                            endpointBaseError.copy(value = "bad endpoint", reason = URI_PATH_FORMAT_ERROR_FMT.format("3", "bad endpoint")),
                        ),
                    ),
                    Pair(
                        updateRequest.copy(endpoint = "badscheme://bad auth"),
                        listOf(
                            endpointBaseError.copy(
                                value = "badscheme://bad auth",
                                reason = URI_AUTH_FORMAT_ERROR_FMT.format("15", "badscheme://bad auth"),
                            ),
                        ),
                    ),
                ).forEach { (request, expected) ->
                    assertDivaDeviceUpdateError(divaDeviceName, request, expected, ++testNum)
                }

                // TODO agent unhappy path testing

                val deleteResponse = rioClient.deleteDivaDevice(divaDeviceName)
                assertThat(deleteResponse.statusCode).isEqualTo(HttpStatusCode.NoContent)
                assertThat(rioClient.headDivaDevice(divaDeviceName)).isFalse
                assertThat(rioClient.headDevice("diva", divaDeviceName)).isFalse

                listResponse = rioClient.listDivaDevices()
                assertThat(listResponse.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(listResponse.page.totalItems).isEqualTo(totalDivaDevices)

                // create unhappy path
                listOf(
                    Pair(
                        createRequest.copy(name = ""),
                        listOf(nameInvalidError.copy(value = "", reason = EMPTY_ERROR)),
                    ),
                    Pair(
                        createRequest.copy(name = "  "),
                        listOf(nameInvalidError.copy(value = "  ", reason = EMPTY_ERROR)),
                    ),
                    Pair(
                        createRequest.copy(name = "bad name"),
                        listOf(nameInvalidError.copy(value = "bad name", reason = INVALID_NAME_MSG_FMT)),
                    ),
                    Pair(
                        createRequest.copy(name = "Bad&Name"),
                        listOf(nameInvalidError.copy(value = "Bad&Name", reason = INVALID_NAME_MSG_FMT)),
                    ),
                    Pair(
                        createRequest.copy(name = "", endpoint = ""),
                        listOf(
                            nameInvalidError.copy(value = "", reason = EMPTY_ERROR),
                            endpointBaseError.copy(value = "", reason = EMPTY_ERROR),
                        ),
                    ),
                ).forEach { (request, expected) ->
                    assertDivaDeviceCreateError(request, expected, ++testNum)
                }

                // update non-existing device
                val ex =
                    catchThrowableOfType(
                        {
                            runBlocking {
                                rioClient.updateDivaDevice("bad-name", updateRequest)
                            }
                        },
                        RioHttpException::class.java,
                    )
                assertRioResourceError(ex, RioResourceErrorMessage(DEVICE_RESOURCE_ERROR_FMT.format("bad-name"), 404, "bad-name", "DEVICE"))
            } finally {
                if (rioClient.headAgent(testBroker, divaAgentName)) {
                    removeAgent(testBroker, divaAgentName)
                }
                if (rioClient.headDivaDevice(divaDeviceName)) {
                    rioClient.deleteDivaDevice(divaDeviceName)
                }
            }
        }

    private fun assertDivaDeviceCreateError(
        request: DivaDeviceCreateRequest,
        expected: List<RioValidationMessage>,
        testNum: Int,
    ) {
        val ex =
            catchThrowableOfType(
                {
                    runBlocking {
                        rioClient.createDivaDevice(request)
                    }
                },
                RioHttpException::class.java,
            )
        assertRioValidationError(ex, expected, testNum)
    }

    private fun assertDivaDeviceUpdateError(
        name: String,
        request: DivaDeviceUpdateRequest,
        expected: List<RioValidationMessage>,
        testNum: Int,
    ) {
        val ex =
            catchThrowableOfType(
                {
                    runBlocking {
                        rioClient.updateDivaDevice(name, request)
                    }
                },
                RioHttpException::class.java,
            )
        assertRioValidationError(ex, expected, testNum)
    }

    @Test
    fun nasBrokerTest(
        @TempDir tempDir: Path,
    ) = blockingTest {
        val uuid = UUID.randomUUID()
        val archiveAgentPath = tempDir.resolve("archiveAgent").also { it.toFile().mkdir() }
        val writableAgentPath = tempDir.resolve("writeAgent").also { it.toFile().mkdir() }
        val archivePath =
            archiveAgentPath
                .toFile()
                .absolutePath
                .toString()
                .split(File.separator)
                .joinToString("/")
        val writablePath =
            writableAgentPath
                .toFile()
                .absolutePath
                .toString()
                .split(File.separator)
                .joinToString("/")
        val firstFile = archiveAgentPath.resolve("first.txt").toFile()
        val secondFile = writableAgentPath.resolve("second.txt").toFile()
        firstFile.writeText("abc".repeat(10))
        secondFile.writeText("abc".repeat(10))
        val brokerName = "nas-broker-$uuid"
        val archiveAgentConfig =
            NasAgentConfig(
                URI("file:///$archivePath").toString(),
            )
        val writableAgentConfig =
            NasAgentConfig(
                URI("file:///$writablePath").toString(),
            )
        rioClient
            .createBroker(
                BrokerCreateRequest(
                    brokerName,
                    "archive-agent",
                    archiveAgentConfig.toConfigMap(),
                    "nas_agent",
                ),
            ).let { resp ->
                assertThat(resp.statusCode).isEqualTo(HttpStatusCode.Created)
                assertThat(resp.name).isEqualTo(brokerName)
            }
        rioClient.listAgents(brokerName).let { resp ->
            assertThat(resp.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(resp.agents).hasSize(1)
            resp.agents[0].let { agent ->
                assertThat(agent.name).isEqualTo("archive-agent")
                assertThat(agent.type).isEqualTo("nas_agent")
                assertThat(agent.writable).isTrue
                assertThat(agent.archiveAgent).isTrue
            }
        }
        rioClient.listObjects(brokerName).let { resp ->
            assertThat(resp.page.totalItems).isEqualTo(1)
            assertThat(resp.objects.firstOrNull()?.name).isEqualTo(firstFile.name)
        }
        rioClient
            .createAgent(
                brokerName,
                AgentCreateRequest(
                    "writable-agent",
                    "nas_agent",
                    writableAgentConfig.toConfigMap(),
                    true,
                ),
            ).let { resp ->
                assertThat(resp.statusCode).isEqualTo(HttpStatusCode.Created)
                assertThat(resp.name).isEqualTo("writable-agent")
                assertThat(resp.type).isEqualTo("nas_agent")
                assertThat(resp.writable).isTrue
                assertThat(resp.archiveAgent).isFalse
            }
        rioClient.listAgents(brokerName).let { resp ->
            assertThat(resp.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(resp.agents).hasSize(2)
            resp.agents.firstOrNull { it.name == "archive-agent" }?.let { agent ->
                assertThat(agent.name).isEqualTo("archive-agent")
                assertThat(agent.type).isEqualTo("nas_agent")
                assertThat(agent.writable).isTrue
                assertThat(agent.archiveAgent).isTrue
            }
            resp.agents.firstOrNull { it.name == "writable-agent" }?.let { agent ->
                assertThat(agent.name).isEqualTo("writable-agent")
                assertThat(agent.type).isEqualTo("nas_agent")
                assertThat(agent.writable).isTrue
                assertThat(agent.archiveAgent).isFalse
            }
        }
        rioClient.listObjects(brokerName).let { resp ->
            assertThat(resp.page.totalItems).isEqualTo(2)
        }
        firstFile.delete()
        secondFile.delete()
        archiveAgentPath.toFile().delete()
        writableAgentPath.toFile().delete()
        tempDir.toFile().delete()
    }

    @Test
    fun endPointTest(
        @TempDir uriDir: Path,
    ) = blockingTest {
        val ftpName = "ftp-${uuid()}"
        val ftpRequest = FtpEndpointDeviceCreateRequest(ftpName, "ftp://ftp.test.com", "user", "pass")

        val endpointsResponse = rioClient.listEndpointDevices()
        assertThat(endpointsResponse.statusCode).isEqualTo(HttpStatusCode.OK)

        assertRioListResponse("endpoints", endpointsResponse.page.totalItems) { page, perPage ->
            rioClient.listEndpointDevices(page, perPage)
        }

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
        assertThat(endpointList.devices.map { it.name }).contains(ftpName)
        assertThat(endpointList.devices.map { it.name }).contains(uriName)
        assertThat(endpointCount).isGreaterThanOrEqualTo(2)

        assertThat(rioClient.headEndpointDevice(ftpName)).isTrue
        assertThat(rioClient.headEndpointDevice(uriName)).isTrue

        val deleteFtpResponse = rioClient.deleteEndpointDevice(ftpName)
        assertThat(deleteFtpResponse.statusCode).isEqualTo(HttpStatusCode.NoContent)

        val deleteUriResponse = rioClient.deleteEndpointDevice(uriName)
        assertThat(deleteUriResponse.statusCode).isEqualTo(HttpStatusCode.NoContent)

        assertThat(rioClient.headEndpointDevice(ftpName)).isFalse
        assertThat(rioClient.headEndpointDevice(uriName)).isFalse

        endpointList = rioClient.listEndpointDevices()
        assertThat(endpointList.page.totalItems).isEqualTo(endpointCount - 2)

        // TODO: endpoint unhappy path testing
    }

    @Test
    fun brokerTest() =
        blockingTest {
            try {
                removeBroker(testBroker)

                val agentConfig =
                    BpAgentConfig(
                        brokerBucket,
                        spectraDeviceCreateRequest.name,
                        spectraDeviceCreateRequest.username,
                        protect = false,
                    )
                val createRequest = BrokerCreateRequest(testBroker, testAgent, agentConfig.toConfigMap())

                val createBroker = rioClient.createBroker(createRequest)
                assertThat(createBroker.statusCode).isEqualTo(HttpStatusCode.Created)
                assertThat(rioClient.headBroker(testBroker)).isTrue

                val getBroker = rioClient.getBroker(testBroker)
                assertThat(getBroker.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(getBroker.name).isEqualTo(testBroker)

                val listBrokers = rioClient.listBrokers()
                assertThat(listBrokers.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(listBrokers.brokers).isNotEmpty
                assertThat(listBrokers.brokers.map { it.name }).contains(testBroker)

                assertRioListResponse("brokers", listBrokers.page.totalItems) { page, perPage ->
                    rioClient.listBrokers(page, perPage)
                }

                var getWriteAgent = rioClient.getAgent(testBroker, testAgent)
                assertThat(getWriteAgent.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(getWriteAgent.name).isEqualTo(testAgent)
                assertThat(getWriteAgent.type).isEqualTo("bp_agent")
                assertThat(getWriteAgent.writable).isEqualTo(true)
                assertThat(getWriteAgent.agentConfig).isEqualTo(agentConfig.toConfigMap())

                val listAgents = rioClient.listAgents(testBroker)
                assertThat(listAgents.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(listAgents.agents).hasSize(1)
                assertThat(listAgents.agents.first().name).isEqualTo(getWriteAgent.name)

                assertRioListResponse("agents", listAgents.page.totalItems) { page, perPage ->
                    rioClient.listAgents(testBroker, page, perPage)
                }

                val mapEntry = Pair("username", spectraDeviceAltUsername)
                val updateRequest = AgentUpdateRequest(mapOf(mapEntry))
                val newAgentConfig = agentConfig.toConfigMap().plus(mapEntry).toMap()
                val updateResponse = rioClient.updateAgent(testBroker, testAgent, updateRequest)
                assertThat(updateResponse.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(updateResponse.agentConfig).isEqualTo(newAgentConfig)

                getWriteAgent = rioClient.getAgent(testBroker, testAgent)
                assertThat(getWriteAgent.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(getWriteAgent.name).isEqualTo(testAgent)
                assertThat(getWriteAgent.type).isEqualTo("bp_agent")
                assertThat(getWriteAgent.writable).isEqualTo(true)
                assertThat(getWriteAgent.agentConfig).isEqualTo(newAgentConfig)

                val readAgentName = "test-read-agent"
                assertThat(rioClient.headAgent(testBroker, readAgentName)).isFalse

                val agentCreateRequest = AgentCreateRequest(readAgentName, "bp_agent", agentConfig.toConfigMap(), false)
                val createAgent = rioClient.createAgent(testBroker, agentCreateRequest)
                assertThat(createAgent.statusCode).isEqualTo(HttpStatusCode.Created)
                assertThat(createAgent.name).isEqualTo(readAgentName)
                assertThat(createAgent.type).isEqualTo("bp_agent")
                assertThat(createAgent.writable).isEqualTo(false)
                assertThat(createAgent.agentConfig).isEqualTo(agentConfig.toConfigMap())

                var getReadAgent = rioClient.getAgent(testBroker, readAgentName)
                assertThat(getReadAgent.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(getReadAgent.copy(lastIndexDate = null)).isEqualTo(createAgent)

                var i = 30
                while (getReadAgent.indexState != "COMPLETE" && --i > 0) {
                    delay(1000)
                    getReadAgent = rioClient.getAgent(testBroker, readAgentName, true)
                }
                assertThat(getReadAgent.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(getReadAgent.indexState).isEqualTo("COMPLETE")
                assertThat(getReadAgent.lastIndexDate).isNotNull

                rioClient.reindexAgent(testBroker, readAgentName)

                assertThat(rioClient.headAgent(testBroker, readAgentName)).isTrue
                val deleteAgentResponse = rioClient.deleteAgent(testBroker, readAgentName, true)
                assertThat(deleteAgentResponse.statusCode).isEqualTo(HttpStatusCode.NoContent)
                assertThat(rioClient.headAgent(testBroker, readAgentName)).isFalse

                // TODO broker unhappy path testing
                // TODO agent unhappy path testing
            } finally {
                removeBroker(testBroker)
            }
        }

    @Test
    fun jobTest() =
        blockingTest {
            try {
                ensureBrokerExists()

                val archiveJobName = "archive-job-${uuid()}"
                val metadata = mapOf(Pair("key1", "val1"), Pair("key2", "val2"))
                val archiveRequest =
                    ArchiveRequest(
                        archiveJobName,
                        listOf(
                            FileToArchive(uuid(), URI("aToZSequence://file"), 1024L, metadata),
                            FileToArchive(uuid(), URI("aToZSequence://file"), 2048L, metadata),
                        ),
                    )
                val archiveJob = rioClient.createArchiveJob(testBroker, archiveRequest, failFast = true)
                assertThat(archiveJob.statusCode).isEqualTo(HttpStatusCode.Created)
                assertThat(archiveJob.name).isEqualTo(archiveJobName)
                assertThat(archiveJob.numberOfFiles).isEqualTo(2)
                assertThat(archiveJob.totalSizeInBytes).isEqualTo(3072)

                assertThat(rioClient.headJob(archiveJob.id.toString())).isTrue

                var i = 25
                var archiveJobStatus = rioClient.jobStatus(archiveJob.id)
                while (archiveJobStatus.status.status == "ACTIVE" && --i > 0) {
                    delay(1000)
                    archiveJobStatus = rioClient.jobStatus(archiveJob.id)
                }
                assertThat(archiveJobStatus.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(archiveJobStatus.status.status).isEqualTo("COMPLETED")
                assertThat(archiveJobStatus.filesTransferred).isEqualTo(2)
                assertThat(archiveJobStatus.progress).isEqualTo(1.0f)
                assertThat(archiveJobStatus.files.first().fileId).isNotBlank()

                val archiveFilesStatus = rioClient.fileStatus(archiveJob.id)
                assertThat(archiveFilesStatus.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(archiveFilesStatus.fileStatus).hasSize(6)

                val archiveFileStatus = rioClient.fileStatus(archiveJob.id, archiveRequest.files[0].name)
                assertThat(archiveFileStatus.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(archiveFileStatus.fileStatus).hasSize(3)

                rioClient.listJobs(fileName = archiveRequest.files[1].name).let { resp ->
                    assertThat(resp.jobs).hasSize(1)
                }

                val restoreJobName = "restore-job-${uuid()}"
                val restoreRequest =
                    RestoreRequest(
                        restoreJobName,
                        listOf(
                            FileToRestore(archiveRequest.files[0].name, URI("null://name1")),
                            FileToRestore(archiveRequest.files[1].name, URI("null://name2")),
                        ),
                    )
                val restoreJob = rioClient.createRestoreJob(testBroker, restoreRequest)
                assertThat(restoreJob.statusCode).isEqualTo(HttpStatusCode.Created)
                assertThat(restoreJob.name).isEqualTo(restoreJobName)
                assertThat(restoreJob.numberOfFiles).isEqualTo(2)

                assertThat(rioClient.headJob(restoreJob.id.toString())).isTrue

                rioClient.listJobs(fileName = archiveRequest.files[1].name).let { resp ->
                    assertThat(resp.jobs).hasSize(2)
                }

                i = 25
                var restoreJobStatus = rioClient.jobStatus(restoreJob.id)
                while (restoreJobStatus.status.status == "ACTIVE" && --i > 0) {
                    delay(1000)
                    restoreJobStatus = rioClient.jobStatus(restoreJob.id)
                }
                assertThat(restoreJobStatus.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(restoreJobStatus.status.status).isEqualTo("COMPLETED")
                assertThat(restoreJobStatus.filesTransferred).isEqualTo(2)
                assertThat(restoreJobStatus.progress).isEqualTo(1.0f)
                assertThat(restoreJobStatus.files.first().fileId).isNotBlank()

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

                assertRioListResponse("completed jobs", jobList.page.totalItems) { page, perPage ->
                    rioClient.listJobs(broker = testBroker, jobStatus = "COMPLETED", page = page, perPage = perPage)
                }

                val totalJobs = jobList.page.totalItems
                val deleteArchiveJobResponse = rioClient.deleteJob(archiveJob.id)
                assertThat(deleteArchiveJobResponse.statusCode).isEqualTo(HttpStatusCode.NoContent)

                val deleteRestoreJobResponse = rioClient.deleteJob(restoreJob.id)
                assertThat(deleteRestoreJobResponse.statusCode).isEqualTo(HttpStatusCode.NoContent)

                assertThat(rioClient.headJob(archiveJob.id.toString())).isFalse
                assertThat(rioClient.headJob(restoreJob.id.toString())).isFalse

                jobList = rioClient.listJobs(broker = testBroker, jobStatus = "COMPLETED")
                assertThat(jobList.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(jobList.page.totalItems).isEqualTo(totalJobs - 2)

                val allJobCount = rioClient.listJobs().page.totalItems
                val spectraJobCount = rioClient.listJobs(createBy = 1L).page.totalItems
                assertThat(spectraJobCount).isLessThanOrEqualTo(allJobCount)
                // assertThat(spectraJobCount).isLessThan(allJobCount)  Not always true, but a better test

                // TODO job unhappy path testing
            } finally {
                removeBroker(testBroker)
            }
        }

    @Test
    fun jobCallbackTest() =
        blockingTest {
            try {
                ensureBrokerExists()

                val archiveJobName = "archive-job-callback-${uuid()}"
                val jobMetadata = mapOf(Pair("jobkey1", "jobval1"), Pair("jobkey2", "jobval2"))
                val metadata = mapOf(Pair("key1", "val1"), Pair("key2", "val2"))
                val archiveJobCallbacks =
                    listOf(
                        JobCallback("http://host/path?archive=job", "JOB", "COMPLETE"),
                        JobCallback("http://host/path?archive=file", "FILE", "COMPLETE"),
                    )
                val archiveRequest =
                    ArchiveRequest(
                        archiveJobName,
                        listOf(
                            FileToArchive(uuid(), URI("aToZSequence://file"), 1024L, metadata),
                            FileToArchive(uuid(), URI("aToZSequence://file"), 2048L, metadata),
                        ),
                        jobMetadata,
                        archiveJobCallbacks,
                    )
                val archiveJob = rioClient.createArchiveJob(testBroker, archiveRequest)
                assertThat(archiveJob.statusCode).isEqualTo(HttpStatusCode.Created)
                assertThat(archiveJob.name).isEqualTo(archiveJobName)
                assertThat(archiveJob.numberOfFiles).isEqualTo(2)
                assertThat(archiveJob.totalSizeInBytes).isEqualTo(3072)
                assertThat(archiveJob.callbacks).isEqualTo(archiveJobCallbacks)

                assertThat(rioClient.headJob(archiveJob.id.toString())).isTrue

                val archiveJobs = rioClient.listJobs(jobName = archiveJobName)
                assertThat(archiveJobs.jobs).hasSize(1)
                assertThat(archiveJobs.jobs[0].callbacks).isEqualTo(archiveJobCallbacks)

                var i = 25
                var archiveJobStatus = rioClient.jobStatus(archiveJob.id)
                while (archiveJobStatus.status.status == "ACTIVE" && --i > 0) {
                    delay(1000)
                    archiveJobStatus = rioClient.jobStatus(archiveJob.id)
                }
                assertThat(archiveJobStatus.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(archiveJobStatus.status.status).isEqualTo("COMPLETED")
                assertThat(archiveJobStatus.filesTransferred).isEqualTo(2)
                assertThat(archiveJobStatus.progress).isEqualTo(1.0f)
                assertThat(archiveJobStatus.callbacks).isEqualTo(archiveJobCallbacks)

                val restoreJobName = "restore-job-callback-${uuid()}"
                val restoreJobCallbacks =
                    listOf(
                        JobCallback("http://host/path?restore=file", "FILE", "COMPLETE"),
                        JobCallback("http://host/path?restore=job", "JOB", "COMPLETE"),
                    )
                val restoreRequest =
                    RestoreRequest(
                        restoreJobName,
                        listOf(
                            FileToRestore(archiveRequest.files[0].name, URI("null://name1")),
                            FileToRestore(archiveRequest.files[1].name, URI("null://name2")),
                        ),
                        restoreJobCallbacks,
                    )
                val restoreJob = rioClient.createRestoreJob(testBroker, restoreRequest)
                assertThat(restoreJob.statusCode).isEqualTo(HttpStatusCode.Created)
                assertThat(restoreJob.name).isEqualTo(restoreJobName)
                assertThat(restoreJob.numberOfFiles).isEqualTo(2)
                assertThat(restoreJob.callbacks).isEqualTo(restoreJobCallbacks)

                assertThat(rioClient.headJob(restoreJob.id.toString())).isTrue

                val restoreJobs = rioClient.listJobs(jobName = restoreJobName)
                assertThat(restoreJobs.jobs).hasSize(1)
                assertThat(restoreJobs.jobs[0].callbacks).isEqualTo(restoreJobCallbacks)

                i = 25
                var restoreJobStatus = rioClient.jobStatus(restoreJob.id)
                while (restoreJobStatus.status.status == "ACTIVE" && --i > 0) {
                    delay(1000)
                    restoreJobStatus = rioClient.jobStatus(restoreJob.id)
                }
                assertThat(restoreJobStatus.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(restoreJobStatus.status.status).isEqualTo("COMPLETED")
                assertThat(restoreJobStatus.filesTransferred).isEqualTo(2)
                assertThat(restoreJobStatus.progress).isEqualTo(1.0f)
                assertThat(restoreJobStatus.callbacks).isEqualTo(restoreJobCallbacks)
            } finally {
                removeBroker(testBroker)
            }
        }

    @Test
    fun archiveFolderJobGroupTest(
        @TempDir tempDir: Path,
    ) = blockingTest {
        val uuid = UUID.randomUUID()
        val endpointName = "endpoint-$uuid"
        val endpointPath = tempDir.resolve("endpoint")
        endpointPath.toFile().mkdir()
        val endpointDirs =
            (1..3).map { idx ->
                endpointPath
                    .resolve("endpoint-dir-$idx")
                    .also { it.toFile().mkdir() }
            }
        val endpointFiles: List<File> =
            endpointDirs
                .mapIndexed { epIdx, epDir ->
                    (1..3).map { fileIdx ->
                        epDir
                            .resolve("Endpoint-file-$epIdx-$fileIdx.txt")
                            .toFile()
                            .also { it.writeText("This is an endpoint file ${it.name}") }
                    }
                }.flatten()
        val files: List<File> =
            (1..10).map { idx ->
                endpointPath
                    .resolve("file-$idx.txt")
                    .toFile()
                    .also { it.writeText("This is a plain file") }
            }
        val req =
            ArchiveFolderRequest(
                jobName = "ArchiveFolder $uuid",
                prefix = "$uuid/",
                files = files.map { FileToArchive(it.name, it.toURI(), null) },
                folders = endpointDirs.map { FolderToArchive(it.toUri()) },
            )

        try {
            ensureBrokerExists()

            rioClient
                .createUriEndpointDevice(
                    UriEndpointDeviceCreateRequest(endpointName, endpointPath.toUri().toString()),
                ).let { resp ->
                    assertThat(resp.statusCode).isEqualTo(HttpStatusCode.Created)
                }

            val jobGroupId: UUID =
                rioClient.createArchiveFolderJob(testBroker, req).let { resp ->
                    assertThat(resp.statusCode).isEqualTo(HttpStatusCode.Created)
                    UUID.fromString(resp.jobGroupId)
                }
            var tries = 100

            do {
                delay(20000)
                val jgStatus = rioClient.jobGroupStatus(jobGroupId)
                assertThat(jgStatus.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(jgStatus.jobs).isNotEmpty

                val activeJobCount = jgStatus.jobs.filter { it.status.status == "ACTIVE" }.size

                rioClient.listJobGroups().let { resp ->
                    assertThat(resp.statusCode).isEqualTo(HttpStatusCode.OK)
                    assertThat(resp.jobGroups.map { it.groupId }).contains(jobGroupId.toString())
                }
            } while (--tries > 0 && activeJobCount > 0)
            val jgJobCount: Int =
                rioClient.listJobs(groupId = jobGroupId).let { resp ->
                    assertThat(resp.statusCode).isEqualTo(HttpStatusCode.OK)
                    assertThat(resp.page.totalItems).isGreaterThan(0)
                    resp.page.totalItems.toInt()
                }
            rioClient.jobGroupStatus(jobGroupId).let { resp ->
                assertThat(resp.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(resp.errorCount).isEqualTo(0)
                assertThat(resp.failedFiles).isEmpty()
                assertThat(resp.jobs).hasSize(jgJobCount)
            }
        } finally {
            if (rioClient.headEndpointDevice(endpointName)) {
                rioClient.deleteEndpointDevice(endpointName)
            }
        }
    }

    @Test
    fun objectTest() =
        blockingTest {
            try {
                ensureBrokerExists()

                var listObjects = rioClient.listObjects(testBroker)
                val totalObjects = listObjects.page.totalItems

                assertRioListResponse("objects", listObjects.page.totalItems) { page, perPage ->
                    rioClient.listObjects(brokerName = testBroker, page = page, perPage = perPage)
                }

                var countResponse = rioClient.objectCount(testBroker)
                assertThat(countResponse.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(countResponse.objectCount).isEqualTo(totalObjects)

                val objectName = "object-${uuid()}"
                val metadata = mapOf(Pair("key1", "val1"))
                val archiveRequest =
                    ArchiveRequest(
                        "archive-job-${uuid()}",
                        listOf(
                            FileToArchive(objectName, URI("aToZSequence://file"), 1024L, metadata),
                        ),
                    )
                val archiveJob = rioClient.createArchiveJob(testBroker, archiveRequest)
                assertThat(archiveJob.statusCode).isEqualTo(HttpStatusCode.Created)

                var i = 25
                var archiveJobStatus = rioClient.jobStatus(archiveJob.id)
                assertThat(archiveJobStatus.statusCode).isEqualTo(HttpStatusCode.OK)
                while (archiveJobStatus.status.status == "ACTIVE" && --i > 0) {
                    delay(1000)
                    archiveJobStatus = rioClient.jobStatus(archiveJob.id)
                }
                assertThat(archiveJobStatus.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(archiveJobStatus.status.status).isEqualTo("COMPLETED")

                listObjects = rioClient.listObjects(testBroker)
                assertThat(listObjects.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(listObjects.page.totalItems).isEqualTo(totalObjects + 1)

                countResponse = rioClient.objectCount(testBroker)
                assertThat(countResponse.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(countResponse.objectCount).isEqualTo(totalObjects + 1)

                listObjects = rioClient.listObjects(brokerName = testBroker, filename = objectName)
                assertThat(listObjects.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(listObjects.page.totalItems).isEqualTo(1)

                countResponse = rioClient.objectCount(brokerName = testBroker, filename = objectName)
                assertThat(countResponse.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(countResponse.objectCount).isEqualTo(1)

                val getObject = rioClient.getObject(testBroker, objectName)
                assertThat(getObject.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(getObject.broker).isEqualTo(testBroker)
                assertThat(getObject.name).isEqualTo(objectName)
                assertThat(getObject.size).isEqualTo(archiveRequest.files[0].size)
                assertThat(getObject.metadata).isEqualTo(metadata)

                rioClient.getObject(testBroker, objectName, includeAgentCopies = true).let { resp ->
                    assertThat(resp.statusCode).isEqualTo(HttpStatusCode.OK)
                    assertThat(resp.broker).isEqualTo(testBroker)
                    assertThat(resp.name).isEqualTo(objectName)
                    assertThat(resp.size).isEqualTo(archiveRequest.files[0].size)
                    assertThat(resp.metadata).isEqualTo(metadata)
                    assertThat(resp.copies).hasSize(1)
                    resp.copies?.get(0)?.agentName.let { copyAgentName ->
                        assertThat(copyAgentName).isEqualTo(testAgent)
                    }
                }

                rioClient.listObjects(testBroker, filename = objectName, includeAgentCopies = true).let { resp ->
                    assertThat(resp.statusCode).isEqualTo(HttpStatusCode.OK)
                    assertThat(resp.objects).hasSize(1)
                    resp.objects[0].let { objData ->
                        assertThat(objData.broker).isEqualTo(testBroker)
                        assertThat(objData.name).isEqualTo(objectName)
                        assertThat(objData.size).isEqualTo(archiveRequest.files[0].size)
                        assertThat(objData.metadata).isEqualTo(metadata)
                        assertThat(objData.copies).hasSize(1)
                        objData.copies?.get(0)?.agentName.let { copyAgentName ->
                            assertThat(copyAgentName).isEqualTo(testAgent)
                        }
                    }
                }

                val newMetadata = mapOf(Pair("key9", "val9"))
                val updateObject = rioClient.updateObject(testBroker, objectName, newMetadata)
                assertThat(updateObject.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(updateObject).isEqualTo(getObject.copy(metadata = newMetadata))

                assertThat(rioClient.objectExists(testBroker, objectName)).isTrue

                val objectBatchHeadRequest = ObjectBatchHeadRequest(listOf(objectName, "bad-object-1", "bad-object-2"))
                val objectBatchHeadResponse = rioClient.objectBatchHead(testBroker, objectBatchHeadRequest)
                assertThat(objectBatchHeadResponse.objects).hasSize(3)
                objectBatchHeadResponse.objects.forEach {
                    assertThat(it.found).isEqualTo(objectName == it.name)
                }

                val deleteObjectResponse = rioClient.deleteObject(testBroker, objectName)
                assertThat(deleteObjectResponse.statusCode).isEqualTo(HttpStatusCode.NoContent)
                assertThat(rioClient.objectExists(testBroker, objectName)).isFalse

                listObjects = rioClient.listObjects(testBroker)
                assertThat(listObjects.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(listObjects.page.totalItems).isEqualTo(totalObjects)

                // TODO: object unhappy path testing
            } finally {
                removeBroker(testBroker)
            }
        }

    @Test
    fun objectMetadataTest() =
        blockingTest {
            val uuid = uuid()
            val fileList: List<String> = (1..10).map { "metaObject-$uuid-$it" }

            val metadata1 = mapOf(Pair("m1", "v1"), Pair("m2", "v2"), Pair("m3", "v3"))
            val metadata2 = mapOf(Pair("m1-a", "v1-a"), Pair("m2-b", "v2-b"), Pair("m3-c", "v3-c"))
            val internalMetadata1 = mapOf(Pair("i1", "y1"), Pair("i2", "y2"))
            val internalMetadata2 = mapOf(Pair("i1-x", "y1-x"), Pair("i2-z", "y2-z"))

            val metadata1UpdateRequest: List<ObjectUpdateRequest> =
                fileList.map {
                    ObjectUpdateRequest(it, metadata1)
                }
            val metadata2UpdateRequest: List<ObjectUpdateRequest> =
                fileList.map {
                    ObjectUpdateRequest(it, metadata2)
                }
            val internalMetadata1UpdateRequest: List<ObjectUpdateRequest> =
                fileList.map {
                    ObjectUpdateRequest(it, internalMetadata1)
                }
            val internalMetadata2UpdateRequest: List<ObjectUpdateRequest> =
                fileList.map {
                    ObjectUpdateRequest(it, internalMetadata2)
                }

            val objectBroker = "object-broker"

            try {
                if (!rioClient.headBroker(objectBroker)) {
                    val agentConfig =
                        BpAgentConfig(brokerObjectBucket, spectraDeviceCreateRequest.name, spectraDeviceCreateRequest.username)
                    val createRequest = BrokerCreateRequest(objectBroker, "agent-name", agentConfig.toConfigMap())
                    val createResponse = rioClient.createBroker(createRequest)
                    assertThat(createResponse.statusCode).isEqualTo(HttpStatusCode.Created)
                }

                val filesToArchive: List<FileToArchive> =
                    fileList.map {
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
                assertThat(metadata1Response.statusCode).isEqualTo(HttpStatusCode.NoContent)

                fileList.forEach { fileName ->
                    val getObject = rioClient.getObject(objectBroker, fileName, includeInternalMetadata = true)
                    assertThat(getObject.statusCode).isEqualTo(HttpStatusCode.OK)
                    assertThat(getObject.metadata).isEqualTo(metadata1)
                    assertThat(getObject.internalMetadata).isNullOrEmpty()
                }

                val internalMetadata1Request = ObjectBatchUpdateRequest(internalMetadata1UpdateRequest)
                val internalMetadata1Response = rioClient.updateObjects(objectBroker, internalMetadata1Request, internalData = true)
                assertThat(internalMetadata1Response.statusCode).isEqualTo(HttpStatusCode.NoContent)

                fileList.forEach { fileName ->
                    val getObject = rioClient.getObject(objectBroker, fileName, includeInternalMetadata = true)
                    assertThat(getObject.statusCode).isEqualTo(HttpStatusCode.OK)
                    assertThat(getObject.metadata).isEqualTo(metadata1)
                    assertThat(getObject.internalMetadata).isEqualTo(internalMetadata1)
                }

                val metadata2Request = ObjectBatchUpdateRequest(metadata2UpdateRequest)
                val metadata2Response = rioClient.updateObjects(objectBroker, metadata2Request, merge = true)
                assertThat(metadata2Response.statusCode).isEqualTo(HttpStatusCode.NoContent)

                val combinedMetadata = metadata1.plus(metadata2)
                fileList.forEach { fileName ->
                    val getObject = rioClient.getObject(objectBroker, fileName, includeInternalMetadata = true)
                    assertThat(getObject.statusCode).isEqualTo(HttpStatusCode.OK)
                    assertThat(getObject.metadata).isEqualTo(combinedMetadata)
                    assertThat(getObject.internalMetadata).isEqualTo(internalMetadata1)
                }

                val internalMetadata2Request = ObjectBatchUpdateRequest(internalMetadata2UpdateRequest)
                val internalMetadata2Response =
                    rioClient.updateObjects(
                        objectBroker,
                        internalMetadata2Request,
                        internalData = true,
                        merge = true,
                    )
                assertThat(internalMetadata2Response.statusCode).isEqualTo(HttpStatusCode.NoContent)

                val combinedInternalMetadata = internalMetadata1.plus(internalMetadata2)
                fileList.forEach { fileName ->
                    val getObject = rioClient.getObject(objectBroker, fileName, includeInternalMetadata = true)
                    assertThat(getObject.statusCode).isEqualTo(HttpStatusCode.OK)
                    assertThat(getObject.metadata).isEqualTo(combinedMetadata)
                    assertThat(getObject.internalMetadata).isEqualTo(combinedInternalMetadata)
                }

                val metadataResponse = rioClient.updateObjects(objectBroker, metadata1Request)
                assertThat(metadataResponse.statusCode).isEqualTo(HttpStatusCode.NoContent)

                fileList.forEach { fileName ->
                    val getObject = rioClient.getObject(objectBroker, fileName, includeInternalMetadata = true)
                    assertThat(getObject.statusCode).isEqualTo(HttpStatusCode.OK)
                    assertThat(getObject.metadata).isEqualTo(metadata1)
                    assertThat(getObject.internalMetadata).isEqualTo(combinedInternalMetadata)
                }

                val internalMetadataResponse = rioClient.updateObjects(objectBroker, internalMetadata1Request, internalData = true)
                assertThat(internalMetadataResponse.statusCode).isEqualTo(HttpStatusCode.NoContent)

                fileList.forEach { fileName ->
                    val getObject = rioClient.getObject(objectBroker, fileName, includeInternalMetadata = true)
                    assertThat(getObject.statusCode).isEqualTo(HttpStatusCode.OK)
                    assertThat(getObject.metadata).isEqualTo(metadata1)
                    assertThat(getObject.internalMetadata).isEqualTo(internalMetadata1)
                }
            } finally {
                // TODO: remove bucket or objects from bucket
                removeBroker(objectBroker)
            }
        }

    @Test
    fun logTest() =
        blockingTest {
            var listLogs = rioClient.listLogsets()
            val totalLogs = listLogs.page.totalItems

            val newLog = rioClient.createLogset()
            assertThat(newLog.statusCode).isEqualTo(HttpStatusCode.Accepted)

            var i = 50
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
            assertThat(listLogs.logs.map { it.id }).contains(newLog.id)

            assertRioListResponse("logsets", listLogs.page.totalItems) { page, perPage ->
                rioClient.listLogsets(page, perPage)
            }

            val deleteLogSetResponse = rioClient.deleteLogset(UUID.fromString(newLog.id))
            assertThat(deleteLogSetResponse.statusCode).isEqualTo(HttpStatusCode.NoContent)
            assertThat(rioClient.headLogset(UUID.fromString(newLog.id))).isFalse

            listLogs = rioClient.listLogsets()
            assertThat(listLogs.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(listLogs.page.totalItems).isEqualTo(totalLogs)

            // TODO: log unhappy path testing
        }

    @Test
    fun logLevelTest() =
        blockingTest {
            var previousLevel = rioClient.getLogLevel().currentLevel
            listOf("TRACE", "INFO", "ERROR", "WARN", "DEBUG").forEach { logLevel ->
                val resp = rioClient.setLogLevel(logLevel)
                assertThat(resp.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(resp.currentLevel).isEqualTo(logLevel)
                assertThat(resp.requestedLevel).isEqualTo(logLevel)
                assertThat(resp.previousLevel).isEqualTo(previousLevel)
                assertThat(rioClient.getLogLevel().currentLevel).isEqualTo(logLevel)
                previousLevel = logLevel
            }

            val ex =
                catchThrowableOfType(
                    {
                        runBlocking {
                            rioClient.setLogLevel("bad")
                        }
                    },
                    RioHttpException::class.java,
                )
            assertThat(ex.statusCode).isEqualTo(HttpStatusCode.BadRequest.value)
            assertThat(ex.errorMessage().message).isEqualTo("Log Level bad is invalid. Log level change request denied.")
        }

    @Test
    fun systemTest() =
        blockingTest {
            val systemResponse = rioClient.systemInfo()
            assertThat(systemResponse.statusCode).isEqualTo(HttpStatusCode.OK)

            assertThat(systemResponse.apiVersion).isNotBlank
            assertThat(systemResponse.buildDate).isNotBlank
            assertThat(systemResponse.buildNumber).isNotBlank
            assertThat(systemResponse.buildType).isNotBlank
            assertThat(systemResponse.gitCommitHash).isNotBlank
            assertThat(systemResponse.version).isNotBlank

            assertThat(systemResponse.runtimeStats.freeMemory).isGreaterThan(0)
            assertThat(systemResponse.runtimeStats.totalMemory).isGreaterThan(0)
            assertThat(systemResponse.runtimeStats.uptime).isGreaterThan(0)
            assertThat(systemResponse.runtimeStats.usedMemory).isGreaterThan(0)

            assertThat(systemResponse.server.jvm.version).isNotBlank
            assertThat(systemResponse.server.jvm.vendor).isNotBlank
            assertThat(systemResponse.server.jvm.vmName).isNotBlank
            assertThat(systemResponse.server.jvm.vmVersion).isNotBlank

            assertThat(systemResponse.server.operatingSystem.name).isNotBlank
            assertThat(systemResponse.server.operatingSystem.arch).isNotBlank
            assertThat(systemResponse.server.operatingSystem.cores).isGreaterThan(0)
            assertThat(systemResponse.server.operatingSystem.version).isNotBlank
        }

    @Test
    fun systemClientDataTest() =
        blockingTest {
            val total = 6L
            val uuid = UUID.randomUUID()
            val clientName = "clientName-$uuid"
            val clientDataIdBase = "cdi-$uuid"
            val tagBase = "tag-$uuid"
            var testNum = 0
            val cdtDescFmt = "ClientDataTest %d"

            (1..total).forEach { idx ->
                val clientDataId = "$clientDataIdBase-$idx"
                val tag = "$tagBase-${idx % 2}"
                val mapData =
                    mapOf(
                        Pair("key1-$idx", "val1-$idx"),
                        Pair("key2-$idx", "val2-$idx"),
                        Pair("key3-$idx", "val3-$idx"),
                    )
                val data = rioClient.clientDataInsert(ClientDataRequest(clientDataId, clientName, tag, mapData))
                assertThat(data.statusCode).isEqualTo(HttpStatusCode.Created)
                assertThat(data.clientDataId).isEqualTo(clientDataId)
                assertThat(data.clientName).isEqualTo(clientName)
                assertThat(data.tag).isEqualTo(tag)
                assertThat(data.mapData).isEqualTo(mapData)
            }

            var listData = rioClient.clientDataList(clientDataId = "$clientDataIdBase-*")
            assertThat(listData.statusCode).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(HttpStatusCode.OK)
            assertThat(listData.page.totalItems).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(total)
            assertThat(listData.result).describedAs(cdtDescFmt.format(++testNum)).hasSize(total.toInt())
            listData = rioClient.clientDataList(clientDataId = "$clientDataIdBase-1")
            assertThat(listData.statusCode).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(HttpStatusCode.OK)
            assertThat(listData.page.totalItems).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(1)
            assertThat(listData.result).describedAs(cdtDescFmt.format(++testNum)).hasSize(1)
            listData = rioClient.clientDataList(clientDataId = "abc*")
            assertThat(listData.statusCode).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(HttpStatusCode.OK)
            assertThat(listData.page.totalItems).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(0)
            assertThat(listData.result).describedAs(cdtDescFmt.format(++testNum)).hasSize(0)

            val half = total / 2
            listData = rioClient.clientDataList(tag = "$tagBase-*")
            assertThat(listData.statusCode).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(HttpStatusCode.OK)
            assertThat(listData.page.totalItems).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(total)
            assertThat(listData.result).describedAs(cdtDescFmt.format(++testNum)).hasSize(total.toInt())
            listData = rioClient.clientDataList(tag = "$tagBase-0")
            assertThat(listData.statusCode).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(HttpStatusCode.OK)
            assertThat(listData.page.totalItems).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(half)
            assertThat(listData.result).describedAs(cdtDescFmt.format(++testNum)).hasSize(half.toInt())
            listData = rioClient.clientDataList(tag = "$tagBase-1")
            assertThat(listData.statusCode).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(HttpStatusCode.OK)
            assertThat(listData.page.totalItems).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(half)
            assertThat(listData.result).describedAs(cdtDescFmt.format(++testNum)).hasSize(half.toInt())

            listData = rioClient.clientDataList(clientDataId = "$clientDataIdBase-*", page = 1, perPage = 2)
            assertThat(listData.statusCode).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(HttpStatusCode.OK)
            assertThat(listData.page.totalItems).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(total)
            assertThat(listData.result).describedAs(cdtDescFmt.format(++testNum)).hasSize(2)

            val getItem = listData.result.first()
            val getData = rioClient.clientDataGet(getItem.dataId)
            assertThat(getData.statusCode).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(HttpStatusCode.OK)
            assertThat(getData.dataId).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(getItem.dataId)
            assertThat(getData.clientDataId).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(getItem.clientDataId)
            assertThat(getData.clientName).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(getItem.clientName)
            assertThat(getData.tag).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(getItem.tag)
            assertThat(getData.mapData).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(getItem.mapData)

            val updateMapData =
                getItem.mapData
                    .map { (k, v) ->
                        Pair("new$k", "new$v")
                    }.toMap()
            val updatedItem =
                rioClient.clientDataUpdate(
                    getItem.dataId,
                    ClientDataRequest("123", "456", "789", updateMapData),
                )
            assertThat(updatedItem.statusCode).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(HttpStatusCode.OK)
            assertThat(updatedItem.dataId).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(getItem.dataId)
            assertThat(updatedItem.clientDataId).describedAs(cdtDescFmt.format(++testNum)).isEqualTo("123")
            assertThat(updatedItem.clientName).describedAs(cdtDescFmt.format(++testNum)).isEqualTo("456")
            assertThat(updatedItem.tag).describedAs(cdtDescFmt.format(++testNum)).isEqualTo("789")
            assertThat(updatedItem.mapData).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(updateMapData)

            val getUpdatedItem = rioClient.clientDataGet(getItem.dataId)
            assertThat(getUpdatedItem.statusCode).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(HttpStatusCode.OK)
            assertThat(getUpdatedItem.dataId).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(getItem.dataId)
            assertThat(getUpdatedItem.clientDataId).describedAs(cdtDescFmt.format(++testNum)).isEqualTo("123")
            assertThat(getUpdatedItem.clientName).describedAs(cdtDescFmt.format(++testNum)).isEqualTo("456")
            assertThat(getUpdatedItem.tag).describedAs(cdtDescFmt.format(++testNum)).isEqualTo("789")
            assertThat(getUpdatedItem.mapData).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(updateMapData)

            rioClient.clientDataList(clientDataId = "clientDataId-*").result.forEach {
                val resp = rioClient.clientDataDelete(it.dataId)
                assertThat(resp.statusCode).isEqualTo(HttpStatusCode.NoContent)
            }

            val resp = rioClient.clientDataList(clientDataId = "clientDataId-*")
            assertThat(resp.statusCode).describedAs(cdtDescFmt.format(++testNum)).isEqualTo(HttpStatusCode.OK)
            assertThat(resp.result).describedAs(cdtDescFmt.format(++testNum)).hasSize(0)
        }

    @Test
    fun systemRioClientTest() =
        blockingTest {
            val uuid = UUID.randomUUID()
            val appName = "rio client test $uuid"
            var testNum = 0
            val testDesc = "RegisterClientTest %d"
            val rc1 =
                rioClient.saveRioClient(appName, "1.2.3", 9999, "/app", true).let { resp ->
                    assertThat(resp.statusCode).describedAs(testDesc.format(++testNum)).isEqualTo(HttpStatusCode.Created)
                    assertThat(resp.application).describedAs(testDesc.format(++testNum)).isEqualTo(appName)
                    assertThat(resp.version).describedAs(testDesc.format(++testNum)).isEqualTo("1.2.3")
                    assertThat(resp.ipUrl).describedAs(testDesc.format(++testNum)).startsWith("https://")
                    assertThat(resp.fqdnUrl).describedAs(testDesc.format(++testNum)).startsWith("https://")
                    assertThat(resp.ipUrl).describedAs(testDesc.format(++testNum)).endsWith(":9999/app")
                    assertThat(resp.fqdnUrl).describedAs(testDesc.format(++testNum)).endsWith(":9999/app")
                    resp
                }
            rioClient.getRioClient(rc1.id).let { resp ->
                assertThat(resp.statusCode).describedAs(testDesc.format(++testNum)).isEqualTo(HttpStatusCode.OK)
                assertThat(resp.application).describedAs(testDesc.format(++testNum)).isEqualTo(rc1.application)
                assertThat(resp.macAddress).describedAs(testDesc.format(++testNum)).isEqualTo(rc1.macAddress)
                assertThat(resp.version).describedAs(testDesc.format(++testNum)).isEqualTo(rc1.version)
                assertThat(resp.ipUrl).describedAs(testDesc.format(++testNum)).isEqualTo(rc1.ipUrl)
                assertThat(resp.fqdnUrl).describedAs(testDesc.format(++testNum)).isEqualTo(rc1.fqdnUrl)
                assertThat(resp.createDate).describedAs(testDesc.format(++testNum)).isEqualTo(rc1.createDate)
                assertThat(resp.accessDate).describedAs(testDesc.format(++testNum)).isEqualTo(rc1.accessDate)
            }
            rioClient.listRioClients(appName).let { resp ->
                assertThat(resp.statusCode).describedAs(testDesc.format(++testNum)).isEqualTo(HttpStatusCode.OK)
                assertThat(resp.page.totalItems).describedAs(testDesc.format(++testNum)).isEqualTo(1)
            }
            rioClient
                .updateRioClient(
                    rc1.id,
                    "New name $uuid",
                    "https://10.10.10.10:1010/api",
                    "https://host.domain.com:2020/api",
                ).let { resp ->
                    assertThat(resp.statusCode).describedAs(testDesc.format(++testNum)).isEqualTo(HttpStatusCode.OK)
                    assertThat(resp.application).describedAs(testDesc.format(++testNum)).isEqualTo(appName)
                    assertThat(resp.macAddress).describedAs(testDesc.format(++testNum)).isEqualTo(rc1.macAddress)
                    assertThat(resp.version).describedAs(testDesc.format(++testNum)).isEqualTo(rc1.version)
                    assertThat(resp.createDate).describedAs(testDesc.format(++testNum)).isEqualTo(rc1.createDate)
                    assertThat(resp.accessDate).describedAs(testDesc.format(++testNum)).isEqualTo(rc1.accessDate)
                    assertThat(resp.name).describedAs(testDesc.format(++testNum)).isEqualTo("New name $uuid")
                    assertThat(resp.ipUrl).describedAs(testDesc.format(++testNum)).isEqualTo("https://10.10.10.10:1010/api")
                    assertThat(resp.fqdnUrl).describedAs(testDesc.format(++testNum)).isEqualTo("https://host.domain.com:2020/api")
                }
            delay(1500)
            rioClient.saveRioClient(appName, "3.2.1", 9999, "/app", true).let { resp ->
                assertThat(resp.statusCode).describedAs(testDesc.format(++testNum)).isEqualTo(HttpStatusCode.Created)
                assertThat(resp.application).describedAs(testDesc.format(++testNum)).isEqualTo(rc1.application)
                assertThat(resp.macAddress).describedAs(testDesc.format(++testNum)).isEqualTo(rc1.macAddress)
                assertThat(resp.version).describedAs(testDesc.format(++testNum)).isEqualTo("3.2.1")
                assertThat(resp.ipUrl).describedAs(testDesc.format(++testNum)).isEqualTo("https://10.10.10.10:1010/api")
                assertThat(resp.fqdnUrl).describedAs(testDesc.format(++testNum)).isEqualTo("https://host.domain.com:2020/api")
                assertThat(resp.createDate).describedAs(testDesc.format(++testNum)).isEqualTo(rc1.createDate)
                assertThat(resp.accessDate).describedAs(testDesc.format(++testNum)).isNotEqualTo(rc1.accessDate)
            }
            rioClient.listRioClientApplications().let { resp ->
                assertThat(resp.applications).describedAs(testDesc.format(++testNum)).contains(appName)
            }
            rioClient.deleteRioClient(rc1.id).let { resp ->
                assertThat(resp.statusCode).describedAs(testDesc.format(++testNum)).isEqualTo(HttpStatusCode.NoContent)
            }
            try {
                rioClient.getRioClient(rc1.id).let { resp ->
                    assertThat(resp.statusCode).describedAs(testDesc.format(++testNum)).isEqualTo(HttpStatusCode.NotFound)
                }
            } catch (e: RioHttpException) {
                assertThat(e.statusCode).describedAs(testDesc.format(++testNum)).isEqualTo(HttpStatusCode.NotFound.value)
            }
            rioClient.listRioClientApplications().let { resp ->
                assertThat(resp.applications).describedAs(testDesc.format(++testNum)).doesNotContain(appName)
            }
        }

    @Test
    fun keysTest() =
        blockingTest {
            var listTokens = rioClient.listTokenKeys()
            val totalTokens = listTokens.page.totalItems

            assertRioListResponse("tokens", listTokens.page.totalItems) { page, perPage ->
                rioClient.listTokenKeys(page, perPage)
            }

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
            assertThat(listTokens.data.map { it.id }).containsAll(listOf(createToken.id, longToken.id))

            assertThat(rioClient.headApiToken(createToken.id)).isTrue
            val deleteApiTokenResponse = rioClient.deleteApiToken(createToken.id)
            assertThat(deleteApiTokenResponse.statusCode).isEqualTo(HttpStatusCode.NoContent)
            assertThat(rioClient.headApiToken(createToken.id)).isFalse

            assertThat(rioClient.headApiToken(longToken.id)).isTrue
            val deleteLongApiTokenResponse = rioClient.deleteApiToken(longToken.id)
            assertThat(deleteLongApiTokenResponse.statusCode).isEqualTo(HttpStatusCode.NoContent)
            assertThat(rioClient.headApiToken(longToken.id)).isFalse

            listTokens = rioClient.listTokenKeys()
            assertThat(listTokens.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(listTokens.page.totalItems).isEqualTo(totalTokens)

            // TODO: keys unhappy path testing
        }

    @Test
    fun clusterTest() =
        blockingTest {
            val resp = rioClient.getCluster()
            assertThat(resp.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(resp.clusterName).isNotNull.isNotBlank

            val listResponse = rioClient.listClusterMembers()
            assertThat(listResponse.statusCode).isEqualTo(HttpStatusCode.OK)
            assertThat(listResponse.members).isNotEmpty
            listResponse.members.first().let { member ->
                assertThat(member.memberId).isNotBlank
                assertThat(member.clusterPort).isGreaterThan(0)
                assertThat(member.httpPort).isGreaterThan(0)
                assertThat(member.role).isNotBlank
                assertThat(member.ipAddress).isNotBlank
            }
            listResponse.page().let { page ->
                assertThat(page.number).isEqualTo(0L)
                assertThat(page.pageSize).isEqualTo(listResponse.members.size.toLong())
                assertThat(page.totalPages).isEqualTo(1L)
                assertThat(page.totalItems).isEqualTo(listResponse.members.size.toLong())
            }
        }

    @Test
    fun userTest() =
        blockingTest {
            val username = "user-${uuid()}"
            val password = "wordpass"
            var userUuid: UUID = UUID.randomUUID()
            var testNum = 0
            val testFmt = "userTest-%d"

            assertThat(rioClient.headUserLogin(UUID.randomUUID())).isFalse()
            rioClient
                .createUserLogin(
                    UserCreateRequest(username, username, password, false, true, "Operator"),
                ).let { resp ->
                    assertThat(resp.statusCode).describedAs(testFmt.format(++testNum)).isEqualTo(HttpStatusCode.Created)
                    assertThat(resp.username).describedAs(testFmt.format(++testNum)).isEqualTo(username)
                    assertThat(resp.fullName).describedAs(testFmt.format(++testNum)).isEqualTo(username)
                    assertThat(resp.active).describedAs(testFmt.format(++testNum)).isFalse()
                    assertThat(resp.local).describedAs(testFmt.format(++testNum)).isTrue()
                    assertThat(resp.role).describedAs(testFmt.format(++testNum)).isEqualTo("Operator")
                    userUuid = resp.userUuid
                }
            assertThat(rioClient.headUserLogin(userUuid)).describedAs(testFmt.format(++testNum)).isTrue()
            assertThrows<RioHttpException> {
                rioClient.getBearerToken(username, "bad-password")
            }
            rioClient.getUserLogin(userUuid).let { resp ->
                assertThat(resp.statusCode).describedAs(testFmt.format(++testNum)).isEqualTo(HttpStatusCode.OK)
                assertThat(resp.username).describedAs(testFmt.format(++testNum)).isEqualTo(username)
                assertThat(resp.active).describedAs(testFmt.format(++testNum)).isFalse()
                assertThat(resp.local).describedAs(testFmt.format(++testNum)).isTrue()
                assertThat(resp.role).describedAs(testFmt.format(++testNum)).isEqualTo("Operator")
            }
            rioClient.updateUserLogin(userUuid, UserUpdateRequest(true, "Operator")).let { resp ->
                assertThat(resp.statusCode).describedAs(testFmt.format(++testNum)).isEqualTo(HttpStatusCode.OK)
                assertThat(resp.username).describedAs(testFmt.format(++testNum)).isEqualTo(username)
                assertThat(resp.active).describedAs(testFmt.format(++testNum)).isTrue()
                assertThat(resp.local).describedAs(testFmt.format(++testNum)).isTrue()
                assertThat(resp.role).describedAs(testFmt.format(++testNum)).isEqualTo("Operator")
            }
            rioClient.getBearerToken(username, password).let { resp ->
                assertThat(resp.statusCode).describedAs(testFmt.format(++testNum)).isEqualTo(HttpStatusCode.OK)
                assertThat(resp.username).describedAs(testFmt.format(++testNum)).isEqualTo(username)
                assertThat(resp.role).describedAs(testFmt.format(++testNum)).isEqualTo("Operator")
                assertThat(resp.token).describedAs(testFmt.format(++testNum)).isNotBlank()
            }
            rioClient.listUserLogins().let { resp ->
                assertThat(resp.statusCode).describedAs(testFmt.format(++testNum)).isEqualTo(HttpStatusCode.OK)
                assertThat(resp.users.map { it.username }).describedAs(testFmt.format(++testNum)).contains(username)
            }
            rioClient
                .updateUserLogin(
                    userUuid,
                    UserUpdateRequest(true, "Administrator"),
                ).let { resp ->
                    assertThat(resp.statusCode).describedAs(testFmt.format(++testNum)).isEqualTo(HttpStatusCode.OK)
                    assertThat(resp.username).describedAs(testFmt.format(++testNum)).isEqualTo(username)
                    assertThat(resp.active).describedAs(testFmt.format(++testNum)).isTrue()
                    assertThat(resp.local).describedAs(testFmt.format(++testNum)).isTrue()
                    assertThat(resp.role).describedAs(testFmt.format(++testNum)).isEqualTo("Administrator")
                }
            rioClient
                .updateUserPassword(
                    userUuid,
                    UserUpdatePasswordRequest("new-password"),
                ).let { resp ->
                    assertThat(resp.statusCode).describedAs(testFmt.format(++testNum)).isEqualTo(HttpStatusCode.OK)
                    assertThat(resp.username).describedAs(testFmt.format(++testNum)).isEqualTo(username)
                    assertThat(resp.active).describedAs(testFmt.format(++testNum)).isTrue()
                    assertThat(resp.local).describedAs(testFmt.format(++testNum)).isTrue()
                    assertThat(resp.role).describedAs(testFmt.format(++testNum)).isEqualTo("Administrator")
                }
            rioClient.deleteUserLogin(userUuid).let { resp ->
                assertThat(resp.statusCode).describedAs(testFmt.format(++testNum)).isEqualTo(HttpStatusCode.NoContent)
            }
            assertThat(rioClient.headUserLogin(userUuid)).describedAs(testFmt.format(++testNum)).isFalse()
        }

    @Test
    fun configLdapTest() =
        blockingTest {
            val orig =
                try {
                    rioClient.getActiveDirectoryConfig()
                } catch (_: Throwable) {
                    null
                }
            listOf(true, false).forEach { tls ->
                listOf(true, false).forEach { allow ->
                    listOf("Operator", "Administrator").forEach { role ->
                        val port = if (tls) 636 else 389
                        val expectedRole = if (allow) role else "Operator"
                        rioClient
                            .setActiveDirectoryConfig(
                                ActiveDirectoryRequest(ldapDomain, ldapHost, port, tls, allow, role),
                            ).let { resp ->
                                assertThat(resp.statusCode).isEqualTo(HttpStatusCode.OK)
                                assertThat(resp.domain).isEqualTo(ldapDomain)
                                assertThat(resp.ldapServer).isEqualTo(ldapHost)
                                assertThat(resp.port).isEqualTo(port)
                                assertThat(resp.tls).isEqualTo(tls)
                                assertThat(resp.allowAny).isEqualTo(allow)
                                assertThat(resp.defaultRole).isEqualTo(expectedRole)
                            }

                        rioClient.getActiveDirectoryConfig().let { resp ->
                            assertThat(resp.statusCode).isEqualTo(HttpStatusCode.OK)
                            assertThat(resp.domain).isEqualTo(ldapDomain)
                            assertThat(resp.ldapServer).isEqualTo(ldapHost)
                            assertThat(resp.port).isEqualTo(port)
                            assertThat(resp.tls).isEqualTo(tls)
                            assertThat(resp.allowAny).isEqualTo(allow)
                            assertThat(resp.defaultRole).isEqualTo(expectedRole)
                        }

                        rioClient.deleteActiveDirectoryConfig().let { resp ->
                            assertThat(resp.statusCode).isEqualTo(HttpStatusCode.NoContent)
                        }

                        assertThrows<RioHttpException> {
                            rioClient.getActiveDirectoryConfig()
                        }
                    }
                }
            }
            orig?.let {
                if (it.statusCode == HttpStatusCode.OK) {
                    rioClient
                        .setActiveDirectoryConfig(
                            ActiveDirectoryRequest(it.domain, it.ldapServer, it.port, it.tls, it.allowAny, it.defaultRole),
                        ).let { resp ->
                            assertThat(resp.statusCode).isEqualTo(HttpStatusCode.OK)
                            assertThat(resp.domain).isEqualTo(it.domain)
                            assertThat(resp.ldapServer).isEqualTo(it.ldapServer)
                            assertThat(resp.port).isEqualTo(it.port)
                            assertThat(resp.tls).isEqualTo(it.tls)
                            assertThat(resp.allowAny).isEqualTo(it.allowAny)
                            assertThat(resp.defaultRole).isEqualTo(it.defaultRole)
                        }
                }
            }
        }

    @Test
    fun lifecyclePolicyTest(
        @TempDir tempDir: Path,
    ) = blockingTest {
        val uuid = UUID.randomUUID()
        val tempPath =
            tempDir
                .toFile()
                .absolutePath
                .toString()
                .split(File.separator)
                .joinToString("/")
        val brokerName = "lifecycle-broker-$uuid"
        val agentCount = 3
        val agentRange = (0..(agentCount - 1))

        val lifecycleIds: List<String> =
            agentRange.map {
                val deferDays = if (it == 0) -1 else it // need one lifecycle with deferDays = -1
                val updateDeferDays = if (it == 0) -1 else it * 11
                val deleteDays = if (it == 0) 0 else it * 12
                val updateDeleteDays = if (it == 0) 0 else it * 13
                val createResponse =
                    rioClient.createLifecycle(LifecycleRequest("create-$it-$uuid", deferDays, deleteDays)).also { resp ->
                        assertThat(resp.statusCode).isEqualTo(HttpStatusCode.Created)
                        assertThat(resp.name).isEqualTo("create-$it-$uuid")
                        assertThat(resp.deferDays).isEqualTo(deferDays)
                        assertThat(resp.deleteDays).isEqualTo(deleteDays)
                    }
                rioClient.getLifecycle(createResponse.uuid, true).let { resp ->
                    assertThat(resp.statusCode).isEqualTo(HttpStatusCode.OK)
                    assertThat(resp.name).isEqualTo("create-$it-$uuid")
                    assertThat(resp.deferDays).isEqualTo(deferDays)
                    assertThat(resp.deleteDays).isEqualTo(deleteDays)
                    assertThat(resp.brokersUsing).isNull()
                }
                assertThat(rioClient.headLifecycle(createResponse.uuid)).isTrue
                rioClient
                    .updateLifecycle(
                        createResponse.uuid,
                        LifecycleRequest("update-$it-$uuid", updateDeferDays, updateDeleteDays),
                    ).let { resp ->
                        assertThat(resp.statusCode).isEqualTo(HttpStatusCode.OK)
                        assertThat(resp.name).isEqualTo("update-$it-$uuid")
                        assertThat(resp.deferDays).isEqualTo(updateDeferDays)
                        assertThat(resp.deleteDays).isEqualTo(updateDeleteDays)
                    }
                createResponse.uuid
            }

        val createPolicies =
            agentRange.map { idx ->
                AgentLifecyclePolicyRequest(
                    "agent-$uuid-$idx",
                    lifecycleIds[idx],
                    idx + 1,
                    (0..6).map {
                        DailyPeakHoursRequest(60 * (idx + 1), 60 * (idx + 1) + it)
                    },
                )
            }
        val updatePolicies =
            agentRange.map { idx ->
                AgentLifecyclePolicyRequest(
                    "agent-$uuid-$idx",
                    lifecycleIds[idx],
                    agentCount - idx,
                    (0..6).map {
                        DailyPeakHoursRequest(60 * (idx + 2), 60 * (idx + 2) + it)
                    },
                )
            }
        val agentConfig =
            NasAgentConfig(
                URI("file:///$tempPath").toString(),
            )
        rioClient.createBroker(
            BrokerCreateRequest(
                brokerName,
                createPolicies[0].agentName,
                agentConfig.toConfigMap(),
                "nas_agent",
            ),
        )
        try {
            val subAgentStartIdx = agentRange.first() + 1
            (subAgentStartIdx..agentRange.last).forEach { idx ->
                rioClient.createAgent(
                    brokerName,
                    AgentCreateRequest(
                        createPolicies[idx].agentName,
                        "nas_agent",
                        agentConfig.toConfigMap(),
                        false,
                    ),
                )
            }

            assertThat(rioClient.headLifecyclePolicy(brokerName)).isFalse

            rioClient.saveLifecyclePolicy(brokerName, SaveLifecyclePolicyRequest(createPolicies)).let { resp ->
                assertThat(resp.statusCode).isEqualTo(HttpStatusCode.Created)
                assertThat(resp.agentLifecycles).hasSize(agentCount)
                compareLifecyclePolicies(createPolicies, resp.agentLifecycles)
            }

            assertThat(rioClient.headLifecyclePolicy(brokerName)).isTrue

            rioClient.getLifecyclePolicy(brokerName).let { resp ->
                assertThat(resp.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(resp.agentLifecycles).hasSize(agentCount)
                compareLifecyclePolicies(createPolicies, resp.agentLifecycles)
            }

            agentRange.forEach { idx ->
                rioClient.getLifecycle(lifecycleIds[idx], true).let { resp ->
                    assertThat(resp.statusCode).isEqualTo(HttpStatusCode.OK)
                    assertThat(resp.brokersUsing).hasSize(1)
                    assertThat(resp.brokersUsing?.get(0)).isNotNull.isEqualTo(brokerName)
                }
            }

            rioClient.saveLifecyclePolicy(brokerName, SaveLifecyclePolicyRequest(updatePolicies)).let { resp ->
                assertThat(resp.statusCode).isEqualTo(HttpStatusCode.Created)
                assertThat(resp.agentLifecycles).hasSize(agentCount)
                compareLifecyclePolicies(updatePolicies, resp.agentLifecycles)
            }

            rioClient.getLifecyclePolicy(brokerName).let { resp ->
                assertThat(resp.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(resp.agentLifecycles).hasSize(agentCount)
                compareLifecyclePolicies(updatePolicies, resp.agentLifecycles)
            }
            rioClient.deleteLifecyclePolicy(brokerName).let { resp ->
                assertThat(resp.statusCode).isEqualTo(HttpStatusCode.NoContent)
            }

            assertThat(rioClient.headLifecyclePolicy(brokerName)).isFalse

            lifecycleIds.forEach { lifecycleId ->
                rioClient.deleteLifecycle(lifecycleId).let { resp ->
                    assertThat(resp.statusCode).isEqualTo(HttpStatusCode.NoContent)
                }
                assertThat(rioClient.headLifecycle(lifecycleId)).isFalse
            }
        } finally {
            if (rioClient.headLifecyclePolicy(brokerName)) {
                rioClient.deleteLifecyclePolicy(brokerName)
            }
            removeBroker(brokerName)
        }
    }

    private fun compareLifecyclePolicies(
        reqList: List<AgentLifecyclePolicyRequest>,
        respList: List<AgentLifecyclePolicyResponse>,
    ) {
        reqList.forEach { req ->
            respList.firstOrNull { it.agentName == req.agentName }?.let { resp ->
                assertThat(resp.lifecycleUuid).isEqualTo(req.lifecycleUuid)
                assertThat(resp.restorePriority).isEqualTo(req.restorePriority)
                assertThat(resp.peakHours).hasSize(7)
                (0..6).forEach { peakIdx ->
                    assertThat(resp.peakHours?.get(peakIdx)?.startMinuteOfDay).isEqualTo(req.peakHours[peakIdx].startMinuteOfDay)
                    assertThat(resp.peakHours?.get(peakIdx)?.endMinuteOfDay).isEqualTo(req.peakHours[peakIdx].endMinuteOfDay)
                }
            }
        }
    }

    @Test
    fun lifecyclePolicyErrorTest(
        @TempDir tempDir: Path,
    ) = blockingTest {
        val uuid = UUID.randomUUID()
        val tempPath =
            tempDir
                .toFile()
                .absolutePath
                .toString()
                .split(File.separator)
                .joinToString("/")
        val brokerName = "lifecycle-broker-$uuid"
        val agentConfig =
            NasAgentConfig(
                URI("file:///$tempPath").toString(),
            )
        rioClient.createBroker(
            BrokerCreateRequest(
                brokerName,
                "nas-agent-$uuid",
                agentConfig.toConfigMap(),
                "nas_agent",
            ),
        )

        val lifecycleId =
            rioClient.createLifecycle(LifecycleRequest("test-$uuid", -1, 0)).let { resp ->
                assertThat(resp.statusCode).isEqualTo(HttpStatusCode.Created)
                resp.uuid
            }

        val goodRequest =
            AgentLifecyclePolicyRequest(
                "nas-agent-$uuid",
                lifecycleId,
                1,
                (0..6).map { DailyPeakHoursRequest(600, 700) },
            )

        assertThat(rioClient.headLifecyclePolicy(brokerName)).isFalse

        assertThrows<RioHttpException> {
            rioClient.getLifecyclePolicy(brokerName)
        }.let { ex ->
            assertThat(ex.statusCode).isEqualTo(HttpStatusCode.NotFound.value)
            assertThat(ex.errorMessage().message).isEqualTo("Resource of type LIFE_CYCLE_POLICY and name $brokerName does not exist")
        }

        assertThrows<RioHttpException> {
            rioClient.saveLifecyclePolicy("bad-broker-$uuid", SaveLifecyclePolicyRequest(listOf(goodRequest)))
        }.let { ex ->
            assertThat(ex.statusCode).isEqualTo(HttpStatusCode.NotFound.value)
            assertThat(ex.errorMessage().message).isEqualTo("Resource of type BROKER and name bad-broker-$uuid does not exist")
        }

        assertThrows<RioHttpException> {
            rioClient.saveLifecyclePolicy(brokerName, SaveLifecyclePolicyRequest(listOf(goodRequest.copy(agentName = "bad-agent-$uuid"))))
        }.let { ex ->
            assertThat(ex.statusCode).isEqualTo(HttpStatusCode.NotFound.value)
            assertThat(ex.errorMessage().message).isEqualTo("Resource of type AGENT and name bad-agent-$uuid does not exist")
        }

        val badUuid = UUID.randomUUID().toString()
        assertThrows<RioHttpException> {
            rioClient.saveLifecyclePolicy(brokerName, SaveLifecyclePolicyRequest(listOf(goodRequest.copy(lifecycleUuid = badUuid))))
        }.let { ex ->
            assertThat(ex.statusCode).isEqualTo(HttpStatusCode.NotFound.value)
            assertThat(ex.errorMessage().message).isEqualTo("Data of type LIFE_CYCLE and UUID $badUuid does not exist")
        }
    }

    // TODO fun lifecycleTest() = blockingTest {}
    // TODO fun lifecycleErrorTest() = blockingTest {}

    @Test
    fun createUpdateDeleteRioGroup() =
        blockingTest {
            val description = "An informative description"
            val description2 = "An even better description"
            val groupName = "DELETE ME ${Instant.now().toEpochMilli()}"
            val result = rioClient.createRioGroup(CreateRioGroupRequest(emptyList(), description, groupName, emptyList()))
            assertThat(rioClient.headRioGroup(UUID.fromString(result.rioGroupUuid))).isTrue()
            assertThat(result.rioGroupDescription).isEqualTo(description)
            assertThat(result.rioGroupName).isEqualTo(groupName)
            val updateResponse =
                rioClient.updateRioGroup(
                    UUID.fromString(result.rioGroupUuid),
                    CreateRioGroupRequest(emptyList(), description2, groupName, emptyList()),
                )
            val secondResult = rioClient.getRioGroup(UUID.fromString(updateResponse.rioGroupUuid))
            assertThat(secondResult.rioGroupDescription).isEqualTo(description2)
            rioClient.deleteRioGroup(UUID.fromString(result.rioGroupUuid))
            assertThat(rioClient.headRioGroup(UUID.fromString(result.rioGroupUuid))).isFalse()
        }

    @Test
    fun rioUserGroupsTest() =
        blockingTest {
            rioClient
                .listUserLogins()
                .users
                .forEach {
                    val results = rioClient.listGroupsForUiser(it.userUuid)
                    assertThat(results.userUuid).isEqualTo(results.userUuid)
                }
        }

    @Test
    fun rioGroupsListTest() =
        blockingTest {
            // The everyone group should exist
            val page = 0L
            val perPage = 100L
            val response = rioClient.getRioGroups(page, perPage)
            assertThat(response.rioGroups).isNotEmpty()
            assertThat(response.rioGroups.firstOrNull { it.rioGroupName == "Everyone" }).isNotNull()
        }

    @Test
    fun updateUserRioGroupsTest() =
        blockingTest {
            // Create a temporary user
            val username = "user-" + UUID.randomUUID()
            val password = "wordpass"
            val created =
                rioClient.createUserLogin(
                    UserCreateRequest(username, username, password, active = false, local = true, role = "Operator"),
                )
            val userUuid = created.userUuid

            try {
                val groups = rioClient.getRioGroups(0, 100)
                assertThat(groups.rioGroups).isNotEmpty()
                val targetGroupUuid = groups.rioGroups.first().rioGroupUuid

                val updateResp = rioClient.updateGroupsForUser(userUuid, UpdateUserRioGroupRequest(listOf(targetGroupUuid)))
                assertThat(updateResp.statusCode).isEqualTo(HttpStatusCode.Created)
                assertThat(updateResp.userUuid).isEqualTo(userUuid.toString())
                assertThat(updateResp.rioGroupUuids).contains(targetGroupUuid)

                val verify = rioClient.listGroupsForUiser(userUuid)
                assertThat(verify.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(verify.userUuid).isEqualTo(userUuid.toString())
                assertThat(verify.rioGroupUuids).contains(targetGroupUuid)

                val clearResp = rioClient.updateGroupsForUser(userUuid, UpdateUserRioGroupRequest(emptyList()))
                assertThat(clearResp.statusCode).isEqualTo(HttpStatusCode.Created)
                assertThat(clearResp.userUuid).isEqualTo(userUuid.toString())
                assertThat(clearResp.rioGroupUuids).isEmpty()

                val verifyCleared = rioClient.listGroupsForUiser(userUuid)
                assertThat(verifyCleared.statusCode).isEqualTo(HttpStatusCode.OK)
                assertThat(verifyCleared.rioGroupUuids).isEmpty()
            } finally {
                rioClient.deleteUserLogin(userUuid)
            }
        }

    @Test
    fun systemAccessCacheClearTest() =
        blockingTest {
            // Ensure a broker exists for clearing broker access cache
            ensureBrokerExists()
            run {
                val resp = rioClient.clearBrokerAccessCache(testBroker)
                assertThat(resp.statusCode).isIn(HttpStatusCode.OK, HttpStatusCode.NoContent)
            }

            // Create a temporary Rio Group and clear its access cache
            val groupName = "rioclient-tmp-group-" + UUID.randomUUID()
            val createGroup = rioClient.createRioGroup(CreateRioGroupRequest(emptyList(), "temp group", groupName, emptyList()))
            try {
                val resp = rioClient.clearRioGroupAccessCache(UUID.fromString(createGroup.rioGroupUuid))
                assertThat(resp.statusCode).isIn(HttpStatusCode.OK, HttpStatusCode.NoContent)
            } finally {
                rioClient.deleteRioGroup(UUID.fromString(createGroup.rioGroupUuid))
            }

            // Create a temporary user and clear its access cache
            val username = "user-" + UUID.randomUUID()
            val password = "wordpass"
            val created =
                rioClient.createUserLogin(
                    UserCreateRequest(username, username, password, active = false, local = true, role = "Operator"),
                )
            try {
                val resp = rioClient.clearRioUserAccessCache(created.userUuid)
                assertThat(resp.statusCode).isIn(HttpStatusCode.OK, HttpStatusCode.NoContent)
            } finally {
                rioClient.deleteUserLogin(created.userUuid)
            }
        }

    private suspend fun ensureBrokerExists() {
        if (!rioClient.headBroker(testBroker)) {
            val agentConfig = BpAgentConfig(brokerBucket, spectraDeviceCreateRequest.name, spectraDeviceCreateRequest.username)
            val createRequest = BrokerCreateRequest(testBroker, testAgent, agentConfig.toConfigMap())
            val createResponse = rioClient.createBroker(createRequest)
            assertThat(createResponse.statusCode).isEqualTo(HttpStatusCode.Created)
        }
    }

    private suspend fun removeBroker(broker: String) {
        if (rioClient.headBroker(broker)) {
            var retry = 10
            do {
                try {
                    val deleteBrokerResponse = rioClient.deleteBroker(broker, true)
                    assertThat(deleteBrokerResponse.statusCode).isEqualTo(HttpStatusCode.NoContent)
                    retry = 0
                } catch (e: RioHttpException) {
                    if (e.message?.contains("running jobs") == true) {
                        delay(1000)
                    }
                }
            } while (--retry > 0)
        }
    }

    private suspend fun removeAgent(
        broker: String,
        agent: String,
    ) {
        if (rioClient.headAgent(broker, agent)) {
            var retry = 10
            do {
                try {
                    val deleteAgentResponse = rioClient.deleteAgent(testBroker, agent, true)
                    assertThat(deleteAgentResponse.statusCode).isEqualTo(HttpStatusCode.NoContent)
                    retry = 0
                } catch (e: RioHttpException) {
                    if (e.message?.contains("running jobs") == true) {
                        println("DWL: retry=$retry")
                        delay(1000)
                    }
                }
            } while (--retry > 0)
        }
    }

    private fun uuid(): String = UUID.randomUUID().toString()

    fun blockingTest(test: suspend () -> Unit) {
        runBlocking { test() }
    }

    private fun assertRioResourceError(
        ex: RioHttpException,
        expected: RioResourceErrorMessage,
    ) {
        assertThat(ex).isNotNull
        assertThat(ex.errorMessage()).isNotNull.isInstanceOf(RioResourceErrorMessage::class.java)

        val rioResourceErrorMessage = ex.errorMessage() as RioResourceErrorMessage
        assertThat(rioResourceErrorMessage.message).isEqualTo(expected.message)
        assertThat(rioResourceErrorMessage.statusCode).isEqualTo(expected.statusCode)
        assertThat(rioResourceErrorMessage.resourceName).isEqualTo(expected.resourceName)
        assertThat(rioResourceErrorMessage.resourceType).isEqualTo(expected.resourceType)
    }

    private fun assertRioValidationError(
        ex: RioHttpException,
        expected: List<RioValidationMessage>,
        testNum: Int,
    ) {
        val testFmt = "assertRioValidationError-%c-%d"
        assertThat(ex).describedAs(testFmt.format('a', testNum)).isNotNull
        assertThat(
            ex.errorMessage(),
        ).describedAs(testFmt.format('b', testNum)).isNotNull.isInstanceOf(RioValidationErrorMessage::class.java)

        val rioValidationErrorMessage = ex.errorMessage() as RioValidationErrorMessage
        assertThat(rioValidationErrorMessage.message).describedAs(testFmt.format('c', testNum)).isEqualTo("Validation Failed")
        assertThat(rioValidationErrorMessage.statusCode).describedAs(testFmt.format('d', testNum)).isEqualTo(422)
        assertThat(rioValidationErrorMessage.errors)
            .describedAs(testFmt.format('e', testNum))
            .hasSize(expected.size)
            .containsExactlyInAnyOrderElementsOf(expected)
    }
}

private fun getenvValue(
    key: String,
    default: String,
): String = System.getenv(key) ?: default

private fun <T> assertRioListResponse(
    desc: String,
    assertCount: Long,
    fn: suspend (pageNumber: Long, perPage: Long) -> RioListResponse<T>,
) {
    val firstPage = runBlocking { fn(0L, 3L) }
    assertThat(firstPage.page().totalItems).describedAs(desc).isEqualTo(assertCount)
    var count = firstPage.results().size
    (1L until firstPage.page().totalPages).forEach { num ->
        val nextPage = runBlocking { fn(num, 3L) }
        assertThat(nextPage.page().totalItems).describedAs(desc).isEqualTo(assertCount)
        count += nextPage.results().size
    }
    assertThat(count.toLong()).describedAs(desc).isEqualTo(assertCount)
}
