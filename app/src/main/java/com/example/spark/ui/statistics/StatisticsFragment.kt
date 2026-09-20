package com.example.spark.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.spark.MainActivity
import com.example.spark.R
import com.example.spark.data.AppDatabase
import com.example.spark.data.repository.HabitRepository
import com.example.spark.data.repository.MoodRepository
import com.example.spark.databinding.FragmentStatisticsBinding
import com.example.spark.util.SessionManager
import com.example.spark.viewmodel.DayRateUiModel
import com.example.spark.viewmodel.MoodPointUiModel
import com.example.spark.viewmodel.StatisticsUiState
import com.example.spark.viewmodel.StatisticsViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels {
        val context = requireContext().applicationContext
        val db = AppDatabase.getDatabase(context)
        val habitRepo = HabitRepository(db.habitDao(), db.habitCompletionDao())
        val moodRepo = MoodRepository(db.moodEntryDao())
        val userId = SessionManager(context).getCurrentUserId()
        StatisticsViewModel.Factory(habitRepo, moodRepo, userId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTopBar()
        setupSegmentedToggle()
        setupCompletionChart()
        setupMoodChart()
        observeViewModel()
    }

    private fun setupTopBar() {
        binding.btnStatsBack.setOnClickListener {
            (activity as? MainActivity)?.navigateToHome()
        }

        binding.btnStatsBell.setOnClickListener {
            Toast.makeText(requireContext(), "No new notifications", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSegmentedToggle() {
        binding.btnToggleWeek.setOnClickListener {
            updateToggleVisuals(isWeek = true)
            viewModel.setRange(true)
        }

        binding.btnToggleMonth.setOnClickListener {
            updateToggleVisuals(isWeek = false)
            viewModel.setRange(false)
        }
    }

    private fun updateToggleVisuals(isWeek: Boolean) {
        val activeTextColor = ContextCompat.getColor(requireContext(), R.color.white)
        val inactiveTextColor = ContextCompat.getColor(requireContext(), R.color.primary_deep_purple)

        if (isWeek) {
            binding.btnToggleWeek.setBackgroundResource(R.drawable.bg_stats_toggle_active)
            binding.btnToggleWeek.setTextColor(activeTextColor)
            binding.btnToggleMonth.setBackgroundResource(android.R.color.transparent)
            binding.btnToggleMonth.setTextColor(inactiveTextColor)
        } else {
            binding.btnToggleMonth.setBackgroundResource(R.drawable.bg_stats_toggle_active)
            binding.btnToggleMonth.setTextColor(activeTextColor)
            binding.btnToggleWeek.setBackgroundResource(android.R.color.transparent)
            binding.btnToggleWeek.setTextColor(inactiveTextColor)
        }
    }

    private fun setupCompletionChart() {
        binding.chartCompletion.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(false)
            isHighlightFullBarEnabled = false
            setPinchZoom(false)
            setScaleEnabled(false)
            isDoubleTapToZoomEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(true)
                axisLineColor = ContextCompat.getColor(context, R.color.surface_variant)
                axisLineWidth = 1.2f
                textColor = ContextCompat.getColor(context, R.color.outline)
                textSize = 12f
                granularity = 1f
                isGranularityEnabled = true
            }

            axisLeft.apply {
                setDrawGridLines(false)
                setDrawAxisLine(false)
                setDrawLabels(false)
                axisMinimum = 0f
                axisMaximum = 100f
            }

            axisRight.isEnabled = false
        }
    }

    private fun setupMoodChart() {
        binding.chartMoodTrend.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setPinchZoom(false)
            setScaleEnabled(false)
            isDoubleTapToZoomEnabled = false

            xAxis.apply {
                setDrawGridLines(false)
                setDrawAxisLine(false)
                setDrawLabels(false)
            }

            axisLeft.apply {
                setDrawGridLines(false)
                setDrawAxisLine(false)
                setDrawLabels(false)
                axisMinimum = 0.5f
                axisMaximum = 5.5f
            }

            axisRight.isEnabled = false
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }
    }

    private fun renderState(state: StatisticsUiState) {
        // Percentage header
        binding.tvCompletionPercentage.text = "${state.completionPercentage}%"

        // Mood trend badge
        binding.tvMoodTrendBadge.text = state.moodTrendLabel
        updateMoodBadgeColor(state.moodTrendLabel)

        // Streak summary card
        binding.tvStreakDaysCount.text = "${state.currentStreak}"

        // Today rate card
        binding.tvTodayRateCompleted.text = "${state.todayCompletedCount}"
        binding.tvTodayRateTotal.text = " /${state.todayTotalCount}"

        // Completion Chart
        updateCompletionChart(state.dailyRates, state.isWeek)

        // Mood Chart
        updateMoodChart(state.moodTrendPoints)

        // Mood progressive note
        if (state.moodProgressiveNote.isNotEmpty()) {
            binding.tvMoodProgressiveNote.text = state.moodProgressiveNote
            binding.tvMoodProgressiveNote.visibility = View.VISIBLE
        } else {
            binding.tvMoodProgressiveNote.visibility = View.GONE
        }

        // Common Feelings
        updateCommonFeelings(state.topMoodTags)

        // Habit & Mood insight
        binding.tvHabitMoodInsight.text = state.habitMoodInsight
    }

    private fun updateCommonFeelings(topTags: List<com.example.spark.viewmodel.TagCountUiModel>) {
        binding.chipGroupCommonFeelings.removeAllViews()
        if (topTags.isEmpty()) {
            binding.chipGroupCommonFeelings.visibility = View.GONE
            binding.tvFeelingsEmpty.visibility = View.VISIBLE
        } else {
            binding.tvFeelingsEmpty.visibility = View.GONE
            binding.chipGroupCommonFeelings.visibility = View.VISIBLE
            val density = resources.displayMetrics.density
            topTags.forEach { item ->
                val daySuffix = if (item.count == 1) "day" else "days"
                val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                    text = "${item.name} • ${item.count} $daySuffix"
                    isClickable = false
                    isCheckable = false
                    setChipBackgroundColorResource(R.color.chip_bg)
                    setTextColor(ContextCompat.getColor(context, R.color.chip_text))
                    chipStrokeWidth = 0f
                    textSize = 13f
                    shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                        .setAllCornerSizes(density * 999f)
                        .build()
                    chipStartPadding = density * 14f
                    chipEndPadding = density * 14f
                    chipMinHeight = density * 36f
                }
                binding.chipGroupCommonFeelings.addView(chip)
            }
        }
    }

    private fun updateMoodBadgeColor(label: String) {
        val (badgeColor, textColor) = when (label) {
            "Positive" -> Pair(Color.parseColor("#26874D5E"), Color.parseColor("#D44A68"))
            "Neutral" -> Pair(Color.parseColor("#2652559C"), ContextCompat.getColor(requireContext(), R.color.primary))
            else -> Pair(Color.parseColor("#1F777681"), ContextCompat.getColor(requireContext(), R.color.outline))
        }
        binding.ivTrendIcon.setColorFilter(textColor)
        binding.tvMoodTrendBadge.setTextColor(textColor)
    }

    private fun updateCompletionChart(dailyRates: List<DayRateUiModel>, isWeek: Boolean) {
        if (dailyRates.isEmpty()) {
            binding.chartCompletion.clear()
            binding.tvCompletionEmpty.visibility = View.VISIBLE
            return
        }
        binding.tvCompletionEmpty.visibility = View.GONE

        val entries = ArrayList<BarEntry>()
        val colors = ArrayList<Int>()
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary_container)
        val todayActiveColor = ContextCompat.getColor(requireContext(), R.color.primary_deep_purple)

        dailyRates.forEachIndexed { index, dayRate ->
            // Minimal visible bar height (at least 2f) if rate is zero, so bars appear neat on baseline
            val displayValue = if (dayRate.rate > 0f) dayRate.rate else 0f
            entries.add(BarEntry(index.toFloat(), displayValue))
            colors.add(if (dayRate.isToday) todayActiveColor else primaryColor)
        }

        val dataSet = BarDataSet(entries, "Completion").apply {
            this.colors = colors
            setDrawValues(false)
            highLightColor = todayActiveColor
        }

        val barData = BarData(dataSet).apply {
            barWidth = if (isWeek) 0.38f else 0.60f
        }

        binding.chartCompletion.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(
                if (isWeek) dailyRates.map { it.dayLetter }
                else dailyRates.mapIndexed { idx, item -> if (idx % 5 == 0) item.dayLetter else "" }
            )
            xAxis.labelCount = if (isWeek) 7 else 6
            data = barData
            animateY(400)
            invalidate()
        }
    }

    private fun updateMoodChart(points: List<MoodPointUiModel>) {
        if (points.isEmpty()) {
            binding.chartMoodTrend.clear()
            binding.tvMoodEmpty.visibility = View.VISIBLE
            return
        }
        binding.tvMoodEmpty.visibility = View.GONE

        val entries = points.map { Entry(it.xIndex, it.moodLevel) }
        val pinkColor = Color.parseColor("#F68D9D")
        val surfaceLowest = ContextCompat.getColor(requireContext(), R.color.surface_container_lowest)

        val lineDataSet = LineDataSet(entries, "Mood").apply {
            mode = LineDataSet.Mode.CUBIC_BEZIER
            color = pinkColor
            lineWidth = 3.5f
            setDrawCircles(true)
            circleRadius = 6f
            setCircleColor(pinkColor)
            circleHoleColor = surfaceLowest
            circleHoleRadius = 3.5f
            setDrawValues(false)
            setDrawFilled(false)
            isHighlightEnabled = false
        }

        binding.chartMoodTrend.apply {
            data = LineData(lineDataSet)
            animateX(500)
            invalidate()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshStreak()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): StatisticsFragment = StatisticsFragment()
    }
}
