/**
 * ***************************************************************************
 *    Copyright 2014-2024 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

data class ActiveDirectoryRequest(
    val domain: String,
    val ldapServer: String,
    val port: Int,
    val tls: Boolean,
    val allowAny: Boolean,
    val defaultRole: String
) : RioRequest

data class ActiveDirectoryResponse(
    val domain: String,
    val ldapServer: String,
    val port: Int,
    val tls: Boolean,
    val allowAny: Boolean,
    val defaultRole: String
) : RioResponse()
