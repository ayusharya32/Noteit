package com.appsbyayush.noteit.repo

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import com.appsbyayush.noteit.db.NoteDatabase
import com.appsbyayush.noteit.models.AppSettings
import com.appsbyayush.noteit.models.Note
import com.appsbyayush.noteit.models.NoteMediaItem
import com.appsbyayush.noteit.models.SortItem
import com.appsbyayush.noteit.utils.CommonMethods
import com.appsbyayush.noteit.utils.Constants
import com.appsbyayush.noteit.utils.Resource
import com.appsbyayush.noteit.utils.getNetworkStatus
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    noteDatabase: NoteDatabase,
    private val appSharedPrefs: SharedPreferences,
    @ApplicationContext private val applicationContext: Context
) {
    companion object {
        private const val TAG = "NoteRepositoryyy"
    }

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val storage: FirebaseStorage = Firebase.storage

    private val noteDao = noteDatabase.getNoteDao()

    suspend fun insertNoteLocally(note: Note) {
        note.isSynced = false
        noteDao.insertNote(note)
    }

    suspend fun insertNoteListLocally(noteList: List<Note>) {
        noteDao.insertNoteList(noteList.map {
            it.isSynced = false
            it
        })
    }

    fun getAllNotes(sortItem: SortItem, searchQuery: String = ""): Flow<Resource<List<Note>>> {
        return flow {
            Log.d(TAG, "getAllNotes: Called")

            val searchQuerySql = if(searchQuery.isNotEmpty()) "title LIKE '%$searchQuery%' OR " +
                    "description LIKE '%$searchQuery%' AND " else ""

            val query = SimpleSQLiteQuery("SELECT * FROM notes_table WHERE $searchQuerySql" +
                    "isDeleted = 0 " +
                    "ORDER BY ${sortItem.columnName} ${sortItem.order}")

            val notes = noteDao.getAllNotes(query).map {
                Resource.Success(it)
            }

            Log.d(TAG, "getAllNotes: Called ${query.sql}")

            emitAll(notes)
        }
    }

    suspend fun clearNotesTable() {
        noteDao.clearNotesTable()
    }

    suspend fun clearNoteMediaItemsTable() {
        noteDao.clearNoteMediaItemsTable()
    }

    suspend fun clearAllTrashedNotesAndMediaItems() {
        val currentTimestamp = System.currentTimeMillis()

        val trashedNotes = noteDao.getTrashedNotesOlderThanTimestamp(currentTimestamp)
        val firebaseTrashedNotesDeleted = clearTrashedNotesFromFirebase(trashedNotes)

        if(firebaseTrashedNotesDeleted) {
            noteDao.clearTrashedNotesOlderThanTimestamp(currentTimestamp)
        }

        val trashedNoteMediaItems = noteDao
            .getTrashedNoteMediaItemsOlderThanTimestamp(currentTimestamp)
        val firebaseTrashedMediaItemsDeleted = clearTrashedNoteMediaItemsFromFirebase(trashedNoteMediaItems)

        if(firebaseTrashedMediaItemsDeleted) {
            noteDao.clearTrashedNoteMediaItemsOlderThanTimestamp(currentTimestamp)
        }
    }

    suspend fun clearTrashedNotesOlderThan30days() {
        val timestampBefore30Days = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)

        val trashedNotesBefore30Days = noteDao.getTrashedNotesOlderThanTimestamp(timestampBefore30Days)
        val firebaseNotesDeleted = clearTrashedNotesFromFirebase(trashedNotesBefore30Days)

        if(firebaseNotesDeleted) {
            noteDao.clearTrashedNotesOlderThanTimestamp(timestampBefore30Days)
        }
    }

    suspend fun clearTrashedNoteMediaItemsOlderThan30days() {
        val timestampBefore30Days = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)

        val trashedNoteMediaItemsBefore30Days = noteDao
            .getTrashedNoteMediaItemsOlderThanTimestamp(timestampBefore30Days)
        val firebaseMediaItemsDeleted = clearTrashedNoteMediaItemsFromFirebase(trashedNoteMediaItemsBefore30Days)


        if(firebaseMediaItemsDeleted) {
            noteDao.clearTrashedNoteMediaItemsOlderThanTimestamp(timestampBefore30Days)
        }
    }

    private suspend fun clearTrashedNotesFromFirebase(noteList: List<Note>): Boolean {
        return try {
            val batch = firestore.batch()

            noteList.forEach {
                val docRef = firestore.collection(Constants.COLLECTION_NOTES)
                    .document(it.id)

                batch.delete(docRef)
            }

            batch.commit().await()
            true
        } catch(e: Exception) {
            Log.d(TAG, "clearTrashedNotesFromFirebase: ${e.message}")
            false
        }
    }

    private suspend fun clearTrashedNoteMediaItemsFromFirebase(mediaItems: List<NoteMediaItem>): Boolean {
        return try {
            val batch = firestore.batch()

            mediaItems.forEach {
                val docRef = firestore.collection(Constants.COLLECTION_NOTE_MEDIA_ITEMS)
                    .document(it.id)

                batch.delete(docRef)
            }

            batch.commit().await()
            true
        } catch(e: Exception) {
            Log.d(TAG, "clearTrashedNoteMediaItemsFromFirebase: ${e.message}")
            false
        }
    }

    fun deleteAllLocalMediaFiles() {
        val mediaFolder = applicationContext.getExternalFilesDir("images")

        mediaFolder?.let {
            if(!it.exists() || it.listFiles().isNullOrEmpty()) {
                return
            }

            mediaFolder.deleteRecursively()
        }
    }

    fun getAllTrashedNotes(): Flow<Resource<List<Note>>> {
        return flow {
            val notes = noteDao.getAllTrashedNotes().map {
                Resource.Success(it)
            }

            emitAll(notes)
        }
    }

    fun getAuthenticatedUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun logoutUser() {
        return auth.signOut()
    }

    suspend fun firebaseSignInWithCredentials(idToken: String): AuthResult{
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        return auth.signInWithCredential(firebaseCredential).await()
    }

    suspend fun insertMediaItemsLocally(mediaItems: List<NoteMediaItem>) {
        noteDao.insertNoteMediaItems(mediaItems)
    }

    suspend fun getNoteMediaItems(note: Note): List<NoteMediaItem> {
        return noteDao.getNoteMediaItems(note.id)
    }

    suspend fun getNoteMediaItemById(itemId: String): NoteMediaItem? {
        return noteDao.getNoteMediaItemById(itemId)
    }

    suspend fun getNoteMediaItemsOfMultipleNotes(noteList: List<Note>): List<NoteMediaItem> {
        val noteIds = noteList.map { it.id }
        return noteDao.getNoteMediaItemsOfMultipleNotes(noteIds)
    }

    fun saveAppSettings(appSettings: AppSettings) {
        val appSettingsJson = Gson().toJson(appSettings)

        val editor = appSharedPrefs.edit()
        editor.putString(Constants.PREFS_KEY_APP_SETTINGS, appSettingsJson)
        editor.apply()
    }

    fun getAppSettings(): AppSettings {
        val appSettingsJson = appSharedPrefs.getString(Constants.PREFS_KEY_APP_SETTINGS, "")

        return if(appSettingsJson.isNullOrEmpty()) AppSettings()
            else Gson().fromJson(appSettingsJson, AppSettings::class.java)
    }

    fun saveCurrentSyncProcessId(processId: String) {
        val editor = appSharedPrefs.edit()
        editor.putString(Constants.PREFS_KEY_CURRENT_SYNC_PROCESS_ID, processId)
        editor.apply()
    }

    fun getCurrentSyncProcessId(): String {
        val currentSyncProcessId = appSharedPrefs.getString(
            Constants.PREFS_KEY_CURRENT_SYNC_PROCESS_ID, "")

        return currentSyncProcessId ?: ""
    }

    suspend fun syncNotes(): Boolean {
        if(!shouldSync()) {
            return false
        }

        val unsyncedNotes = noteDao.getAllUnsyncedNotes()
        Timber.tag(TAG).d("syncNotes -- unsyncedNotes: $unsyncedNotes")

        unsyncedNotes.forEach { note ->
            note.userId = auth.currentUser?.uid
            uploadNoteToFirebase(note)
        }

        val notes = getAllNotesFromFirebase().onEach { note ->
            note.isSynced = true
        }

        Timber.tag(TAG).d("syncNotes -- after sync: $notes")

        noteDao.insertNoteList(notes)
        return true
    }

    suspend fun syncNoteMediaItems() = coroutineScope {
        if(!shouldSync()) {
            return@coroutineScope false
        }

        val unsyncedMediaItems = noteDao.getAllUnsyncedNoteMediaItems()

        val operations = unsyncedMediaItems.map {
            launch(Dispatchers.IO) {
                uploadNoteMediaItem(mediaItem = it)
            }
        }

        operations.joinAll()

        val mediaItems = getAllNoteMediaItemsFromFirebase().onEach {
            it.isSynced = true
        }

        noteDao.insertNoteMediaItems(mediaItems)
        return@coroutineScope true
    }

    private suspend fun getAllNotesFromFirebase(): List<Note> {
        val notesSnapshot = firestore.collection(Constants.COLLECTION_NOTES)
            .whereEqualTo(Constants.FIELD_USER_ID, auth.currentUser?.uid)
            .get()
            .await()

        notesSnapshot?.let {
            return it.toObjects<Note>()
        }

        return listOf()
    }

    private suspend fun uploadNoteToFirebase(note: Note) {
        firestore.collection(Constants.COLLECTION_NOTES)
            .document(note.id)
            .set(note)
            .await()
    }

    private suspend fun uploadNoteMediaItem(mediaItem: NoteMediaItem) {
        try {
            if(!mediaItem.isFileUploaded) {
                uploadMediaItemFile(mediaItem)
            }

            mediaItem.userId = auth.currentUser?.uid

            firestore.collection(Constants.COLLECTION_NOTE_MEDIA_ITEMS)
                .document(mediaItem.id)
                .set(mediaItem)
                .await()

        } catch(e: Exception) {
            Timber.tag(TAG).d("uploadNoteMediaItem: $e")
        }
    }

    private suspend fun uploadMediaItemFile(mediaItem: NoteMediaItem) {
        val fileUri = Uri.parse(mediaItem.localUriString)
        val file = fileUri.path?.let { File(it) }

        val loggedInUser = auth.currentUser

        if(file == null || loggedInUser == null) {
            return
        }

        val storagePath = "${loggedInUser.uid}/media/${mediaItem.id}" +
                ".${CommonMethods.getExtensionFromFileName(file.name)}"
        val storageRef = storage.getReference(storagePath)

        storageRef.putFile(fileUri).await()

        val downloadUri = storageRef.downloadUrl.await()

        mediaItem.itemUrl = downloadUri.toString()
        mediaItem.isFileUploaded = true
    }

    private suspend fun getAllNoteMediaItemsFromFirebase(): List<NoteMediaItem> {
        val mediaItemsSnapshot = firestore.collection(Constants.COLLECTION_NOTE_MEDIA_ITEMS)
            .whereEqualTo(Constants.FIELD_USER_ID, auth.currentUser?.uid)
            .get()
            .await()

        mediaItemsSnapshot?.let {
            return it.toObjects<NoteMediaItem>()
        }

        return listOf()
    }

    private fun shouldSync(): Boolean {
        return when {
            auth.currentUser == null -> false
            getNetworkStatus(applicationContext) == 0 -> false
            else -> true
        }
    }
}