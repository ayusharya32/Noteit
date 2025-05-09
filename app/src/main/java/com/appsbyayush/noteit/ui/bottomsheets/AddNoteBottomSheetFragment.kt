package com.appsbyayush.noteit.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.appsbyayush.noteit.databinding.BottomSheetAddNoteBinding
import com.appsbyayush.noteit.ui.bottomsheets.base.BaseBottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class AddNoteBottomSheetFragment(
    private val clickEvent: AddNoteBottomSheetClickEvent
): BaseBottomSheetDialogFragment() {

    private var _binding: BottomSheetAddNoteBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtons()
    }

    private fun setupButtons() {
        binding.btnAddTextNote.setOnClickListener {
            clickEvent.onBtnTextNoteClick()
            dismissAfterDelay()
        }

        binding.btnAddChecklist.setOnClickListener {
            clickEvent.onBtnChecklistClick()
            dismissAfterDelay()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface AddNoteBottomSheetClickEvent {
        fun onBtnTextNoteClick()
        fun onBtnChecklistClick()
    }
}