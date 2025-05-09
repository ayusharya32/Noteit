package com.appsbyayush.noteit.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appsbyayush.noteit.models.Note
import com.appsbyayush.noteit.repo.NoteRepository
import com.appsbyayush.noteit.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val repository: NoteRepository
): ViewModel() {

    private val _eventStateFlow = MutableStateFlow<Event>(Event.Idle)
    val events = _eventStateFlow.asStateFlow()

    var notesFlow: StateFlow<Resource<List<Note>>> = MutableStateFlow(Resource.Loading())

    fun onFragmentStarted() {
        notesFlow = repository.getAllTrashedNotes()
            .stateIn(viewModelScope, SharingStarted.Lazily, Resource.Loading())
    }

    fun restoreNote(note: Note) = viewModelScope.launch {
        note.isDeleted = false
        repository.insertNoteLocally(note)

        _eventStateFlow.emit(Event.NoteRestoredSuccess)
    }

    fun onEventOccurred() = viewModelScope.launch {
        _eventStateFlow.emit(Event.Idle)
    }

    sealed class Event {
        data object NoteRestoredSuccess: Event()
        class ErrorOccurred(val exception: Throwable): Event()
        data object Loading: Event()
        data object Idle : Event()
    }
}