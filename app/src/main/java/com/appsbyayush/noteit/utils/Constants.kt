package com.appsbyayush.noteit.utils

import com.appsbyayush.noteit.models.NoteColor
import com.appsbyayush.noteit.models.SortItem
import com.appsbyayush.noteit.utils.enums.SortType

object Constants {
    const val APP_SHARED_PREFS = "APP_SHARED_PREFS"

    const val PREFS_KEY_APP_SETTINGS = "PREFS_KEY_APP_SETTINGS"
    const val PREFS_KEY_CURRENT_SYNC_PROCESS_ID = "PREFS_KEY_CURRENT_SYNC_PROCESS_ID"

    const val COLLECTION_NOTES = "notes"
    const val COLLECTION_NOTE_MEDIA_ITEMS = "note_media_items"

    const val FIELD_USER_ID = "userId"
    const val FIELD_MODIFIED_AT = "modifiedAt"
    const val FIELD_DELETED = "deleted"

    const val DATE_FORMAT_1 = "dd/MMM"
    const val DATE_FORMAT_2 = "dd MMM hh:mm a"

    const val TIME_FORMAT_1 = "hh:mm a"

    const val DATE_TIME_FORMAT_1 = "yyyyMMddHHmmssSSS"

    const val DESCRIPTION_DEFAULT_MIN_HEIGHT = 1500

    const val NOTIFICATION_CHANNEL_LOW = "NOTIFICATION_CHANNEL_LOW"
    const val NOTIFICATION_CHANNEL_HIGH = "NOTIFICATION_CHANNEL_HIGH"

    const val WORK_RESULT = "WORK_RESULT"
    const val WORK_RESULT_SUCCESS = "WORK_RESULT_SUCCESS"
    const val WORK_RESULT_FAILURE = "WORK_RESULT_FAILURE"

    val SORT_LIST = listOf(
        SortItem("Sort by modified time", "modifiedAt", SortType.BY_MODIFIED_AT, "DESC"),
        SortItem("Sort by name", "title", SortType.BY_NAME_ASCENDING),
        SortItem("Sort by created time", "createdAt", SortType.BY_CREATED_AT, "DESC")
    )

    val colorList = listOf(
        NoteColor("#0882c8", "#9dd3f2"),
        NoteColor("#b300b3", "#ffccff"),
        NoteColor("#009933", "#adebad"),
        NoteColor("#f87122", "#ffd1b3"),
        NoteColor("#e6b800", "#fcf29d"),
        NoteColor("#f93b3b", "#ffcccc")
    )
}