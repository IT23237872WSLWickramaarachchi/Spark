package com.example.spark.ui.auth

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.spark.R
import com.example.spark.databinding.FragmentWelcomeBinding

class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    /**
     * Configurable click listeners with default navigation actions.
     */
    var onLoginClick: (() -> Unit)? = null
    var onRegisterClick: (() -> Unit)? = null
    var onForgotPasswordClick: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupRegisterSpannable()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            onLoginClick?.invoke() ?: run {
                findNavController().navigate(R.id.action_welcome_to_dashboard)
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            onForgotPasswordClick?.invoke() ?: run {
                // Placeholder for password recovery flow
            }
        }
    }

    private fun setupRegisterSpannable() {
        val fullText = getString(R.string.welcome_register_prompt)
        val registerWord = getString(R.string.action_register)

        val spannable = SpannableString(fullText)
        val startIndex = fullText.indexOf(registerWord)

        if (startIndex != -1) {
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    onRegisterClick?.invoke() ?: run {
                        findNavController().navigate(R.id.action_welcome_to_register)
                    }
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = ContextCompat.getColor(requireContext(), R.color.primary)
                    ds.isUnderlineText = false
                    ds.isFakeBoldText = true
                }
            }

            spannable.setSpan(
                clickableSpan,
                startIndex,
                startIndex + registerWord.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        binding.tvRegisterPrompt.apply {
            text = spannable
            movementMethod = LinkMovementMethod.getInstance()
            highlightColor = Color.TRANSPARENT
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
