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
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.spark.R
import com.example.spark.SparkApplication
import com.example.spark.data.local.SessionManager
import com.example.spark.databinding.FragmentWelcomeBinding
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels { AuthViewModel.Factory() }
    private var sessionManager: SessionManager? = null

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

        (activity?.application as? SparkApplication)?.let { app ->
            sessionManager = app.sessionManager
        }

        if (sessionManager?.isLoggedIn == true && onLoginClick == null) {
            findNavController().navigate(R.id.action_welcome_to_dashboard)
            return
        }

        setupClickListeners()
        setupRegisterSpannable()
        observeAuthEvents()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            onLoginClick?.invoke() ?: run {
                clearErrors()
                val email = binding.etEmail.text?.toString().orEmpty()
                val password = binding.etPassword.text?.toString().orEmpty()
                authViewModel.login(email, password)
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            onForgotPasswordClick?.invoke() ?: run {
                // Placeholder for password recovery flow
            }
        }
    }

    private fun clearErrors() {
        setError(binding.tilEmail, null)
        setError(binding.tilPassword, null)
    }

    private fun setError(layout: TextInputLayout, errorText: String?) {
        layout.error = errorText
        layout.isErrorEnabled = !errorText.isNullOrEmpty()
    }

    private fun observeAuthEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.uiEvent.collectLatest { event ->
                    when (event) {
                        is AuthViewModel.AuthUiEvent.Success -> {
                            findNavController().navigate(R.id.action_welcome_to_dashboard)
                        }
                        is AuthViewModel.AuthUiEvent.EmailError -> {
                            setError(binding.tilEmail, event.message)
                        }
                        is AuthViewModel.AuthUiEvent.PasswordError -> {
                            setError(binding.tilPassword, event.message)
                        }
                        is AuthViewModel.AuthUiEvent.GeneralError -> {
                            Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                        }
                        is AuthViewModel.AuthUiEvent.Loading -> {
                            // Optional loading state handling
                        }
                        else -> {}
                    }
                }
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
