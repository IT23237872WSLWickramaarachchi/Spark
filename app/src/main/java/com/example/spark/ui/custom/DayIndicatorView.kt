package com.example.spark.ui.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.spark.R

enum class DayStatus {
    COMPLETED,
    MISSED,
    TODAY,
    FUTURE
}

/**
 * Custom View for displaying daily completion status in the "This Week" row.
 *
 * Implements an enum-driven [setStatus] method supporting:
 * - [DayStatus.COMPLETED]: Solid emerald green with white checkmark.
 * - [DayStatus.TODAY]: Dashed primary outline with a centered indicator dot.
 * - [DayStatus.MISSED]: Subtle outline with neutral/dimmed styling.
 * - [DayStatus.FUTURE]: Soft lavender pill with 50% opacity.
 */
class DayIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var status: DayStatus = DayStatus.FUTURE
        private set

    private val density = resources.displayMetrics.density

    private val strokeWidthPx = 2f * density
    private val checkStrokeWidthPx = 2.2f * density

    private val completedColor = ContextCompat.getColor(context, R.color.tertiary_container)
    private val todayColor = ContextCompat.getColor(context, R.color.primary)
    private val futureColor = ContextCompat.getColor(context, R.color.surface_variant)
    private val outlineVariantColor = ContextCompat.getColor(context, R.color.outline_variant)
    private val whiteColor = Color.WHITE

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
    }

    private val dashedStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        pathEffect = DashPathEffect(floatArrayOf(4f * density, 3f * density), 0f)
        color = todayColor
    }

    private val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = checkStrokeWidthPx
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = whiteColor
    }

    private val checkPath = Path()

    init {
        if (isInEditMode) {
            status = DayStatus.COMPLETED
        }
    }

    fun setStatus(status: DayStatus) {
        this.status = status
        contentDescription = when (status) {
            DayStatus.COMPLETED -> "Completed"
            DayStatus.TODAY -> "Today"
            DayStatus.MISSED -> "Missed"
            DayStatus.FUTURE -> "Upcoming"
        }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultSize = (32 * density).toInt()
        val width = resolveSize(defaultSize, widthMeasureSpec)
        val height = resolveSize(defaultSize, heightMeasureSpec)
        val size = minOf(width, height)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = minOf(width, height).toFloat()
        val radius = size / 2f
        val cx = width / 2f
        val cy = height / 2f

        when (status) {
            DayStatus.COMPLETED -> {
                alpha = 1f
                fillPaint.color = completedColor
                canvas.drawCircle(cx, cy, radius, fillPaint)

                // Draw Checkmark
                checkPath.reset()
                val left = cx - radius * 0.35f
                val midX = cx - radius * 0.05f
                val right = cx + radius * 0.40f

                val topY = cy - radius * 0.05f
                val bottomY = cy + radius * 0.30f
                val endY = cy - radius * 0.35f

                checkPath.moveTo(left, topY)
                checkPath.lineTo(midX, bottomY)
                checkPath.lineTo(right, endY)
                canvas.drawPath(checkPath, checkPaint)
            }

            DayStatus.TODAY -> {
                alpha = 1f
                // White background
                fillPaint.color = whiteColor
                canvas.drawCircle(cx, cy, radius - strokeWidthPx / 2f, fillPaint)

                // Dashed outline
                canvas.drawCircle(cx, cy, radius - strokeWidthPx / 2f, dashedStrokePaint)

                // Inner dot
                fillPaint.color = todayColor
                fillPaint.alpha = 120 // ~45% alpha dot
                canvas.drawCircle(cx, cy, radius * 0.25f, fillPaint)
                fillPaint.alpha = 255
            }

            DayStatus.MISSED -> {
                alpha = 1f
                strokePaint.color = outlineVariantColor
                canvas.drawCircle(cx, cy, radius - strokeWidthPx / 2f, strokePaint)
            }

            DayStatus.FUTURE -> {
                alpha = 0.5f
                fillPaint.color = futureColor
                canvas.drawCircle(cx, cy, radius, fillPaint)
            }
        }
    }
}
