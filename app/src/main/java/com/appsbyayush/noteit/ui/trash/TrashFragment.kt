package com.appsbyayush.noteit.ui.trash

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.appsbyayush.noteit.R
import com.appsbyayush.noteit.adapters.NoteAdapter
import com.appsbyayush.noteit.databinding.FragmentTrashBinding
import com.appsbyayush.noteit.models.Note
import com.appsbyayush.noteit.utils.Resource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class TrashFragment: Fragment() {
    companion object {
        private const val TAG = "TrashFragmentyy"
    }
    
    private var _binding: FragmentTrashBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TrashViewModel by viewModels()
    
    private lateinit var noteAdapter: NoteAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupNoteRecyclerView()
        setupButtons()

        setupNotesCollector()
        setupUIEventCollector()

        viewModel.onFragmentStarted()
    }

    private fun setupNoteRecyclerView() {
        noteAdapter = NoteAdapter(object: NoteAdapter.NoteItemClickEvent {
            override fun onItemClick(note: Note) {
                showRestoreDialog(note)
            }

            override fun onItemLongClick(note: Note) {}
        })

        binding.rvNotes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = noteAdapter
        }
    }

    private fun setupButtons() {
        binding.btnToolbarBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupNotesCollector() {
        Timber.tag(TAG).d("setupNotesCollector: Called")

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.notesFlow.collect { response ->
                    Timber.tag(TAG).d("Notes Collector: Called $response")

                    binding.progressLoading.isVisible = response is Resource.Loading
                    binding.llEmptyNotes.isVisible = response is Resource.Success && response.data.isNullOrEmpty()
                    binding.txtEmptyNotes.text = getString(R.string.no_notes_found)

                    binding.rvNotes.isVisible = response is Resource.Success && !response.data.isNullOrEmpty()

                    if(response is Resource.Success && !response.data.isNullOrEmpty()) {
                        noteAdapter.submitList(response.data)
                        noteAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun showRestoreDialog(note: Note) {
        val dialog = MaterialAlertDialogBuilder(requireContext()).apply {
            setMessage("To view this note, you need to restore it")
            setPositiveButton("Restore Note") { dialog, _ ->
                viewModel.restoreNote(note)
                dialog.dismiss()
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        }.create()

        dialog.show()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).isAllCaps = false
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).isAllCaps = false
    }

    private fun setupUIEventCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    binding.progressLoading.isVisible = event is TrashViewModel.Event.Loading

                    when(event) {
                        is TrashViewModel.Event.NoteRestoredSuccess -> {
                            Toast.makeText(context, "Note successfully restored",
                                Toast.LENGTH_SHORT).show()
                        }

                        is TrashViewModel.Event.ErrorOccurred -> {
                            Toast.makeText(context, event.exception.message, Toast.LENGTH_SHORT).show()
                            viewModel.onEventOccurred()
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}