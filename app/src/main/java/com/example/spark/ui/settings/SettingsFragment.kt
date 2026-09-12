package com.example.spark.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.spark.R
import com.example.spark.databinding.FragmentSettingsBinding
import com.example.spark.databinding.ItemSettingsRowBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Locale

/**
 * SettingsFragment displaying user profile, preferences, about info,
 * and session logout action matching the Spark design tokens.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupAccountSection()
        setupPreferencesSection()
        setupAboutSection()
        setupLogOutButton()
    }

    private fun setupToolbar() {
        binding.ibToolbarBack.setOnClickListener {
            if (!findNavController().navigateUp()) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        binding.ibToolbarBell.setOnClickListener {
            // Notifications hub action
        }
    }

    private fun setupAccountSection() {
        binding.cardProfile.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.settings_profile_name), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupPreferencesSection() {
        // Row 1: Notifications (SwitchMaterial)
        setupRow(
            rowBinding = binding.rowNotifications,
            iconRes = R.drawable.ic_bell,
            label = getString(R.string.settings_notifications)
        ) {
            switchRow.visibility = View.VISIBLE
            switchRow.isChecked = true
            switchRow.setOnCheckedChangeListener { _, isChecked ->
                onNotificationsToggled(isChecked)
            }
        }

        // Row 2: Dark Mode (SwitchMaterial, disabled/no-op - values-night doesn't exist yet)
        setupRow(
            rowBinding = binding.rowDarkMode,
            iconRes = R.drawable.ic_moon,
            label = getString(R.string.settings_dark_mode)
        ) {
            switchRow.visibility = View.VISIBLE
            switchRow.isChecked = false
            switchRow.setOnClickListener {
                switchRow.isChecked = false
                Toast.makeText(
                    requireContext(),
                    R.string.settings_dark_mode_notice,
                    Toast.LENGTH_LONG
                ).show()
            }
            root.setOnClickListener {
                Toast.makeText(
                    requireContext(),
                    R.string.settings_dark_mode_notice,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Row 3: Reminder Time (Value + Chevron)
        setupRow(
            rowBinding = binding.rowReminderTime,
            iconRes = R.drawable.ic_clock,
            label = getString(R.string.settings_reminder_time)
        ) {
            layoutValueChevron.visibility = View.VISIBLE
            tvRowValue.text = getString(R.string.settings_reminder_time_value)
            root.setOnClickListener {
                showReminderTimePicker()
            }
        }

        // Row 4: Language (Value + Chevron)
        setupRow(
            rowBinding = binding.rowLanguage,
            iconRes = R.drawable.ic_globe,
            label = getString(R.string.settings_language)
        ) {
            layoutValueChevron.visibility = View.VISIBLE
            tvRowValue.text = getString(R.string.settings_language_value)
            root.setOnClickListener {
                onLanguageClicked()
            }
        }
    }

    private fun setupAboutSection() {
        // Row 5: Help Center (Chevron only)
        setupRow(
            rowBinding = binding.rowHelpCenter,
            iconRes = R.drawable.ic_help,
            label = getString(R.string.settings_help_center)
        ) {
            ivOnlyChevron.visibility = View.VISIBLE
            root.setOnClickListener {
                Toast.makeText(requireContext(), getString(R.string.settings_help_center), Toast.LENGTH_SHORT).show()
            }
        }

        // Row 6: Privacy Policy (Chevron only)
        setupRow(
            rowBinding = binding.rowPrivacyPolicy,
            iconRes = R.drawable.ic_privacy_tip,
            label = getString(R.string.settings_privacy_policy)
        ) {
            ivOnlyChevron.visibility = View.VISIBLE
            root.setOnClickListener {
                Toast.makeText(requireContext(), getString(R.string.settings_privacy_policy), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupLogOutButton() {
        binding.btnLogOut.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_log_out)
                .setMessage("Are you sure you want to log out of Spark?")
                .setPositiveButton(R.string.settings_log_out) { _, _ ->
                    onLogOutConfirmed()
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }
    }

    private inline fun setupRow(
        rowBinding: ItemSettingsRowBinding,
        @DrawableRes iconRes: Int,
        label: String,
        configure: ItemSettingsRowBinding.() -> Unit
    ) {
        rowBinding.ivRowIcon.setImageResource(iconRes)
        rowBinding.ivRowIcon.setColorFilter(
            ContextCompat.getColor(requireContext(), R.color.primary)
        )
        rowBinding.tvRowLabel.text = label
        rowBinding.configure()
    }

    private fun onNotificationsToggled(enabled: Boolean) {
        // Stub callback for notification preference
    }

    private fun showReminderTimePicker() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(8)
            .setMinute(0)
            .setTitleText(R.string.settings_reminder_time)
            .build()

        picker.addOnPositiveButtonClickListener {
            val formattedMinute = String.format(Locale.getDefault(), "%02d", picker.minute)
            val period = if (picker.hour >= 12) "PM" else "AM"
            val displayHour = when {
                picker.hour == 0 -> 12
                picker.hour > 12 -> picker.hour - 12
                else -> picker.hour
            }
            binding.rowReminderTime.tvRowValue.text = "$displayHour:$formattedMinute $period"
        }

        picker.show(parentFragmentManager, "SettingsReminderTimePicker")
    }

    private fun onLanguageClicked() {
        val languages = arrayOf("English", "Spanish", "French", "German")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_language)
            .setItems(languages) { _, which ->
                binding.rowLanguage.tvRowValue.text = languages[which]
            }
            .show()
    }

    private fun onLogOutConfirmed() {
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
