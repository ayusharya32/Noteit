package com.appsbyayush.noteit.ui.bottomsheets

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.appsbyayush.noteit.databinding.BottomSheetSignUpBinding
import com.appsbyayush.noteit.ui.bottomsheets.base.BaseBottomSheetDialogFragment

class SignupBottomSheetFragment(
    private val clickEvent: SignupBottomSheetClickEvent
): BaseBottomSheetDialogFragment() {
    private var _binding: BottomSheetSignUpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtons()
    }

    private fun setupButtons() {
        binding.btnSignIn.setOnClickListener {
            clickEvent.onLoginButtonClicked()
            dismissAfterDelay()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface SignupBottomSheetClickEvent {
        fun onLoginButtonClicked()
    }
}