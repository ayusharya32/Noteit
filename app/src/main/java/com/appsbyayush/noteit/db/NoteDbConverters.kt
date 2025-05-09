package com.appsbyayush.noteit.db

import androidx.room.TypeConverter
import com.appsbyayush.noteit.models.ChecklistItem
import com.appsbyayush.noteit.models.NoteColor
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.util.*

class NoteDbConverters {
    @TypeConverter
    fun stringListToJson(stringList: List<String>?): String? {
        return if (stringList == null) null else Gson().toJson(stringList)
    }

    @TypeConverter
    fun jsonToStringList(listJson: String?): List<String>? {
        val gson = Gson()
        val type: Type = object : TypeToken<List<String>>() {}.type
        return if (listJson == null) null else gson.fromJson<List<String>>(listJson, type)
    }

    @TypeConverter
    fun checklistToJson(checklist: List<ChecklistItem>?): String? {
        return if (checklist == null) null else Gson().toJson(checklist)
    }

    @TypeConverter
    fun jsonToChecklist(listJson: String?): List<ChecklistItem>? {
        val gson = Gson()
        val type: Type = object : TypeToken<List<ChecklistItem>>() {}.type
        return if (listJson == null) null else gson.fromJson<List<ChecklistItem>>(listJson, type)
    }

    @TypeConverter
    fun mutableChecklistToJson(checklist: MutableList<ChecklistItem>?): String? {
        return if (checklist == null) null else Gson().toJson(checklist)
    }

    @TypeConverter
    fun jsonToMutableChecklist(listJson: String?): MutableList<ChecklistItem>? {
        val gson = Gson()
        val type: Type = object : TypeToken<MutableList<ChecklistItem>>() {}.type
        return if (listJson == null) null else gson.fromJson<MutableList<ChecklistItem>>(listJson, type)
    }

    @TypeConverter
    fun fromTimestampToDate(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun noteColorToJson(noteColor: NoteColor): String {
        return Gson().toJson(noteColor)
    }

    @TypeConverter
    fun jsonToNoteColor(jsonString: String): NoteColor {
        return Gson().fromJson(jsonString, NoteColor::class.java)
    }
}