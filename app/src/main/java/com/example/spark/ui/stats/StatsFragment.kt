package com.example.spark.ui.stats

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.spark.R
import com.example.spark.databinding.FragmentStatsBinding
import com.example.spark.ui.custom.DayStatus
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

/**
 * Fragment displaying habit completion statistics, mood trend line chart,
 * and streak metrics with Week/Month period toggle.
 */
class StatsFragment : Fragment() {

    enum class Period {
        WEEK,
        MONTH
    }

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private var currentPeriod: Period = Period.WEEK

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

        setupToolbar()
        setupToggleGroup()
        setupMoodLineChart()
        reloadData(Period.WEEK)
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
                if (newPeriod != currentPeriod) {
                    currentPeriod = newPeriod
                    reloadData(newPeriod)
                }
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

            // Offset edges for padding circles
            setViewPortOffsets(32f, 24f, 32f, 24f)
        }
    }

    private fun reloadData(period: Period) {
        updateCompletionCard(period)
        updateMoodChart(period)
        updateBottomStats(period)
    }

    private fun updateCompletionCard(period: Period) {
        when (period) {
            Period.WEEK -> {
                binding.tvCompletionRate.text = getString(R.string.stats_completion_percentage_default)

                // Set Day Indicators using DayIndicatorView from Habit Detail
                binding.dayIndicator0.setStatus(DayStatus.COMPLETED)
                binding.dayIndicator1.setStatus(DayStatus.COMPLETED)
                binding.dayIndicator2.setStatus(DayStatus.MISSED)
                binding.dayIndicator3.setStatus(DayStatus.COMPLETED)
                binding.dayIndicator4.setStatus(DayStatus.TODAY)
                binding.dayIndicator5.setStatus(DayStatus.FUTURE)
                binding.dayIndicator6.setStatus(DayStatus.FUTURE)

                // Day Labels
                binding.tvDay4.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))

                // Bar Heights (60%, 80%, 40%, 90%, 100%, 20%, 10%)
                setBarHeight(binding.barDay0, 72)
                setBarHeight(binding.barDay1, 96)
                setBarHeight(binding.barDay2, 48)
                setBarHeight(binding.barDay3, 108)
                setBarHeight(binding.barDay4, 120)
                setBarHeight(binding.barDay5, 24)
                setBarHeight(binding.barDay6, 12)

                binding.barDay0.setBackgroundResource(R.drawable.bg_chart_bar_primary)
                binding.barDay1.setBackgroundResource(R.drawable.bg_chart_bar_primary)
                binding.barDay2.setBackgroundResource(R.drawable.bg_chart_bar_primary)
                binding.barDay3.setBackgroundResource(R.drawable.bg_chart_bar_primary)
                binding.barDay4.setBackgroundResource(R.drawable.bg_chart_bar_primary)
                binding.barDay5.setBackgroundResource(R.drawable.bg_chart_bar_variant)
                binding.barDay6.setBackgroundResource(R.drawable.bg_chart_bar_variant)
            }
            Period.MONTH -> {
                binding.tvCompletionRate.text = "78%"

                // Month aggregation representation
                binding.dayIndicator0.setStatus(DayStatus.COMPLETED)
                binding.dayIndicator1.setStatus(DayStatus.COMPLETED)
                binding.dayIndicator2.setStatus(DayStatus.COMPLETED)
                binding.dayIndicator3.setStatus(DayStatus.COMPLETED)
                binding.dayIndicator4.setStatus(DayStatus.COMPLETED)
                binding.dayIndicator5.setStatus(DayStatus.COMPLETED)
                binding.dayIndicator6.setStatus(DayStatus.MISSED)

                setBarHeight(binding.barDay0, 92)
                setBarHeight(binding.barDay1, 104)
                setBarHeight(binding.barDay2, 86)
                setBarHeight(binding.barDay3, 100)
                setBarHeight(binding.barDay4, 114)
                setBarHeight(binding.barDay5, 80)
                setBarHeight(binding.barDay6, 44)

                binding.barDay0.setBackgroundResource(R.drawable.bg_chart_bar_primary)
                binding.barDay1.setBackgroundResource(R.drawable.bg_chart_bar_primary)
                binding.barDay2.setBackgroundResource(R.drawable.bg_chart_bar_primary)
                binding.barDay3.setBackgroundResource(R.drawable.bg_chart_bar_primary)
                binding.barDay4.setBackgroundResource(R.drawable.bg_chart_bar_primary)
                binding.barDay5.setBackgroundResource(R.drawable.bg_chart_bar_primary)
                binding.barDay6.setBackgroundResource(R.drawable.bg_chart_bar_primary)
            }
        }
    }

    private fun updateMoodChart(period: Period) {
        val entries = when (period) {
            Period.WEEK -> listOf(
                Entry(0f, 2.8f),
                Entry(1f, 4.4f),
                Entry(2f, 3.2f),
                Entry(3f, 4.7f)
            )
            Period.MONTH -> listOf(
                Entry(0f, 3.0f),
                Entry(1f, 3.8f),
                Entry(2f, 3.2f),
                Entry(3f, 4.2f),
                Entry(4f, 4.0f),
                Entry(5f, 4.8f)
            )
        }

        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary)
        val secondaryColor = ContextCompat.getColor(requireContext(), R.color.secondary)

        val dataSet = LineDataSet(entries, "Mood Trend").apply {
            // Cubic Bezier curve creates smooth rolling-average look without raw jagged points
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.22f
            color = primaryColor
            lineWidth = 3.5f

            // Data point dots
            setDrawCircles(true)
            circleRadius = 5.5f
            circleHoleRadius = 2.5f
            setCircleColor(primaryColor)
            circleHoleColor = Color.WHITE

            setDrawValues(false)
            setDrawHighlightIndicators(false)
            isHighlightEnabled = false
        }

        val lineData = LineData(dataSet)
        binding.lineChartMood.data = lineData
        binding.lineChartMood.animateY(500)
        binding.lineChartMood.invalidate()
    }

    private fun updateBottomStats(period: Period) {
        when (period) {
            Period.WEEK -> {
                binding.tvStreakValue.text = getString(R.string.stats_streak_value_default)
                binding.tvStreakUnit.text = getString(R.string.stats_days_unit)
                binding.tvTodayRateValue.text = getString(R.string.stats_today_rate_default)
                binding.tvTodayRateTotal.text = getString(R.string.stats_today_rate_total)
            }
            Period.MONTH -> {
                binding.tvStreakValue.text = "18"
                binding.tvStreakUnit.text = getString(R.string.stats_days_unit)
                binding.tvTodayRateValue.text = "22"
                binding.tvTodayRateTotal.text = " /28"
            }
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
