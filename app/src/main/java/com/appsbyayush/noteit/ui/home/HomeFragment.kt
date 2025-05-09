package com.appsbyayush.noteit.ui.home

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.appsbyayush.noteit.R
import com.appsbyayush.noteit.adapters.NoteAdapter
import com.appsbyayush.noteit.baseactivity.HomeActivity
import com.appsbyayush.noteit.databinding.FragmentHomeBinding
import com.appsbyayush.noteit.models.Note
import com.appsbyayush.noteit.models.SortItem
import com.appsbyayush.noteit.ui.bottomsheets.AddNoteBottomSheetFragment
import com.appsbyayush.noteit.ui.bottomsheets.SettingsBottomSheetFragment
import com.appsbyayush.noteit.ui.bottomsheets.SignupBottomSheetFragment
import com.appsbyayush.noteit.ui.bottomsheets.SortBottomSheetFragment
import com.appsbyayush.noteit.ui.notedetails.NoteDetailsFragment
import com.appsbyayush.noteit.ui.settings.SettingsFragment
import com.appsbyayush.noteit.utils.Resource
import com.appsbyayush.noteit.utils.enums.NoteType
import com.appsbyayush.noteit.utils.getNetworkStatus
import com.appsbyayush.noteit.utils.launchOneTapSignInUI
import com.appsbyayush.noteit.worker.SyncWorker
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.TimeUnit

@SuppressLint("NotifyDataSetChanged", "SetTextI18n")
@AndroidEntryPoint
class HomeFragment: Fragment() {
    companion object {
        private const val TAG = "HomeFragmentyyy"
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var noteAdapter: NoteAdapter
    private var notesSyncing: Boolean = false
    private lateinit var credentialManager: CredentialManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.signInClient = Identity.getSignInClient(requireContext())

        setupNoteRecyclerView()
        setupButtons()

        setupUIEventCollector()
        setupNotesCollector()
        setupAppSettingsCollector()
        setupSyncObserver()

        viewModel.onFragmentStarted()
        addSyncNotesWorker()
    }

    private fun setupNoteRecyclerView() {
        noteAdapter = NoteAdapter(object: NoteAdapter.NoteItemClickEvent {
            override fun onItemClick(note: Note) {
                if(!noteAdapter.multiSelectEnabled) {
                    navigateToNoteDetailsFragment(note.noteType, note)
                    return
                }

                note.isChecked = !note.isChecked

                if(noteAdapter.getTotalSelectedItems().isEmpty()) {
                    noteAdapter.multiSelectEnabled = false
                    toggleNotesMultiSelectViews()
                }

                noteAdapter.notifyDataSetChanged()

                binding.txtSelectedNotes.text = "${noteAdapter.getTotalSelectedItems().size}" +
                        " Note(s) Selected"
            }

            override fun onItemLongClick(note: Note) {
                if(!noteAdapter.multiSelectEnabled && binding.toolbarSearchView.isIconified) {
                    note.isChecked = true

                    noteAdapter.multiSelectEnabled = true
                    noteAdapter.notifyDataSetChanged()

                    binding.txtSelectedNotes.text = "${noteAdapter.getTotalSelectedItems().size}" +
                            " Note(s) Selected"
                    toggleNotesMultiSelectViews()
                }
            }
        })

        binding.rvNotes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = noteAdapter
        }
    }

    private fun toggleNotesMultiSelectViews() {
        binding.apply {
            btnSort.isVisible = !noteAdapter.multiSelectEnabled
            fabAddNote.isVisible = !noteAdapter.multiSelectEnabled
            toolbarSearchView.isVisible = !noteAdapter.multiSelectEnabled
            btnToolbarSettings.isVisible = !noteAdapter.multiSelectEnabled

            txtSelectedNotes.isVisible = noteAdapter.multiSelectEnabled
            btnToolbarDelete.isVisible = noteAdapter.multiSelectEnabled
        }
    }

    private fun setupNotesCollector() {
        Timber.tag(TAG).d("setupNotesCollector: Called")

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.notesFlow.collect { response ->
                    Timber.tag(TAG).d("Notes Collector: Called $response")

                    binding.progressLoading.isVisible = response is Resource.Loading
                    binding.llEmptyNotes.isVisible = response is Resource.Success && response.data.isNullOrEmpty()
                    binding.txtEmptyNotes.text = if(binding.toolbarSearchView.isIconified)
                        getString(R.string.start_adding_notes) else
                            getString(R.string.no_notes_found_for_search)

                    binding.rvNotes.isVisible = response is Resource.Success && !response.data.isNullOrEmpty()

                    if(response is Resource.Success && !response.data.isNullOrEmpty()) {
                        noteAdapter.submitList(response.data)
                        noteAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun setupAppSettingsCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.appSettings.collect {
                    binding.btnSort.text = it.currentSort.name

                    if(viewModel.showSignupMessage()) {
                        openSignupBottomSheet()
                    }
                }
            }
        }
    }

    private fun setupButtons() {
        binding.fabAddNote.setOnClickListener {
            openAddNoteBottomSheet()
        }

        binding.btnSort.setOnClickListener {
            openSortBottomSheet()
        }

        binding.btnToolbarSettings.setOnClickListener {
            openSettingsBottomSheet()
        }

        binding.btnToolbarDelete.setOnClickListener {
            if(!noteAdapter.multiSelectEnabled) {
                return@setOnClickListener
            }

            viewModel.trashNotes(noteAdapter.getTotalSelectedItems())
        }

        activity?.onBackPressedDispatcher
            ?.addCallback(viewLifecycleOwner, object: OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    binding.apply {
                        if(!toolbarSearchView.isIconified || noteAdapter.multiSelectEnabled) {
                            onBackButtonClicked()

                        } else {
                            isEnabled = false
                            activity?.onBackPressedDispatcher?.onBackPressed()
                        }
                    }
                }
            })

        setupToolbarSearch()
    }

    private fun onBackButtonClicked() {
        binding.apply {
            if(!toolbarSearchView.isIconified) {
                toolbarSearchView.isIconified = true
                toolbarSearchView.setQuery("", false)
                toolbarSearchView.clearFocus()
                viewModel.updateSearchQuery("")
            }

            if(noteAdapter.multiSelectEnabled) {
                noteAdapter.multiSelectEnabled = false
                noteAdapter.clearChecksFromAllItems()

                noteAdapter.notifyDataSetChanged()
                toggleNotesMultiSelectViews()
            }
        }
    }

    private fun setupToolbarSearch() {
        binding.toolbarSearchView.setOnSearchClickListener {
            Timber.tag(TAG).d("setupButtons: Search Click Listener")
            toggleToolbarSearch()
        }

        binding.toolbarSearchView.setOnCloseListener {
            Timber.tag(TAG).d("setupButtons: Close Clicked")
            viewModel.updateSearchQuery("")
            toggleToolbarSearch(closing = true)
            false
        }

        binding.toolbarSearchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    viewModel.updateSearchQuery(it)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    private fun toggleToolbarSearch(closing: Boolean = false) {
        binding.apply {
            txtToolbarTitle.isVisible = !txtToolbarTitle.isVisible
            btnToolbarSettings.isVisible = !btnToolbarSettings.isVisible

            toolbarSearchView.layoutParams.width = if(closing)
                ViewGroup.LayoutParams.WRAP_CONTENT else ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    private fun openAddNoteBottomSheet() {
        val addNoteBottomSheet = AddNoteBottomSheetFragment(
            object: AddNoteBottomSheetFragment.AddNoteBottomSheetClickEvent {
                override fun onBtnTextNoteClick() {
                    navigateToNoteDetailsFragment(NoteType.TYPE_TEXT)
                }

                override fun onBtnChecklistClick() {
                    navigateToNoteDetailsFragment(NoteType.TYPE_CHECKLIST)
                }
        })

        addNoteBottomSheet.show(childFragmentManager, "Add note")
    }

    private fun openSortBottomSheet() {
        val sortBottomSheet = SortBottomSheetFragment(
            viewModel.appSettings.value.currentSort.sortType,

            object: SortBottomSheetFragment.SortBottomSheetClickEvent {
                override fun onSortItemClick(sortItem: SortItem) {
                    viewModel.updateCurrentSort(sortItem)
                }
            })

        sortBottomSheet.show(childFragmentManager, "Sort")
    }

    private fun openSettingsBottomSheet() {
        val settingsBottomSheet = SettingsBottomSheetFragment(
            notesSyncing,
            object: SettingsBottomSheetFragment.SettingsBottomSheetClickEvent {
                override fun onBtnSyncNotesClick() {
                    syncNotesImmediately()
                }

                override fun onBtnMoreSettingsClick() {
                    findNavController().navigate(HomeFragmentDirections
                        .actionFragmentHomeToFragmentSettings())
                }
            })

        settingsBottomSheet.show(childFragmentManager, "Settings")
    }
    
    private fun openSignupBottomSheet() {
        val signupBottomSheet = SignupBottomSheetFragment(
            object: SignupBottomSheetFragment.SignupBottomSheetClickEvent {
                override fun onLoginButtonClicked() {
                    viewModel.apply {
                        loginUserWithGoogle()
                    }
                }
            }
        )
        
        signupBottomSheet.show(childFragmentManager, "Signup")
        viewModel.updateSignupPopupLastShownTime()
    }

    private fun navigateToNoteDetailsFragment(noteType: NoteType, note: Note? = null) {
        val noteDetailsFragment = NoteDetailsFragment()
        noteDetailsFragment.arguments = Bundle().apply {
            putSerializable("noteType", noteType)
            putParcelable("note", note)
        }

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.pop_slide_enter,
                R.anim.pop_slide_exit)
            .hide(this)
            .add(R.id.nav_host_fragment, noteDetailsFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun syncNotesImmediately() {
        context?.let {
            if(getNetworkStatus(it) == 0) {
                Toast.makeText(it, "No internet connection..", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if(viewModel.loggedInUser == null) {
            context?.let {
                WorkManager.getInstance(it).cancelUniqueWork(SyncWorker.ONE_TIME_REQUEST_NAME)
            }

            openSignupBottomSheet()
            return
        }

        val workRequestConstraints = Constraints.Builder().apply {
            setRequiredNetworkType(NetworkType.CONNECTED)
        }.build()

        val syncOneTimeRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(workRequestConstraints)
            .build()

        context?.let {
            WorkManager.getInstance(it).enqueueUniqueWork(
                SyncWorker.ONE_TIME_REQUEST_NAME, ExistingWorkPolicy.KEEP, syncOneTimeRequest)
        }

        val syncProcessId = syncOneTimeRequest.id.toString()

        viewModel.saveCurrentSyncProcessId(syncProcessId)
        setupSyncObserver(syncProcessId)
    }

    private fun setupSyncObserver(processId: String? = null) {
        context?.let {
            val savedSyncProcessId = viewModel.getCurrentSyncProcessId()

            if(savedSyncProcessId.isEmpty() && processId.isNullOrEmpty()) {
                binding.llSyncingNotes.visibility = View.GONE
                return
            }

            val currentSyncProcessId = processId ?: savedSyncProcessId

            WorkManager.getInstance(it)
                .getWorkInfoByIdLiveData(UUID.fromString(currentSyncProcessId))
                .observe(viewLifecycleOwner) { workInfo ->
                    Timber.tag(TAG).d("setupSyncObserver: $workInfo")
                    if(workInfo == null) {
                        return@observe
                    }

                    when(workInfo.state) {
                        WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED -> {
                            binding.llSyncingNotes.visibility = View.VISIBLE
                            notesSyncing = true
                        }

                        WorkInfo.State.SUCCEEDED, WorkInfo.State.CANCELLED, WorkInfo.State.FAILED -> {
                            binding.llSyncingNotes.visibility = View.GONE
                            notesSyncing = false
                            viewModel.saveCurrentSyncProcessId("")
                        }
                    }

                    if(workInfo.state == WorkInfo.State.SUCCEEDED) {
                        Toast.makeText(context, "Notes synced successfully", Toast.LENGTH_SHORT).show()
                    }

                    if(workInfo.state == WorkInfo.State.FAILED) {
                        Toast.makeText(context, "Some error occurred while syncing notes." +
                                " Please try again later", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun setupUIEventCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    binding.progressLoading.isVisible = event is HomeViewModel.Event.Loading

                    when(event) {
                        is HomeViewModel.Event.SignInSuccess -> {
                            Toast.makeText(context, "Sign in successful", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(context, HomeActivity::class.java))
                            activity?.finish()
                        }

                        is HomeViewModel.Event.BeginOneTapSignInProcess -> {
                            context?.let {
                                credentialManager = CredentialManager.create(it)
                                launchOneTapSignInUI(
                                    context = it,
                                    credentialManager = credentialManager,
                                    signInRequest = event.signInRequest,
                                    viewLifecycleOwner = viewLifecycleOwner,
                                    onResultRetrieved = { credential ->
                                        viewModel.onOneTapSignInResultRetrieved(credential)
                                    },
                                    onErrorOccurred = {}
                                )
                            }
                            viewModel.onEventOccurred()
                        }

                        is HomeViewModel.Event.BeginOneTapSignInFailure -> {
                            Toast.makeText(context, event.exception.message, Toast.LENGTH_SHORT).show()
                            viewModel.onEventOccurred()
                        }

                        is HomeViewModel.Event.TrashNotesSuccess -> {
                            noteAdapter.multiSelectEnabled = false
                            noteAdapter.notifyDataSetChanged()
                            toggleNotesMultiSelectViews()

                            Snackbar.make(requireView(), "Note(s) trashed successfully",
                                 Snackbar.LENGTH_SHORT).show()

                            viewModel.onEventOccurred()
                        }
                        
                        is HomeViewModel.Event.ErrorOccurred -> {
                            Toast.makeText(context, event.exception.message, Toast.LENGTH_SHORT).show()
                            viewModel.onEventOccurred()
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun addSyncNotesWorker() {
        if(viewModel.loggedInUser == null) {
            context?.let {
                WorkManager.getInstance(it).cancelUniqueWork(SyncWorker.PERIODIC_REQUEST_NAME)
            }
            return
        }

        val workRequestConstraints = Constraints.Builder().apply {
            setRequiredNetworkType(NetworkType.CONNECTED)
        }.build()

        val syncPeriodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(2, TimeUnit.HOURS)
            .setConstraints(workRequestConstraints)
            .build()

        context?.let {
            WorkManager.getInstance(it).enqueueUniquePeriodicWork(
                SyncWorker.PERIODIC_REQUEST_NAME, ExistingPeriodicWorkPolicy.KEEP, syncPeriodicRequest)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.tag(TAG).d("onDestroyView: Called")
        _binding = null
    }
}