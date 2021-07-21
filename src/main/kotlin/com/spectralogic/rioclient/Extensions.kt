/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

import com.google.common.net.UrlEscapers
import java.net.URLDecoder

fun String.urlEncode(): String {
    return UrlEscapers.urlFragmentEscaper().escape(this)
}

fun String.urlDecode(): String {
    return URLDecoder.decode(this, "utf-8")
}
