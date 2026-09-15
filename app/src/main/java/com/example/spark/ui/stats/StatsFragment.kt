package com.example.spark.ui.stats

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.spark.R
import com.example.spark.SparkApplication
import com.example.spark.databinding.FragmentStatsBinding
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.launch

/**
 * Fragment displaying habit completion statistics, mood trend line chart,
 * and streak metrics with Week/Month period toggle, reactively connected to [StatsViewModel].
 */
class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: StatsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as SparkApplication
        viewModel = ViewModelProvider(
            this,
            StatsViewModel.Factory(app.habitRepository, app.moodRepository)
        )[StatsViewModel::class.java]

        setupToolbar()
        setupToggleGroup()
        setupMoodLineChart()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.ibToolbarBack.setOnClickListener {
            if (!findNavController().navigateUp()) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        binding.ibToolbarBell.setOnClickListener {
            // Mindful notifications hook
        }
    }

    private fun setupToggleGroup() {
        binding.toggleGroupPeriod.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val newPeriod = when (checkedId) {
                    R.id.btnPeriodWeek -> Period.WEEK
                    R.id.btnPeriodMonth -> Period.MONTH
                    else -> Period.WEEK
                }
                viewModel.onPeriodChange(newPeriod)
            }
        }
    }

    private fun setupMoodLineChart() {
        binding.lineChartMood.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setDragEnabled(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)

            xAxis.apply {
                isEnabled = false
                setDrawGridLines(false)
                setDrawAxisLine(false)
            }

            axisLeft.apply {
                isEnabled = false
                setDrawGridLines(false)
                setDrawAxisLine(false)
                axisMinimum = 1f
                axisMaximum = 6f
            }

            axisRight.apply {
                isEnabled = false
                setDrawGridLines(false)
                setDrawAxisLine(false)
            }

            setViewPortOffsets(32f, 24f, 32f, 24f)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: StatsUiState) {
        // Update toggle group selection without re-triggering listener loops
        val targetButtonId = if (state.period == Period.WEEK) R.id.btnPeriodWeek else R.id.btnPeriodMonth
        if (binding.toggleGroupPeriod.checkedButtonId != targetButtonId) {
            binding.toggleGroupPeriod.check(targetButtonId)
        }

        // Completion Card
        binding.tvCompletionRate.text = "${state.completionPercent}%"
        binding.tvCompletionSubtitle.text = state.completionSubtitle

        // Day Indicators & Bars
        val dayIndicators = listOf(
            binding.dayIndicator0,
            binding.dayIndicator1,
            binding.dayIndicator2,
            binding.dayIndicator3,
            binding.dayIndicator4,
            binding.dayIndicator5,
            binding.dayIndicator6
        )
        val dayBars = listOf(
            binding.barDay0,
            binding.barDay1,
            binding.barDay2,
            binding.barDay3,
            binding.barDay4,
            binding.barDay5,
            binding.barDay6
        )
        val dayLabels = listOf(
            binding.tvDay0,
            binding.tvDay1,
            binding.tvDay2,
            binding.tvDay3,
            binding.tvDay4,
            binding.tvDay5,
            binding.tvDay6
        )

        state.dayStats.forEachIndexed { index, dayStat ->
            if (index < dayIndicators.size) {
                dayIndicators[index].setStatus(dayStat.status)

                val barHeightDp = (12 + (dayStat.completionPercent * 1.08f)).toInt()
                setBarHeight(dayBars[index], barHeightDp)

                val barBg = if (dayStat.completionPercent > 0) {
                    R.drawable.bg_chart_bar_primary
                } else {
                    R.drawable.bg_chart_bar_variant
                }
                dayBars[index].setBackgroundResource(barBg)

                val labelColor = if (dayStat.isToday) {
                    ContextCompat.getColor(requireContext(), R.color.primary)
                } else {
                    ContextCompat.getColor(requireContext(), R.color.outline)
                }
                dayLabels[index].setTextColor(labelColor)
            }
        }

        // Mood Chart & Trend Pill
        updateMoodChart(state)

        // Bottom Stats
        binding.tvStreakValue.text = "${state.currentStreak}"
        binding.tvStreakUnit.text = getString(R.string.stats_days_unit)
        binding.tvTodayRateValue.text = "${state.todayCompletedCount}"
        binding.tvTodayRateTotal.text = " /${state.todayTotalCount}"
    }

    /**
     * Updates the mood trend line chart.
     * Re-uses the existing LineDataSet and notifies changes rather than re-creating
     * the dataset from scratch, avoiding animation flickers on recomposition.
     */
    private fun updateMoodChart(state: StatsUiState) {
        val entries = state.moodPoints.map { Entry(it.x, it.y) }
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary)
        val chart = binding.lineChartMood

        val lineData = chart.data
        if (lineData != null && lineData.dataSetCount > 0) {
            val dataSet = lineData.getDataSetByIndex(0) as LineDataSet
            dataSet.values = entries
            lineData.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.invalidate()
        } else {
            val dataSet = LineDataSet(entries, "Mood Trend").apply {
                mode = LineDataSet.Mode.CUBIC_BEZIER
                cubicIntensity = 0.22f
                color = primaryColor
                lineWidth = 3.5f

                setDrawCircles(true)
                circleRadius = 5.5f
                circleHoleRadius = 2.5f
                setCircleColor(primaryColor)
                circleHoleColor = Color.WHITE

                setDrawValues(false)
                setDrawHighlightIndicators(false)
                isHighlightEnabled = false
            }
            chart.data = LineData(dataSet)
            chart.invalidate()
        }

        // Trend Pill
        binding.tvTrendStatus.text = state.moodTrendStatus
        binding.tvMoodTrendSubtitle.text = if (state.period == Period.WEEK) {
            getString(R.string.stats_mood_trend_sublabel)
        } else {
            "Past 30 days"
        }
    }

    private fun setBarHeight(view: View, heightDp: Int) {
        val heightPx = (heightDp * resources.displayMetrics.density).toInt()
        val params = view.layoutParams
        params.height = heightPx
        view.layoutParams = params
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
