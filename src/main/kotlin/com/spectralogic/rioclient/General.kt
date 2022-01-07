/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

data class PageInfo(
    val number: Long,
    val pageSize: Long,
    val totalPages: Long,
    val totalItems: Long = 0L
)

interface PageData<T> {
    val objects: List<T>
    val page: PageInfo
}

class RioHttpException(t: Throwable) : RuntimeException(t.message, t)

// best effort to find relevant message in exception.  RioBroker message should be DefaultErrorMessage(message: String, statusCode: Int)
fun RioHttpException.parse(): String {
    val message = this.message ?: ""
    return when (val rioMessage = message.jsonValue("message") ?: "") {
        "Validation Failed" -> message.jsonValue("reason") ?: message.jsonValue("errorType")
        else -> rioMessage
    } ?: "Unknown"
}

private fun String.jsonValue(key: String): String? {
    val keyStr = "\"$key\":"
    if (this.contains(keyStr)) {
        val data = this.substringAfter(keyStr).trimStart()
        return if (data.startsWith("\"")) {
            data.substring(1).substringBefore("\"")
        } else {
            data.substringBefore(",").substringBefore("}").substringBefore("]")
        }
    }
    return null
}
