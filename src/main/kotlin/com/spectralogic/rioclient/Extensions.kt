/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun String.urlEncode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.toString()).replace("+", "%20")

fun String.urlDecode(): String = URLDecoder.decode(this, "utf-8")
