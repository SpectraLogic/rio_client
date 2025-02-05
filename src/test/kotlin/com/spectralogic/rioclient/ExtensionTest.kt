/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("test")
class ExtensionTest {
    @Test
    fun urlEncodeTest() {
        val plain = "abcdefghijklmnopqrstuvwxyz 01234567890 ~!@#$%^&()_+`-={}[]:;\"'<>,.?/'"
        val encoded = plain.urlEncode()
        val revert = encoded.urlDecode()
        val validCharSet = "abcdefghijklmnopqrstuvwxyz01234567890%._-ABCDEF"

        assertThat(plain).isEqualTo(revert)
        encoded.forEach {
            assertThat(validCharSet).contains(it.toString())
        }
    }
}
