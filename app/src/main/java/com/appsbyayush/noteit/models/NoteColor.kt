package com.appsbyayush.noteit.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NoteColor(
    val darkHexCode: String = "#000000",
    val lightHexCode: String = "#ffffff"
): Parcelable