package com.example.spark.ui.mood

import android.app.Dialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.spark.R
import com.example.spark.SparkApplication
import com.example.spark.databinding.FragmentMoodCheckinBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

/**
 * BottomSheetDialogFragment for the daily mood check-in.
 *
 * Implements FR-06: Daily mood log on scale 1–5 with optional note.
 * Features animated halo selection state on mood face icons and shame-free skip.
 */
class MoodCheckInDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentMoodCheckinBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MoodCheckInViewModel
    private lateinit var moodViews: List<ImageView>

    override fun getTheme(): Int = R.style.Widget_Spark_BottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoodCheckinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as SparkApplication
        viewModel = ViewModelProvider(
            this,
            MoodCheckInViewModel.Factory(app.moodRepository)
        )[MoodCheckInViewModel::class.java]

        // Apply window insets to prevent top elements (tvClose, dragHandle) from being covered by system bars / status bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.moodSheetRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top)
            insets
        }

        moodViews = listOf(
            binding.ivMood1,
            binding.ivMood2,
            binding.ivMood3,
            binding.ivMood4,
            binding.ivMood5
        )

        setupDismissActions()
        setupMoodSelectors()
        setupNoteInput()
        setupSaveButton()
        observeViewModel()
    }

    private fun setupDismissActions() {
        binding.tvClose.setOnClickListener {
            viewModel.skip()
        }

        binding.tvSkip.setOnClickListener {
            viewModel.skip()
        }
    }

    private fun setupMoodSelectors() {
        moodViews.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                viewModel.onMoodSelect(index)
            }
        }
    }

    private fun setupNoteInput() {
        binding.etMoodNote.doAfterTextChanged { text ->
            viewModel.onNoteChange(text?.toString().orEmpty())
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveMood.setOnClickListener {
            viewModel.saveMood()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    var previousIndex: Int? = null
                    viewModel.formState.collect { state ->
                        binding.btnSaveMood.isEnabled = state.isSaveEnabled
                        if (state.selectedMoodIndex != previousIndex) {
                            animateMoodSelection(previousIndex, state.selectedMoodIndex)
                            previousIndex = state.selectedMoodIndex
                        }
                    }
                }
                launch {
                    viewModel.dismissEvent.collect {
                        dismiss()
                    }
                }
            }
        }
    }

    private fun animateMoodSelection(previousIndex: Int?, newIndex: Int?) {
        val selectedTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.on_secondary_container))
        val defaultTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.on_surface_variant))

        // Reset previous selected mood
        if (previousIndex != null && previousIndex in moodViews.indices) {
            val prevView = moodViews[previousIndex]
            prevView.setBackgroundResource(R.drawable.bg_mood_item_unselected)
            prevView.imageTintList = defaultTint
            prevView.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(180)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }

        // Animate newly selected mood with halo ring & scale
        if (newIndex != null && newIndex in moodViews.indices) {
            val newView = moodViews[newIndex]
            newView.setBackgroundResource(R.drawable.bg_mood_item_selected_halo)
            newView.imageTintList = selectedTint
            newView.animate()
                .scaleX(1.25f)
                .scaleY(1.25f)
                .setDuration(220)
                .setInterpolator(OvershootInterpolator(2.0f))
                .start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "MoodCheckInDialogFragment"
        const val REQUEST_KEY = "request_mood_checkin"
        const val EXTRA_MOOD_SCORE = "extra_mood_score"
        const val EXTRA_MOOD_NOTE = "extra_mood_note"

        fun newInstance(): MoodCheckInDialogFragment = MoodCheckInDialogFragment()
    }
}
