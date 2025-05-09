package com.appsbyayush.noteit.models

import android.os.Parcelable
import com.appsbyayush.noteit.utils.enums.SortType
import kotlinx.parcelize.Parcelize

@Parcelize
data class SortItem(
    val name: String,
    val columnName: String,
    val sortType: SortType,
    val order: String = "ASC"
): Parcelable
