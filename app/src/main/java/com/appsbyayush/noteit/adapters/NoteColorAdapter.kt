package com.appsbyayush.noteit.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.appsbyayush.noteit.databinding.ItemNoteColorBinding
import com.appsbyayush.noteit.models.NoteColor

class NoteColorAdapter(
    private val clickEvent: NoteColorItemClickEvent
): ListAdapter<NoteColor, NoteColorAdapter.NoteColorViewHolder>(NoteColorComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteColorViewHolder {
        val binding = ItemNoteColorBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return NoteColorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteColorViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class NoteColorViewHolder(private val binding: ItemNoteColorBinding)
        : RecyclerView.ViewHolder(binding.root) {

            init {
                binding.root.setOnClickListener {
                    if(adapterPosition != RecyclerView.NO_POSITION) {
                        clickEvent.onItemClick(getItem(adapterPosition))
                    }
                }
            }

            fun bind(colorItem: NoteColor) {
                binding.colorView.setBackgroundColor(Color.parseColor(colorItem.darkHexCode))
            }
    }

    interface NoteColorItemClickEvent {
        fun onItemClick(color: NoteColor)
    }

    class NoteColorComparator: DiffUtil.ItemCallback<NoteColor>() {
        override fun areItemsTheSame(oldItem: NoteColor, newItem: NoteColor): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: NoteColor, newItem: NoteColor): Boolean {
            return oldItem == newItem
        }
    }
}