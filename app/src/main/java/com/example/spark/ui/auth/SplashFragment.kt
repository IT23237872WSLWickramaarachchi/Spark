package com.example.spark.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.spark.R
import com.example.spark.SparkApplication
import com.example.spark.data.local.SessionManager
import com.example.spark.databinding.FragmentSplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    private var sessionManager: SessionManager? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity?.application as? SparkApplication)?.let { app ->
            sessionManager = app.sessionManager
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Small delay to ensure smooth splash rendering and session check work
            delay(600)

            val isLoggedIn = sessionManager?.isLoggedIn == true

            try {
                if (isLoggedIn) {
                    findNavController().navigate(R.id.action_splash_to_dashboard)
                } else {
                    findNavController().navigate(R.id.action_splash_to_welcome)
                }
            } catch (e: Exception) {
                // Fallback navigation if action fails or destination is missing
                try {
                    findNavController().navigate(R.id.welcome)
                } catch (ignored: Exception) {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
