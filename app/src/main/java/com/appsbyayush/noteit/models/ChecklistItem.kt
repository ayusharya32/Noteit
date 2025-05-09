package com.appsbyayush.noteit.models

import android.os.Parcelable
import com.appsbyayush.noteit.utils.enums.NoteMediaType
import com.google.firebase.firestore.Exclude
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class ChecklistItem(
    var id: String = "",
    var content: String = "",
    var done: Boolean = false
): Parcelable
