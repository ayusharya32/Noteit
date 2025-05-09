package com.appsbyayush.noteit.db

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.appsbyayush.noteit.models.Note
import com.appsbyayush.noteit.models.NoteMediaItem
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteList(notes: List<Note>)

    @Delete
    suspend fun deleteNote(note: Note)

    @RawQuery(observedEntities = [Note::class])
    fun getAllNotes(query: SupportSQLiteQuery): Flow<List<Note>>

    @Query("SELECT * FROM notes_table WHERE isDeleted = 1 ORDER BY modifiedAt DESC")
    fun getAllTrashedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes_table WHERE isSynced = 0 ORDER BY modifiedAt")
    suspend fun getAllUnsyncedNotes(): List<Note>

    @Query("SELECT * FROM notes_table WHERE id = :noteId")
    suspend fun getNoteById(noteId: String): Note?

    @Query("DELETE FROM notes_table")
    suspend fun clearNotesTable()

    @Query("SELECT * FROM notes_table WHERE isDeleted = 1 AND modifiedAt < :timestamp")
    suspend fun getTrashedNotesOlderThanTimestamp(timestamp: Long): List<Note>

    @Query("DELETE FROM notes_table WHERE isDeleted = 1 AND modifiedAt < :timestamp")
    suspend fun clearTrashedNotesOlderThanTimestamp(timestamp: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteMediaItems(mediaItems: List<NoteMediaItem>)

    @Query("SELECT * FROM note_media_table WHERE id = :itemId")
    suspend fun getNoteMediaItemById(itemId: String): NoteMediaItem?

    @Query("SELECT * FROM note_media_table WHERE noteId = :noteId AND isDeleted = 0 ORDER BY createdAt")
    suspend fun getNoteMediaItems(noteId: String): List<NoteMediaItem>

    @Query("SELECT * FROM note_media_table WHERE noteId IN (:noteIds) AND isDeleted = 0 ORDER BY createdAt")
    suspend fun getNoteMediaItemsOfMultipleNotes(noteIds: List<String>): List<NoteMediaItem>

    @Query("SELECT * FROM note_media_table WHERE isSynced = 0 ORDER BY createdAt")
    suspend fun getAllUnsyncedNoteMediaItems(): List<NoteMediaItem>

    @Query("DELETE FROM note_media_table")
    suspend fun clearNoteMediaItemsTable()

    @Query("SELECT * FROM note_media_table WHERE isDeleted = 1 AND createdAt < :timestamp")
    suspend fun getTrashedNoteMediaItemsOlderThanTimestamp(timestamp: Long): List<NoteMediaItem>

    @Query("DELETE FROM note_media_table WHERE isDeleted = 1 AND createdAt < :timestamp")
    suspend fun clearTrashedNoteMediaItemsOlderThanTimestamp(timestamp: Long)
}