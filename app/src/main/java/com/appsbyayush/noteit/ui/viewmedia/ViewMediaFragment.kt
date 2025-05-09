package com.appsbyayush.noteit.ui.viewmedia

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.appsbyayush.noteit.databinding.FragmentViewMediaBinding
import com.bumptech.glide.Glide

class ViewMediaFragment: Fragment() {
    private var _binding: FragmentViewMediaBinding? = null
    private val binding get() = _binding!!

    private val args: ViewMediaFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewMediaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mediaItem = args.mediaItem

        val itemUri = if(mediaItem.isFileUploaded) mediaItem.itemUrl
        else mediaItem.localUriString?.toUri()

        Glide.with(binding.root)
            .load(itemUri)
            .into(binding.imgMedia)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}