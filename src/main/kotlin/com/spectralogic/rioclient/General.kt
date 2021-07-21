/**
 * ***************************************************************************
 *    Copyright 2014-2021 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */
package com.spectralogic.rioclient

data class PageInfo(
    val number: Int,
    val pageSize: Int,
    val totalPages: Long,
    val totalItems: Long = 0L
)

interface PageData<T> {
    val objects: List<T>
    val page: PageInfo
}
