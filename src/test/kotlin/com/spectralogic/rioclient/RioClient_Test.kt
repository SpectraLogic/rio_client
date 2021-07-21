/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.net.URL

@Tag("test")
class RioClient_Test {

    private companion object {
        private lateinit var rioClient: RioClient

        @JvmStatic
        @BeforeAll
        fun setup() {
            rioClient = RioClient(URL("https://localhost:5050"))
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
