package com.appsbyayush.noteit.ui.bottomsheets

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.appsbyayush.noteit.adapters.NoteColorAdapter
import com.appsbyayush.noteit.databinding.BottomSheetPickColorBinding
import com.appsbyayush.noteit.models.NoteColor
import com.appsbyayush.noteit.ui.bottomsheets.base.BaseBottomSheetDialogFragment
import com.appsbyayush.noteit.utils.Constants

class PickColorBottomSheetFragment(
    private val clickEvent: PickColorBottomSheetClickEvent
): BaseBottomSheetDialogFragment() {
    companion object {
        private const val TAG = "PickColorBottomSheetyy"
    }

    private var _binding: BottomSheetPickColorBinding? = null
    private val binding get() = _binding!!

    private lateinit var noteColorAdapter: NoteColorAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPickColorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupColorRecyclerView()
    }

    private fun setupColorRecyclerView() {
        noteColorAdapter = NoteColorAdapter(object : NoteColorAdapter.NoteColorItemClickEvent {
            override fun onItemClick(color: NoteColor) {
                clickEvent.onColorItemClick(color)
                dismissAfterDelay()
            }
        })

        binding.rvColors.apply {
            layoutManager = GridLayoutManager(context, 6)
            adapter = noteColorAdapter
        }

        noteColorAdapter.submitList(Constants.colorList)
    }

    override fun onPause() {
        super.onPause()

        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface PickColorBottomSheetClickEvent {
        fun onColorItemClick(color: NoteColor)
    }
}