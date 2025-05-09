package com.appsbyayush.noteit.adapters

import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.appsbyayush.noteit.R
import com.appsbyayush.noteit.databinding.ItemChecklistBinding
import com.appsbyayush.noteit.models.ChecklistItem

class ChecklistAdapter(
    private var editMode: Boolean,
    private val clickEvent: ChecklistItemClickEvent
): RecyclerView.Adapter<ChecklistAdapter.ChecklistItemViewHolder>() {

    companion object {
        private const val TAG = "ChecklistAdapteryy"
    }

    var checklistItems: List<ChecklistItem> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecklistItemViewHolder {
        val binding = ItemChecklistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChecklistItemViewHolder(binding)
    }

    override fun getItemCount() = checklistItems.size

    override fun onBindViewHolder(holder: ChecklistItemViewHolder, position: Int) {
        val item = checklistItems[position]
        holder.bind(item)
    }

    inner class ChecklistItemViewHolder(private val binding: ItemChecklistBinding)
        : RecyclerView.ViewHolder(binding.root) {

            init {
                binding.root.setOnClickListener {
                    if(adapterPosition != RecyclerView.NO_POSITION && !editMode) {
                        clickEvent.onItemClick(checklistItems[adapterPosition], adapterPosition)
                    }
                }

                binding.imgRemove.setOnClickListener {
                    Log.d(TAG, "Inside Remove Listener: $adapterPosition")
                    if(adapterPosition != RecyclerView.NO_POSITION) {
                        clickEvent.onRemoveBtnClick(checklistItems[adapterPosition], adapterPosition)
                    }
                }
            }

            fun bind(checklistItem: ChecklistItem) {
                binding.apply {
                    txtChecklistContent.text = checklistItem.content

                    if(checklistItem.done) {
                        imgDone.setImageResource(R.drawable.ic_check_circle)
                        imgDone.setColorFilter(ContextCompat.getColor(binding.root.context, R.color.green_300))
                        txtChecklistContent.paintFlags = txtChecklistContent.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

                    } else {
                        imgDone.setImageResource(R.drawable.ic_pending)
                        imgDone.setColorFilter(ContextCompat.getColor(binding.root.context, R.color.grey_300))
                        txtChecklistContent.paintFlags = txtChecklistContent.paintFlags and
                                Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    }

                    imgRemove.isVisible = editMode
                    imgDone.isVisible = !editMode
                }
            }
        }

    interface ChecklistItemClickEvent {
        fun onItemClick(item: ChecklistItem, position: Int)
        fun onRemoveBtnClick(item: ChecklistItem, position: Int)
    }
}