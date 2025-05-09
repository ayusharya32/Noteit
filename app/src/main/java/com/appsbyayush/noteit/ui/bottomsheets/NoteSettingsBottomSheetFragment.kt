package com.appsbyayush.noteit.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.appsbyayush.noteit.databinding.BottomSheetNoteSettingsBinding
import com.appsbyayush.noteit.ui.bottomsheets.base.BaseBottomSheetDialogFragment

class NoteSettingsBottomSheetFragment(
    private val clickEvent: NoteSettingsBottomSheetClickEvent
): BaseBottomSheetDialogFragment() {

    private var _binding: BottomSheetNoteSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetNoteSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtons()
    }

    private fun setupButtons() {
        binding.apply {
            btnTrashNote.setOnClickListener {
                clickEvent.onTrashBtnClick()
                dismissAfterDelay()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface NoteSettingsBottomSheetClickEvent {
        fun onTrashBtnClick()
    }
}