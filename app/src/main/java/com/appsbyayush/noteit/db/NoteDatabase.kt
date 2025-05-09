package com.appsbyayush.noteit.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.appsbyayush.noteit.models.Note
import com.appsbyayush.noteit.models.NoteMediaItem

@Database(
    entities = [Note::class, NoteMediaItem::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(NoteDbConverters::class)
abstract class NoteDatabase: RoomDatabase() {
    abstract fun getNoteDao(): NoteDao
}