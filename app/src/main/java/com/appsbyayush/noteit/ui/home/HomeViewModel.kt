package com.appsbyayush.noteit.ui.home

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.http.NetworkException
import android.util.Log
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appsbyayush.noteit.R
import com.appsbyayush.noteit.models.AppSettings
import com.appsbyayush.noteit.models.Note
import com.appsbyayush.noteit.models.SortItem
import com.appsbyayush.noteit.repo.NoteRepository
import com.appsbyayush.noteit.utils.NoInternetException
import com.appsbyayush.noteit.utils.Resource
import com.appsbyayush.noteit.utils.getNetworkStatus
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val app: Application
): ViewModel() {

    companion object {
        private const val TAG = "HomeViewModelyy"
    }

    private val _eventStateFlow = MutableStateFlow<Event>(Event.Idle)
    val events = _eventStateFlow.asStateFlow()

    var notesFlow: StateFlow<Resource<List<Note>>> = MutableStateFlow(Resource.Loading())
    var loggedInUser = repository.getAuthenticatedUser()
    var signInClient: SignInClient? = null

    private val credentialManager = CredentialManager.create(app)

    private val _appSettingsStateFlow = MutableStateFlow(AppSettings())
    val appSettings = _appSettingsStateFlow.asStateFlow()

    private val _searchQueryFlow = MutableStateFlow("")
    val searchQuery = _searchQueryFlow.asStateFlow()

    init {
        Timber.tag(TAG).d("User: ${loggedInUser?.email}: ")
    }

    fun onFragmentStarted() {
       notesFlow = _appSettingsStateFlow.combine(_searchQueryFlow) { settings, searchQuery ->
           Pair(settings, searchQuery)
       }.flatMapLatest {
           val settings = it.first
           val searchQuery = it.second

           repository.getAllNotes(settings.currentSort, searchQuery)
       }.stateIn(viewModelScope, SharingStarted.Lazily, Resource.Loading())

        _appSettingsStateFlow.value = repository.getAppSettings()

        loggedInUser = repository.getAuthenticatedUser()
    }

    fun updateCurrentSort(sortItem: SortItem) {
        _appSettingsStateFlow.update {
            val updatedAppSettings = it.copy(currentSort = sortItem)
            repository.saveAppSettings(updatedAppSettings)
            updatedAppSettings
        }
    }

    fun updateSearchQuery(searchQuery: String) {
        _searchQueryFlow.value = searchQuery
    }

    fun trashNotes(noteList: List<Note>) = viewModelScope.launch {
        val trashedNotes = noteList.map {
            it.isDeleted = true
            it
        }

        repository.insertNoteListLocally(trashedNotes)
        sendEvent(Event.TrashNotesSuccess)
    }

    fun loginUserWithGoogle() = viewModelScope.launch {
        Timber.tag(TAG).d("loginUserWithGoogle: Called")
        try {
            sendEvent(Event.Loading)
            if(getNetworkStatus(app) == 0) {
                throw NoInternetException()
            }

            val googleIdTokenRequestOptions = GetGoogleIdOption.Builder()
                .setServerClientId(app.applicationContext.getString(R.string.default_web_client_id))
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(true)
                .build()

            val signInRequest = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdTokenRequestOptions)
                .build()

            sendEvent(Event.BeginOneTapSignInProcess(signInRequest))

        } catch(e: Exception) {
            Timber.tag(TAG).d("loginUserWithGoogle: ${e.message}")
            if(e is ApiException || e is NoInternetException) {
                sendEvent(Event.BeginOneTapSignInFailure(e))
            }
        }
    }

    fun onOneTapSignInResultRetrieved(credential: Credential) = viewModelScope.launch {
        Timber.d("onOneTapSignInResultRetrieved: Called")
        sendEvent(Event.Loading)

        if (credential is CustomCredential
            && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            signInUserWithCredentials(googleIdTokenCredential.idToken)
        } else {
            Timber.tag(TAG).w("Credential is not of type Google ID!")
            _eventStateFlow.emit(Event.ErrorOccurred(Exception("Some error occurred")))
        }
    }

    private suspend fun signInUserWithCredentials(idToken: String) {
        try {
            repository.firebaseSignInWithCredentials(idToken)
            loggedInUser = repository.getAuthenticatedUser()

            sendEvent(Event.SignInSuccess)
        } catch(e: Exception) {
            Timber.tag(TAG).d("signInUserWithCredentials: ${e.message}")
            sendEvent(Event.ErrorOccurred(e))
        }
    }

    fun showSignupMessage(): Boolean {
        val timeLapsedSinceSignupMessageShown = (System.currentTimeMillis()
                - _appSettingsStateFlow.value.signupPopupLastShownTimestamp)

        Timber.tag(TAG).d("showSignupMessage: timeLapsedSinceSignupMessageShown: $timeLapsedSinceSignupMessageShown")
        Timber.tag(TAG).d("App Settings: ${_appSettingsStateFlow.value}")

        return (timeLapsedSinceSignupMessageShown > TimeUnit.HOURS.toMillis(2)
                && loggedInUser == null)
    }

    fun updateSignupPopupLastShownTime() {
        _appSettingsStateFlow.update {
            val updatedAppSettings = it.copy(signupPopupLastShownTimestamp = System.currentTimeMillis())
            repository.saveAppSettings(updatedAppSettings)
            updatedAppSettings
        }
    }

    fun saveCurrentSyncProcessId(processId: String) {
        repository.saveCurrentSyncProcessId(processId)
    }

    fun getCurrentSyncProcessId(): String {
        return repository.getCurrentSyncProcessId()
    }

    fun getUpdatedAppSettings(): AppSettings {
        _appSettingsStateFlow.update {
            repository.getAppSettings()
        }

        return repository.getAppSettings()
    }

    private fun sendEvent(event: Event) = viewModelScope.launch {
        _eventStateFlow.emit(event)
    }

    fun onEventOccurred() = viewModelScope.launch {
        _eventStateFlow.emit(Event.Idle)
    }

    sealed class Event {
        data object SignInSuccess: Event()
        data class BeginOneTapSignInProcess(val signInRequest: GetCredentialRequest): Event()
        data class BeginOneTapSignInFailure(val exception: Exception): Event()
        data object TrashNotesSuccess: Event()
        class ErrorOccurred(val exception: Throwable): Event()
        data object Loading: Event()
        data object Idle : Event()
    }
}