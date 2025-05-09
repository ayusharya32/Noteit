package com.appsbyayush.noteit.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.appsbyayush.noteit.databinding.BottomSheetSettingsBinding
import com.appsbyayush.noteit.ui.bottomsheets.base.BaseBottomSheetDialogFragment

class SettingsBottomSheetFragment(
    private val notesSyncing: Boolean = false,
    private val clickEvent: SettingsBottomSheetClickEvent
): BaseBottomSheetDialogFragment() {
    private var _binding: BottomSheetSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnSyncNotesText = if(notesSyncing) "Syncing notes..." else "Sync Notes"

        binding.btnSyncNotes.apply {
            text = btnSyncNotesText
            isEnabled = !notesSyncing
        }

        setupButtons()
    }

    private fun setupButtons() {
        binding.btnSyncNotes.setOnClickListener {
            clickEvent.onBtnSyncNotesClick()
            dismissAfterDelay()
        }

        binding.btnMoreSettings.setOnClickListener {
            clickEvent.onBtnMoreSettingsClick()
            dismissAfterDelay()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface SettingsBottomSheetClickEvent {
        fun onBtnSyncNotesClick()
        fun onBtnMoreSettingsClick()
    }
}