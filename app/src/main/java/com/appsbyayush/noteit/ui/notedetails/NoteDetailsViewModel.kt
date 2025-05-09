package com.appsbyayush.noteit.ui.notedetails

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appsbyayush.noteit.models.Note
import com.appsbyayush.noteit.models.NoteMediaItem
import com.appsbyayush.noteit.repo.NoteRepository
import com.appsbyayush.noteit.utils.CommonMethods
import com.appsbyayush.noteit.utils.Constants
import com.appsbyayush.noteit.utils.enums.NoteMediaType
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NoteDetailsViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val app: Application
): ViewModel() {

    companion object {
        private const val TAG = "NoteDetailsViewModel"
    }

    var currentNote = Note()
    var isNewNote = true

    private val _currentNoteMediaItems = MutableStateFlow<List<NoteMediaItem>>(listOf())
    val currentNoteMediaItems = _currentNoteMediaItems.asStateFlow()

    private val _eventStateFlow = MutableStateFlow<Event>(Event.Idle)
    val events = _eventStateFlow.asStateFlow()

    fun saveNote(note: Note) = viewModelScope.launch {
        repository.insertNoteLocally(note)
        repository.insertMediaItemsLocally(currentNoteMediaItems.value)

        _eventStateFlow.emit(Event.NoteSaveSuccess)
    }

    fun trashCurrentNote() = viewModelScope.launch {
        currentNote.isDeleted = true
        repository.insertNoteLocally(currentNote)

        _eventStateFlow.emit(Event.NoteTrashSuccess)
    }

    fun getAuthenticatedUser(): FirebaseUser? {
        return repository.getAuthenticatedUser()
    }

    fun onEventOccurred() = viewModelScope.launch {
        _eventStateFlow.emit(Event.Idle)
    }

    fun getNoteMediaItems() = viewModelScope.launch {
        _currentNoteMediaItems.emit(repository.getNoteMediaItems(currentNote))
    }

    fun addNewMediaItems(uriList: List<Uri>) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                uriList.forEach { uri ->
                    val compressedImageUri = CommonMethods.getCompressedImageUri(
                        app.applicationContext, uri)

                    val noteMediaItem = NoteMediaItem(
                        id = UUID.randomUUID().toString(),
                        noteId = currentNote.id,
                        userId = getAuthenticatedUser()?.uid,
                        localUriString = compressedImageUri.toString(),
                        itemUrl = null,
                        type = NoteMediaType.TYPE_IMAGE,
                        isFileUploaded = false,
                        isDeleted = false,
                        isSynced = false
                    )

                    _currentNoteMediaItems.value += listOf(noteMediaItem)
                }

            } catch(e: Exception) {
                Timber.tag(TAG).d("getMediaItemListFromImageUriList: $e")
            }
        }

    fun deleteMediaItem(mediaItem: NoteMediaItem) = viewModelScope.launch {
        val savedItem = repository.getNoteMediaItemById(mediaItem.id)

        if(savedItem == null) {
            _currentNoteMediaItems.update {
                val updatedList = it.toMutableList()
                updatedList.remove(mediaItem)

                updatedList
            }
        } else {
            _currentNoteMediaItems.update {
                val updatedList = it.toMutableList()
                updatedList.map { item ->
                    if(item.id == mediaItem.id) {
                        item.copy(
                            isDeleted = true,
                            isSynced = false
                        )

                    } else {
                        item
                    }
                }
            }
        }
    }

    fun getFormattedNoteModifiedDate(): String {
        return when {
            CommonMethods.isDateOfToday(currentNote.modifiedAt) -> {
                "Today at ${CommonMethods.getFormattedDateTime(currentNote.modifiedAt,
                    Constants.TIME_FORMAT_1)}"
            }
            CommonMethods.isDateOfYesterday(currentNote.modifiedAt) -> {
                "Yesterday at ${CommonMethods.getFormattedDateTime(currentNote.modifiedAt,
                    Constants.TIME_FORMAT_1)}"
            }
            else -> CommonMethods.getFormattedDateTime(currentNote.modifiedAt,
                Constants.DATE_FORMAT_2)
        }
    }

    sealed class Event {
        data object NoteSaveSuccess : Event()
        data object NoteTrashSuccess : Event()
        class ErrorOccurred(val exception: Throwable): Event()
        data object Idle : Event()
    }
}