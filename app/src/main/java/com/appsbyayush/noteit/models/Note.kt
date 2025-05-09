package com.appsbyayush.noteit.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.appsbyayush.noteit.utils.Constants
import com.appsbyayush.noteit.utils.enums.NoteType
import com.google.firebase.firestore.Exclude
import kotlinx.parcelize.Parcelize
import java.util.*

@Entity(tableName = "notes_table")
@Parcelize
data class Note(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),

    var userId: String? = "",

    var title: String = "",

    var description: String = "",
    var checklistItems: MutableList<ChecklistItem> = mutableListOf(),

    var noteType: NoteType = NoteType.TYPE_TEXT,
    var color: NoteColor = Constants.colorList[0],

    var createdAt: Date = Calendar.getInstance().time,
    var modifiedAt: Date = Calendar.getInstance().time,

    var isDeleted: Boolean = false,

    @get:Exclude
    var isSynced: Boolean = false,

    @Ignore
    @get:Exclude
    var isChecked: Boolean = false

): Parcelable