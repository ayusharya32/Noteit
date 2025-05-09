package com.appsbyayush.noteit.ui.notedetails

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.ColorUtils
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.appsbyayush.noteit.R
import com.appsbyayush.noteit.adapters.ChecklistAdapter
import com.appsbyayush.noteit.adapters.NoteMediaAdapter
import com.appsbyayush.noteit.databinding.FragmentNoteDetailsBinding
import com.appsbyayush.noteit.models.ChecklistItem
import com.appsbyayush.noteit.models.NoteColor
import com.appsbyayush.noteit.models.NoteMediaItem
import com.appsbyayush.noteit.ui.bottomsheets.NoteSettingsBottomSheetFragment
import com.appsbyayush.noteit.ui.bottomsheets.PickColorBottomSheetFragment
import com.appsbyayush.noteit.ui.viewmedia.ViewMediaFragment
import com.appsbyayush.noteit.utils.Constants
import com.appsbyayush.noteit.utils.enums.NoteType
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import java.util.UUID

@SuppressLint("NotifyDataSetChanged")
@AndroidEntryPoint
class NoteDetailsFragment: Fragment() {
    companion object {
        private const val TAG = "NoteDetailsFragmentyy"
    }

    private var _binding: FragmentNoteDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NoteDetailsViewModel by viewModels()
    private val args: NoteDetailsFragmentArgs by navArgs()

    private lateinit var noteType: NoteType
    private lateinit var noteColor: NoteColor
    private var editMode = false
    private var addMediaBtnClicked = false

    private lateinit var checklistAdapter: ChecklistAdapter
    private lateinit var mediaAdapter: NoteMediaAdapter

    private var etDescriptionTextWatcher: TextWatcher? = null
    private var txtDescriptionTextWatcher: TextWatcher? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        args.note?.let {
            viewModel.apply {
                currentNote = it
                isNewNote = false
                getNoteMediaItems()
            }
        }

        noteType = args.noteType
        noteColor = Constants.colorList[0]

        binding.apply {
            Timber.tag(TAG)
                .d("setupMediaItemsCollector: ViewCreated - Height: ${etNoteDescription.height}")
            Timber.tag(TAG)
                .d("setupMediaItemsCollector: ViewCreated - MinHeight: ${etNoteDescription.minHeight}")
        }

        setupFragment()
        setupButtons()

        setupMediaItemsCollector()
        setupUIEventCollector()
    }

    private fun setupFragment() {
        if(!viewModel.isNewNote) {
            setupNoteDetails()
        }

        setupFragmentViews(editMode = viewModel.isNewNote)
    }

    private fun setupButtons() {
        binding.btnToolbarEdit.setOnClickListener {
            setupFragmentViews(editMode = true)
        }

        binding.btnAddChecklistItem.setOnClickListener {
            onAddChecklistItemClicked()
        }

        binding.btnToolbarColorPick.setOnClickListener {
            onToolbarColorPickClicked()
        }

        binding.btnToolbarMenu.setOnClickListener {
            openNoteSettingsBottomSheet()
        }

        binding.fabAddMedia.setOnClickListener {
            addMediaBtnClicked = true
            pickMediaLauncher.launch("image/*")
        }

        activity?.onBackPressedDispatcher
            ?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onBackButtonClicked()

                    if(!editMode || isNoteEmpty()) {
                        isEnabled = false
                        activity?.onBackPressedDispatcher?.onBackPressed()
                    }
                }
            })
    }

    private fun openNoteSettingsBottomSheet() {
        val noteSettingsSheet = NoteSettingsBottomSheetFragment(
            object: NoteSettingsBottomSheetFragment.NoteSettingsBottomSheetClickEvent {
                override fun onTrashBtnClick() {
                    showTrashNoteDialog()
                }
            }
        )

        noteSettingsSheet.show(childFragmentManager, "Note Settings")
    }

    private fun showTrashNoteDialog() {
        MaterialAlertDialogBuilder(requireContext()).apply {
            setMessage("Are you sure you want to move this note to trash?")
            setPositiveButton("Confirm") { dialog, _ ->
                viewModel.trashCurrentNote()
                dialog.dismiss()
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

    private fun setupFragmentViews(editMode: Boolean) {
        this.editMode = editMode

        binding.apply {
            txtNoteTitle.isVisible = !editMode
            txtNoteDescription.isVisible = !editMode && noteType == NoteType.TYPE_TEXT
            btnToolbarEdit.isVisible = !editMode

            llEditChecklist.isVisible = noteType == NoteType.TYPE_CHECKLIST && editMode
            rvChecklistItems.isVisible = noteType == NoteType.TYPE_CHECKLIST

            btnToolbarColorPick.isVisible = editMode
            etNoteTitle.isVisible = editMode
            etNoteDescription.isVisible = editMode && noteType == NoteType.TYPE_TEXT

            rvMedia.isVisible = viewModel.currentNoteMediaItems.value.isNotEmpty()
            fabAddMedia.isVisible = editMode

            txtNoteModified.isVisible = !editMode

            if(noteType == NoteType.TYPE_CHECKLIST) {
                setupChecklistItemsRecyclerView()
            }

            setupMediaRecyclerView()
            setupDescriptionTextChangeListener()

            if(noteType == NoteType.TYPE_TEXT && viewModel.currentNoteMediaItems.value.isEmpty()) {
                setupDescriptionHeight("setupFragmentViews")
            }

            setupNoteColor()
        }
    }

    private fun setupDescriptionTextChangeListener() {
        binding.apply {
            if(etDescriptionTextWatcher != null) {
                etNoteDescription.removeTextChangedListener(etDescriptionTextWatcher)
            }

            if(txtDescriptionTextWatcher != null) {
                txtNoteDescription.removeTextChangedListener(txtDescriptionTextWatcher)
            }

            etDescriptionTextWatcher = etNoteDescription.doAfterTextChanged {
                Timber.tag(TAG)
                    .d("setupDescriptionTextChangeListener(ET): Called")
                setupDescriptionHeight("ET Changed")
            }

            txtDescriptionTextWatcher = txtNoteDescription.doAfterTextChanged {
                Timber.tag(TAG)
                    .d("setupDescriptionTextChangeListener(TXT): Called")
                setupDescriptionHeight("TXT Changed")
            }
        }
    }

    private fun  setupDescriptionHeight(caller: String) {
        Timber.tag(TAG)
            .d("setupDescriptionHeight: Called $caller")

        if(mediaAdapter.mediaItems.isEmpty()) {
            setupDescriptionHeightForNoMediaItems()
        } else {
            setupDescriptionHeightForNoteWithMediaItems(caller)
        }
    }

    private fun setupDescriptionHeightForNoMediaItems() {
        Timber.tag(TAG)
            .d("setupDescriptionHeightForNoMediaItems: Called")

        binding.apply {
            scrollMain.doOnLayout {
                val totalLinesHeight = getDescriptionTotalLinesHeight()

                if(totalLinesHeight <= binding.scrollMain.height) {
                    etNoteDescription.height = binding.scrollMain.height - 50
                    txtNoteDescription.height = binding.scrollMain.height - (txtNoteModified.height + 50)
                } else {
                    etNoteDescription.height = totalLinesHeight + 20
                    txtNoteDescription.height = totalLinesHeight + 20

                    if(!editMode) scrollMain.scrollY = 0
                }
            }
        }
    }

    private fun setupDescriptionHeightForNoteWithMediaItems(caller: String) {
        Timber.tag(TAG)
            .d("setupDescriptionHeightForNoteWithMediaItems: Called: $caller")

        binding.apply {
            val totalLinesHeight = getDescriptionTotalLinesHeight()
            Timber.tag(TAG)
                .d("setupDescriptionHeightForNoteWithMediaItems: Called: $caller -- totalLinesHeight: ${totalLinesHeight} -- DefaultMinHeight: ${Constants.DESCRIPTION_DEFAULT_MIN_HEIGHT}")

            if(totalLinesHeight >= Constants.DESCRIPTION_DEFAULT_MIN_HEIGHT) {
                etNoteDescription.height = totalLinesHeight + 20
                txtNoteDescription.height = totalLinesHeight + 20

                if(!editMode) scrollMain.scrollY = 0
                Timber.tag(TAG)
                    .d("setupDescriptionHeightForNoteWithMediaItems: Called TRUE -- Updated ET Height - ${etNoteDescription.height} -- Updated TXT Height - ${txtNoteDescription.height}")
            } else {
                etNoteDescription.height = Constants.DESCRIPTION_DEFAULT_MIN_HEIGHT
                txtNoteDescription.height = Constants.DESCRIPTION_DEFAULT_MIN_HEIGHT

//                CoroutineScope(Dispatchers.Main).launch {
//                    delay(2000)
                Timber.tag(TAG)
                    .d("setupDescriptionHeightForNoteWithMediaItems: Called FALSE -- Updated ET Height - ${etNoteDescription.height} -- Updated TXT Height - ${txtNoteDescription.height}")
//                }
            }
        }
    }

    private fun getDescriptionTotalLinesHeight(): Int {
        return if(editMode) binding.etNoteDescription.lineCount * binding.etNoteDescription.lineHeight
        else binding.txtNoteDescription.lineCount * binding.txtNoteDescription.lineHeight
    }

    private fun setupNoteDetails() {
        Timber.tag(TAG)
            .d("setupNoteDetails: ${viewModel.currentNote}")

        viewModel.currentNote.let {
            binding.apply {
                txtNoteTitle.text = it.title
                etNoteTitle.setText(it.title)

                if(it.noteType == NoteType.TYPE_TEXT) {
                    txtNoteDescription.text = it.description
                    etNoteDescription.setText(it.description)

                }

                val lastModifiedString = "Last Modified: ${viewModel.getFormattedNoteModifiedDate()}"
                txtNoteModified.text = lastModifiedString

                noteColor = it.color
            }
        }
    }

    private fun setupChecklistItemsRecyclerView() {
        checklistAdapter = ChecklistAdapter(
            editMode,
            object: ChecklistAdapter.ChecklistItemClickEvent {
                override fun onItemClick(item: ChecklistItem, position: Int) {
                    item.done = !item.done
                    checklistAdapter.notifyItemChanged(position)

                    prepareAndSaveNote()
                }

                override fun onRemoveBtnClick(item: ChecklistItem, position: Int) {
                    viewModel.currentNote.checklistItems.remove(item)
                    checklistAdapter.checklistItems = viewModel.currentNote.checklistItems
                    checklistAdapter.notifyItemRemoved(position)
                }
            }
        )

        binding.rvChecklistItems.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = checklistAdapter
        }

        checklistAdapter.checklistItems = viewModel.currentNote.checklistItems
        checklistAdapter.notifyDataSetChanged()
    }

    private fun setupMediaRecyclerView() {
        if(this::mediaAdapter.isInitialized) {
            mediaAdapter.editMode = editMode
            mediaAdapter.notifyDataSetChanged()

            return
        }

        mediaAdapter = NoteMediaAdapter(editMode, object: NoteMediaAdapter.NoteMediaItemClickEvent {
            override fun onItemClick(mediaItem: NoteMediaItem) {
                navigateToViewMediaFragment(mediaItem)
            }

            override fun onRemoveBtnClick(mediaItem: NoteMediaItem) {
                viewModel.deleteMediaItem(mediaItem)
            }
        })

        binding.rvMedia.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = mediaAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun onAddChecklistItemClicked() {
        if(binding.etChecklistItemContent.text.toString().trim().isEmpty()) {
            return
        }

        viewModel.currentNote.checklistItems.add(ChecklistItem(
            UUID.randomUUID().toString(),
            binding.etChecklistItemContent.text.toString().trim(),
            false
        ))

        binding.etChecklistItemContent.setText("")
        checklistAdapter.checklistItems = viewModel.currentNote.checklistItems
        checklistAdapter.notifyDataSetChanged()
    }

    private fun onToolbarColorPickClicked() {
        val pickColorSheet = PickColorBottomSheetFragment(
            object : PickColorBottomSheetFragment.PickColorBottomSheetClickEvent {
                override fun onColorItemClick(color: NoteColor) {
                    noteColor = color
                    setupNoteColor()
                }
            }
        )

        pickColorSheet.show(childFragmentManager, "Pick Color")
    }

    private fun setupNoteColor() {
        binding.apply {
            llAppBar.setBackgroundColor(Color.parseColor(noteColor.darkHexCode))
            view.setBackgroundColor(Color.parseColor(noteColor.darkHexCode))
            val noteBgColor = Color.parseColor(noteColor.lightHexCode)
            clMain.setBackgroundColor(ColorUtils.setAlphaComponent(noteBgColor, 100))
            btnToolbarColorPick.setBackgroundColor(Color.parseColor(noteColor.darkHexCode))
            txtNoteDescription.setLineColor(Color.parseColor(noteColor.darkHexCode))
            etNoteDescription.setLineColor(Color.parseColor(noteColor.darkHexCode))
            btnAddChecklistItem.setBackgroundColor(Color.parseColor(noteColor.darkHexCode))
        }
    }

    private fun onBackButtonClicked() {
        Timber.tag(TAG)
            .d("onBackButtonClicked: EditMode - $editMode || IsNoteEmpty() - ${isNoteEmpty()}")
        if(!editMode || isNoteEmpty() || addMediaBtnClicked) {
            return
        }

        prepareAndSaveNote()
    }

    private fun isNoteEmpty(): Boolean {
        return when(noteType) {
            NoteType.TYPE_TEXT -> {
                binding.etNoteTitle.text.toString().isEmpty()
                        && binding.etNoteDescription.text.toString().isEmpty()
                        && viewModel.currentNoteMediaItems.value.isEmpty()
            }

            NoteType.TYPE_CHECKLIST -> {
                viewModel.isNewNote
                        && viewModel.currentNote.checklistItems.isEmpty()
                        && viewModel.currentNoteMediaItems.value.isEmpty()
            }
        }
    }

    private fun prepareAndSaveNote() {
        Timber.tag(TAG)
            .d("prepareAndSaveNote: called")

        binding.apply {
            val noteTitle = getNoteTitle()
            val authUser = viewModel.getAuthenticatedUser()

            viewModel.currentNote.let { note ->
                note.title = noteTitle
                note.description = etNoteDescription.text.toString().trim()
                note.color = noteColor

                if(viewModel.isNewNote) {
                    note.userId = authUser?.uid
                    note.noteType = noteType
                    note.createdAt = Calendar.getInstance().time
                }

                note.modifiedAt = Calendar.getInstance().time

                Timber.tag(TAG)
                    .d("prepareAndSaveNote: ${note.description}")
                viewModel.saveNote(note)
            }
        }
    }

    private fun getNoteTitle(): String {
        return binding.etNoteTitle.text.toString().trim().ifEmpty {
            if(noteType == NoteType.TYPE_TEXT) {
                if(binding.etNoteDescription.text.toString().trim().length < 12) {
                    binding.etNoteDescription.text.toString().trim()
                } else {
                    binding.etNoteDescription.text.toString().trim().substring(0, 11)
                }
            } else {
                if(viewModel.currentNote.checklistItems[0].content.length < 12) {
                    viewModel.currentNote.checklistItems[0].content.trim()
                } else {
                    viewModel.currentNote.checklistItems[0].content.trim().substring(0, 11)
                }
            }
        }
    }

    private fun setupMediaItemsCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentNoteMediaItems.collect { mediaItems ->
                    mediaAdapter.mediaItems = mediaItems.filter { !it.isDeleted }
                    mediaAdapter.notifyDataSetChanged()

                    binding.apply {
                        rvMedia.isVisible = mediaItems.isNotEmpty()
                        setupDescriptionHeight("MediaItemsCollector")
                    }
                }
            }
        }
    }


    private fun navigateToViewMediaFragment(mediaItem: NoteMediaItem) {
        val viewMediaFragment = ViewMediaFragment()
        viewMediaFragment.arguments = Bundle().apply {
            putParcelable("mediaItem", mediaItem)
        }

        parentFragmentManager.beginTransaction()
            .hide(this)
            .add(R.id.nav_host_fragment, viewMediaFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun setupUIEventCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is NoteDetailsViewModel.Event.NoteSaveSuccess -> {
                            if(editMode) {
                                val successMessage = if(noteType == NoteType.TYPE_TEXT) "Note Saved"
                                else "Checklist Saved"
                                Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()

                                setupNoteDetails()
                                setupFragmentViews(editMode = false)
                            }

                            viewModel.onEventOccurred()
                        }

                        is NoteDetailsViewModel.Event.NoteTrashSuccess -> {
                            Toast.makeText(
                                context,
                                "Note successfully moved to trash",
                                Toast.LENGTH_SHORT
                            ).show()

                            parentFragmentManager.popBackStack()
                        }

                        is NoteDetailsViewModel.Event.ErrorOccurred -> {
                            Toast.makeText(context, event.exception.message, Toast.LENGTH_SHORT).show()
                            viewModel.onEventOccurred()
                        }

                        is NoteDetailsViewModel.Event.Idle -> {}
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()

        onBackButtonClicked()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val pickMediaLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { selectedUriList ->
            addMediaBtnClicked = false

            if(selectedUriList.isEmpty()) {
                return@registerForActivityResult
            }

            viewModel.addNewMediaItems(selectedUriList)
        }
}