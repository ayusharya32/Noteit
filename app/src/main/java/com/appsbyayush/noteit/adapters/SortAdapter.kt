package com.appsbyayush.noteit.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.appsbyayush.noteit.R
import com.appsbyayush.noteit.databinding.ItemSortBinding
import com.appsbyayush.noteit.models.SortItem
import com.appsbyayush.noteit.utils.enums.SortType

class SortAdapter(
    private var sortList: List<SortItem>,
    private var currentSortType: SortType,
    private val clickEvent: SortItemClickEvent
): RecyclerView.Adapter<SortAdapter.SortViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SortViewHolder {
        val binding = ItemSortBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)

        return SortViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SortViewHolder, position: Int) {
        val item: SortItem = sortList[position]

        holder.bind(item)
    }

    override fun getItemCount() = sortList.size

    inner class SortViewHolder(private val binding: ItemSortBinding)
        : RecyclerView.ViewHolder(binding.root) {

            init {
                binding.btnSortName.setOnClickListener {
                    if(adapterPosition != RecyclerView.NO_POSITION) {
                        clickEvent.onItemClick(sortList[adapterPosition])
                    }
                }
            }

        fun bind(item: SortItem) {
            binding.btnSortName.text = item.name

            if (item.sortType == currentSortType) {
                binding.btnSortName.setBackgroundColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.blue_200
                    )
                )
                binding.btnSortName.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.white
                    )
                )
            }
        }
    }

    interface SortItemClickEvent {
        fun onItemClick(sortItem: SortItem)
    }
}