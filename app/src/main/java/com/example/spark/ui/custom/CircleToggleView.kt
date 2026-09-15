package com.example.spark.ui.custom

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.Checkable
import androidx.core.content.ContextCompat
import com.example.spark.R
import kotlin.math.min

/**
 * Custom View representing a circular toggle checkbox with animated transitions
 * between outline (incomplete) and filled-green with checkmark (complete).
 *
 * Used in habit list rows ([item_habit.xml]) to mark habit completion.
 */
class CircleToggleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), Checkable {

    private val density = resources.displayMetrics.density
    private val strokeWidthPx = 2f * density
    private val checkStrokeWidthPx = 2.5f * density

    private val outlineColor = ContextCompat.getColor(context, R.color.divider_grey)
    private val activeColor = Color.parseColor("#6FCF97") // Success green
    private val checkColor = Color.WHITE

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        color = outlineColor
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = activeColor
    }

    private val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = checkStrokeWidthPx
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = checkColor
    }

    private val checkPath = Path()
    private var isCheckedState = false
    private var animationFraction = 0f // 0f = unchecked, 1f = checked
    private var animator: ValueAnimator? = null

    var onCheckedChangeListener: ((view: CircleToggleView, isChecked: Boolean) -> Unit)? = null

    init {
        isClickable = true
        isFocusable = true
        setOnClickListener {
            toggle()
        }
    }

    override fun isChecked(): Boolean = isCheckedState

    override fun setChecked(checked: Boolean) {
        setChecked(checked, animate = true)
    }

    fun setChecked(checked: Boolean, animate: Boolean) {
        if (isCheckedState == checked) return
        isCheckedState = checked

        animator?.cancel()
        val target = if (checked) 1f else 0f

        if (!animate || !isAttachedToWindow) {
            animationFraction = target
            invalidate()
            onCheckedChangeListener?.invoke(this, isCheckedState)
            return
        }

        animator = ValueAnimator.ofFloat(animationFraction, target).apply {
            duration = 300L
            interpolator = OvershootInterpolator(1.2f)
            addUpdateListener { va ->
                animationFraction = va.animatedValue as Float
                invalidate()
            }
            start()
        }

        onCheckedChangeListener?.invoke(this, isCheckedState)
    }

    override fun toggle() {
        performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
        setChecked(!isCheckedState, animate = true)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val diameter = min(width, height).toFloat()
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = (diameter - strokeWidthPx) / 2f

        // 1. Draw base outline circle
        outlinePaint.color = if (animationFraction > 0.5f) activeColor else outlineColor
        canvas.drawCircle(cx, cy, maxRadius, outlinePaint)

        // 2. Draw expanding filled circle
        if (animationFraction > 0f) {
            val fillRadius = maxRadius * animationFraction
            canvas.drawCircle(cx, cy, fillRadius, fillPaint)
        }

        // 3. Draw checkmark centered inside
        if (animationFraction > 0.3f) {
            val checkAlpha = ((animationFraction - 0.3f) / 0.7f).coerceIn(0f, 1f)
            checkPaint.alpha = (checkAlpha * 255).toInt()

            val size = diameter * 0.42f
            val half = size / 2f

            checkPath.reset()
            // Checkmark coordinates relative to center
            checkPath.moveTo(cx - half * 0.7f, cy + half * 0.05f)
            checkPath.lineTo(cx - half * 0.15f, cy + half * 0.65f)
            checkPath.lineTo(cx + half * 0.75f, cy - half * 0.45f)

            canvas.drawPath(checkPath, checkPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
