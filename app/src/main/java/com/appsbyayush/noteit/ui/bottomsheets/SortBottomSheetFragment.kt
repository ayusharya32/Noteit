package com.appsbyayush.noteit.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.appsbyayush.noteit.adapters.SortAdapter
import com.appsbyayush.noteit.databinding.BottomSheetSortBinding
import com.appsbyayush.noteit.models.SortItem
import com.appsbyayush.noteit.ui.bottomsheets.base.BaseBottomSheetDialogFragment
import com.appsbyayush.noteit.utils.Constants
import com.appsbyayush.noteit.utils.enums.SortType

class SortBottomSheetFragment(
    private val currentSortType: SortType,
    private val clickEvent: SortBottomSheetClickEvent
): BaseBottomSheetDialogFragment() {

    private var _binding: BottomSheetSortBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSortBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSortRecyclerView()
    }

    private fun setupSortRecyclerView() {
        val sortAdapter = SortAdapter(Constants.SORT_LIST, currentSortType,
            object: SortAdapter.SortItemClickEvent {
                override fun onItemClick(sortItem: SortItem) {
                    clickEvent.onSortItemClick(sortItem)
                    dismissAfterDelay()
                }
            })

        binding.rvSort.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = sortAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface SortBottomSheetClickEvent {
        fun onSortItemClick(sortItem: SortItem)
    }
}