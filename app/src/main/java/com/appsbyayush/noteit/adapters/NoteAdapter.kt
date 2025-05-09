package com.appsbyayush.noteit.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.alpha
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Visibility
import com.appsbyayush.noteit.databinding.ItemNoteBinding
import com.appsbyayush.noteit.models.Note
import com.appsbyayush.noteit.utils.CommonMethods
import com.appsbyayush.noteit.utils.Constants
import com.appsbyayush.noteit.utils.toDp
import com.appsbyayush.noteit.utils.toPx

class NoteAdapter(
    private val clickEvent: NoteItemClickEvent
): ListAdapter<Note, NoteAdapter.NoteViewHolder>(NoteComparator()) {

    var multiSelectEnabled = false

    fun getTotalSelectedItems(): List<Note> {
        return currentList.filter { it.isChecked }
    }

    fun clearChecksFromAllItems() {
        currentList.forEach { it.isChecked = false }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        holder.bind(note)
    }

    inner class NoteViewHolder(private val binding: ItemNoteBinding)
        : RecyclerView.ViewHolder(binding.root) {

            private lateinit var note: Note

            init {
                binding.root.setOnClickListener {
                    if(adapterPosition != RecyclerView.NO_POSITION) {
                        clickEvent.onItemClick(getItem(adapterPosition))
                    }
                }

                binding.root.setOnLongClickListener {
                    if(adapterPosition != RecyclerView.NO_POSITION) {
                        clickEvent.onItemLongClick(getItem(adapterPosition))
                    }

                    true
                }
            }

            fun bind(note: Note) {
                this.note = note

                binding.apply {
                    txtNoteTitle.text = note.title
                    txtNoteDescription.text = note.description
                    txtNoteModified.text = getFormattedNoteModifiedDate()

                    val noteBarColor = Color.parseColor(note.color.darkHexCode)
                    noteColorBar.setBackgroundColor(noteBarColor)

                    val noteBgColor = Color.parseColor(note.color.lightHexCode)
                    clMain.setBackgroundColor(ColorUtils.setAlphaComponent(noteBgColor, 100))

                    txtNoteDescription.isVisible = note.description.isNotEmpty()

                    if(note.isChecked && multiSelectEnabled) {
                        root.strokeWidth = 5
                        root.radius = 3.toPx(binding.root.context)
                        noteSelectedForeground.visibility = View.VISIBLE

                    } else {
                        root.strokeWidth = 0
                        root.radius = 6.toPx(binding.root.context)
                        noteSelectedForeground.visibility = View.GONE
                    }
                }
            }

            private fun getFormattedNoteModifiedDate(): String {
                return when {
                    CommonMethods.isDateOfToday(note.modifiedAt) -> "Today"
                    CommonMethods.isDateOfYesterday(note.modifiedAt) -> "Yesterday"
                    else -> CommonMethods.getFormattedDateTime(note.modifiedAt,
                        Constants.DATE_FORMAT_1).replace("/", " ")
                }
            }
        }

    interface NoteItemClickEvent {
        fun onItemClick(note: Note)
        fun onItemLongClick(note: Note)
    }

    class NoteComparator: DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}