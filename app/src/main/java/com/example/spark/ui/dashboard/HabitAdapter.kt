package com.example.spark.ui.dashboard

import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.spark.R
import com.example.spark.databinding.ItemHabitBinding

/**
 * ListAdapter for habit items displayed on the Dashboard.
 *
 * Uses DiffUtil for efficient animated updates. Checkbox click events are dispatched
 * directly to the ViewModel/Repository Flow without mutating the view locally, guaranteeing
 * that UI state always reflects the database single source of truth.
 */
class HabitAdapter(
    private val onToggleHabit: (habitId: String, isChecked: Boolean) -> Unit,
    private val onItemClick: ((HabitUiModel) -> Unit)? = null
) : ListAdapter<HabitUiModel, HabitAdapter.ViewHolder>(HabitDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHabitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemHabitBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HabitUiModel) {
            val context = binding.root.context

            binding.tvHabitTitle.text = item.title
            binding.tvHabitSubtitle.text = item.subtitle

            // Completed state styling: strikethrough & muted color
            if (item.isCompleted) {
                binding.tvHabitTitle.paintFlags =
                    binding.tvHabitTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvHabitTitle.setTextColor(ContextCompat.getColor(context, R.color.outline))
            } else {
                binding.tvHabitTitle.paintFlags =
                    binding.tvHabitTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvHabitTitle.setTextColor(ContextCompat.getColor(context, R.color.on_surface))
            }

            // Leading Icon & tinted circular background
            binding.ivHabitIcon.setImageResource(item.iconRes)
            binding.ivHabitIcon.setColorFilter(item.iconColor)

            val circleBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(item.iconBgColor)
            }
            binding.frameIconContainer.background = circleBg

            // Trailing animated CircleToggleView - DB-truth bound via DiffUtil
            binding.toggleHabit.onCheckedChangeListener = null
            binding.toggleHabit.setChecked(item.isCompleted, animate = false)

            // Click listener delegates to ViewModel without optimistic local toggle
            binding.toggleHabit.setOnClickListener {
                onToggleHabit(item.id.toString(), !item.isCompleted)
            }

            binding.cardHabitRow.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }

    class HabitDiffCallback : DiffUtil.ItemCallback<HabitUiModel>() {
        override fun areItemsTheSame(oldItem: HabitUiModel, newItem: HabitUiModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HabitUiModel, newItem: HabitUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
