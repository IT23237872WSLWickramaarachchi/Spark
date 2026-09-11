package com.example.spark.ui.custom

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.example.spark.R
import kotlin.math.min

/**
 * Custom View that draws an animated circular progress ring using Canvas in onDraw.
 *
 * Used in the "Daily Progress" dashboard card to visually show habit completion percentage.
 */
class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var strokeWidthPx = 14f * resources.displayMetrics.density

    private var trackColor = ContextCompat.getColor(context, R.color.divider_grey)
    private var progressColor = ContextCompat.getColor(context, R.color.primary)

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = trackColor
        strokeWidth = strokeWidthPx
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = progressColor
        strokeWidth = strokeWidthPx
    }

    private val arcBounds = RectF()
    private var animator: ValueAnimator? = null

    /** Current progress percentage (0f to 100f). */
    var progress: Float = 0f
        private set

    var maxProgress: Float = 100f
        set(value) {
            field = if (value <= 0f) 100f else value
            invalidate()
        }

    init {
        // Default preview progress for Android Studio layout editor
        if (isInEditMode) {
            progress = 60f
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val diameter = min(w, h)
        val halfStroke = strokeWidthPx / 2f
        val left = (w - diameter) / 2f + halfStroke
        val top = (h - diameter) / 2f + halfStroke
        val right = left + diameter - strokeWidthPx
        val bottom = top + diameter - strokeWidthPx
        arcBounds.set(left, top, right, bottom)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = arcBounds.centerX()
        val cy = arcBounds.centerY()
        val radius = (arcBounds.width()) / 2f

        // 1. Draw full track background ring
        canvas.drawCircle(cx, cy, radius, trackPaint)

        // 2. Draw animated progress arc from top (-90 degrees)
        if (progress > 0f) {
            val sweepAngle = (progress / maxProgress).coerceIn(0f, 1f) * 360f
            canvas.drawArc(arcBounds, -90f, sweepAngle, false, progressPaint)
        }
    }

    /**
     * Updates the progress with an optional smooth ValueAnimator transition.
     */
    fun setProgress(targetProgress: Float, animated: Boolean = true) {
        val clamped = targetProgress.coerceIn(0f, maxProgress)
        animator?.cancel()

        if (!animated) {
            progress = clamped
            invalidate()
            return
        }

        animator = ValueAnimator.ofFloat(progress, clamped).apply {
            duration = 800L
            interpolator = DecelerateInterpolator()
            addUpdateListener { va ->
                progress = va.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    /**
     * Allows customizing the progress stroke color dynamically (e.g. green when 100%).
     */
    fun setProgressColor(color: Int) {
        progressColor = color
        progressPaint.color = color
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
