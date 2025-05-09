package com.appsbyayush.noteit.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.appsbyayush.noteit.utils.enums.NoteMediaType
import com.google.firebase.firestore.Exclude
import kotlinx.parcelize.Parcelize
import java.util.*

@Entity(tableName = "note_media_table")
@Parcelize
data class NoteMediaItem(
    @PrimaryKey
    var id: String = "",

    var noteId: String = "",
    var userId: String? = "",

    var localUriString: String? = "",
    var itemUrl: String? = "",
    var type: NoteMediaType = NoteMediaType.TYPE_IMAGE,
    var createdAt: Date = Calendar.getInstance().time,

    var isFileUploaded: Boolean = false,
    var isDeleted: Boolean = false,

    @get:Exclude
    var isSynced: Boolean = false
): Parcelable
