package com.example.spark.ui.mood

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.spark.R
import com.example.spark.data.AppDatabase
import com.example.spark.data.repository.MoodRepository
import com.example.spark.databinding.DialogMoodCheckinBinding
import com.example.spark.util.DateUtils
import com.example.spark.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MoodCheckInDialog : DialogFragment() {

    private var _binding: DialogMoodCheckinBinding? = null
    private val binding get() = _binding!!

    private var selectedLevel = 4 // Default "Good" matching reference screenshot
    private var onMoodSavedListener: ((Int) -> Unit)? = null

    fun setOnMoodSavedListener(listener: (Int) -> Unit) {
        this.onMoodSavedListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogMoodCheckinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize default selection visual
        updateMoodSelection(selectedLevel)

        val commonChips = mapOf(
            "Calm" to binding.chipTagCalm,
            "Energetic" to binding.chipTagEnergetic,
            "Focused" to binding.chipTagFocused,
            "Tired" to binding.chipTagTired,
            "Stressed" to binding.chipTagStressed,
            "Anxious" to binding.chipTagAnxious
        )

        commonChips.forEach { (tagName, chip) ->
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedTags.add(tagName)
                } else {
                    selectedTags.remove(tagName)
                }
            }
        }

        binding.chipMoreTags.setOnClickListener {
            showAllTagsDialog(commonChips)
        }

        // Load today's existing entry if present
        loadTodayMoodIfExists(commonChips)

        binding.ivMood1.setOnClickListener { updateMoodSelection(1) }
        binding.ivMood2.setOnClickListener { updateMoodSelection(2) }
        binding.ivMood3.setOnClickListener { updateMoodSelection(3) }
        binding.ivMood4.setOnClickListener { updateMoodSelection(4) }
        binding.ivMood5.setOnClickListener { updateMoodSelection(5) }

        binding.tvCloseMood.setOnClickListener { dismiss() }
        binding.btnSkipMood.setOnClickListener { dismiss() }

        binding.btnSaveMood.setOnClickListener {
            val note = binding.etMoodNote.text?.toString()?.trim()
            saveMood(selectedLevel, if (note.isNullOrEmpty()) null else note)
        }
    }

    private val selectedTags = mutableSetOf<String>()

    private fun showAllTagsDialog(commonChips: Map<String, com.google.android.material.chip.Chip>) {
        val allTags = com.example.spark.model.MoodTags.ALL_NAMES
        val checkedItems = BooleanArray(allTags.size) { i ->
            selectedTags.contains(allTags[i])
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Feelings")
            .setMultiChoiceItems(allTags.toTypedArray(), checkedItems) { _, which, isChecked ->
                val tag = allTags[which]
                if (isChecked) {
                    selectedTags.add(tag)
                } else {
                    selectedTags.remove(tag)
                }
            }
            .setPositiveButton("Done") { _, _ ->
                // Sync common chips
                commonChips.forEach { (tagName, chip) ->
                    chip.isChecked = selectedTags.contains(tagName)
                }
                // Add any non-common tags as visible chips in group if selected
                syncExtraTagChips()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun syncExtraTagChips() {
        val commonNames = setOf("Calm", "Energetic", "Focused", "Tired", "Stressed", "Anxious")
        val extraSelected = selectedTags.filter { it !in commonNames }

        // Remove any previously added dynamic extra chips
        val toRemove = mutableListOf<View>()
        for (i in 0 until binding.chipGroupMoodTags.childCount) {
            val child = binding.chipGroupMoodTags.getChildAt(i)
            if (child.tag == "EXTRA_TAG") {
                toRemove.add(child)
            }
        }
        toRemove.forEach { binding.chipGroupMoodTags.removeView(it) }

        // Insert new chips before chipMoreTags
        val moreChipIndex = binding.chipGroupMoodTags.indexOfChild(binding.chipMoreTags)
        extraSelected.forEach { tagName ->
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = tagName
                isCheckable = true
                isChecked = true
                tag = "EXTRA_TAG"
                setChipBackgroundColorResource(R.color.selector_chip_background)
                setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.selector_chip_text))
                setOnCheckedChangeListener { _, isChecked ->
                    if (!isChecked) {
                        selectedTags.remove(tagName)
                        binding.chipGroupMoodTags.removeView(this)
                    }
                }
            }
            if (moreChipIndex >= 0) {
                binding.chipGroupMoodTags.addView(chip, moreChipIndex)
            } else {
                binding.chipGroupMoodTags.addView(chip)
            }
        }
    }

    private fun loadTodayMoodIfExists(commonChips: Map<String, com.google.android.material.chip.Chip>) {
        val ctx = context ?: return
        val sessionManager = SessionManager(ctx)
        val userId = sessionManager.getCurrentUserId()
        if (userId <= 0) return

        val db = AppDatabase.getDatabase(ctx)
        val repository = MoodRepository(db.moodEntryDao())
        val today = DateUtils.getTodayDateString()

        lifecycleScope.launch {
            val existing = withContext(Dispatchers.IO) {
                repository.getMoodForDate(userId, today)
            }
            if (existing != null && _binding != null) {
                selectedLevel = existing.moodLevel
                updateMoodSelection(existing.moodLevel)
                if (!existing.note.isNullOrEmpty()) {
                    binding.etMoodNote.setText(existing.note)
                }
                val tagsList = existing.getTagsList()
                selectedTags.clear()
                selectedTags.addAll(tagsList)
                commonChips.forEach { (tagName, chip) ->
                    chip.isChecked = selectedTags.contains(tagName)
                }
                syncExtraTagChips()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun updateMoodSelection(level: Int) {
        selectedLevel = level
        val icons = listOf(
            binding.ivMood1,
            binding.ivMood2,
            binding.ivMood3,
            binding.ivMood4,
            binding.ivMood5
        )

        icons.forEachIndexed { index, iv ->
            val iconLevel = index + 1
            if (iconLevel == level) {
                iv.setBackgroundResource(R.drawable.bg_mood_item_selected_halo)
                iv.setColorFilter(ContextCompat.getColor(requireContext(), R.color.secondary_container))
                iv.scaleX = 1.15f
                iv.scaleY = 1.15f
                iv.elevation = 6f
            } else {
                iv.setBackgroundResource(R.drawable.bg_circle_arrow)
                iv.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary))
                iv.scaleX = 1.0f
                iv.scaleY = 1.0f
                iv.elevation = 0f
            }
        }
    }

    private fun saveMood(level: Int, note: String?) {
        val context = context ?: return
        val sessionManager = SessionManager(context)
        val userId = sessionManager.getCurrentUserId()
        if (userId <= 0) {
            Toast.makeText(context, "User session not found", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        val db = AppDatabase.getDatabase(context)
        val repository = MoodRepository(db.moodEntryDao())
        val today = DateUtils.getTodayDateString()
        val tagsString = com.example.spark.data.entity.MoodEntryEntity.joinTags(selectedTags.toList())

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                repository.saveMood(userId, today, level, note, tags = tagsString)
            }
            val feedbackMsg = com.example.spark.util.MoodMessageProvider.getFeedbackMessage(level, selectedTags.toList())
            Toast.makeText(context, feedbackMsg, Toast.LENGTH_LONG).show()
            onMoodSavedListener?.invoke(level)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "MoodCheckInDialog"

        fun newInstance(): MoodCheckInDialog {
            return MoodCheckInDialog()
        }
    }
}
