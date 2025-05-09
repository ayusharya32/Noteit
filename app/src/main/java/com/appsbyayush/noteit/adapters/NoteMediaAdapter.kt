package com.appsbyayush.noteit.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.appsbyayush.noteit.databinding.ItemNoteMediaBinding
import com.appsbyayush.noteit.models.NoteMediaItem
import com.bumptech.glide.Glide

class NoteMediaAdapter(
    var editMode: Boolean,
    private val clickEvent: NoteMediaItemClickEvent
): RecyclerView.Adapter<NoteMediaAdapter.NoteMediaViewHolder>() {

    var mediaItems: List<NoteMediaItem> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteMediaViewHolder {
        val binding = ItemNoteMediaBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return NoteMediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteMediaViewHolder, position: Int) {
        val item = mediaItems[position]
        holder.bind(item)
    }

    override fun getItemCount() = mediaItems.size

    inner class NoteMediaViewHolder(private val binding: ItemNoteMediaBinding)
        : RecyclerView.ViewHolder(binding.root) {

            init {
                binding.root.setOnClickListener {
                    if(adapterPosition != RecyclerView.NO_POSITION) {
                        clickEvent.onItemClick(mediaItems[adapterPosition])
                    }
                }

                binding.imgRemove.setOnClickListener {
                    if(adapterPosition != RecyclerView.NO_POSITION) {
                        clickEvent.onRemoveBtnClick(mediaItems[adapterPosition])
                    }
                }
            }

            fun bind(mediaItem: NoteMediaItem) {
                binding.apply {
                    val itemUri = if(mediaItem.isFileUploaded) mediaItem.itemUrl
                        else mediaItem.localUriString?.toUri()

                    Glide.with(root)
                        .load(itemUri)
                        .into(imgMedia)

                    imgRemove.isVisible = editMode
                }
            }
        }

    interface NoteMediaItemClickEvent {
        fun onItemClick(mediaItem: NoteMediaItem)
        fun onRemoveBtnClick(mediaItem: NoteMediaItem)
    }
}