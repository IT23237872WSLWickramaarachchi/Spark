package com.example.spark.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.spark.databinding.ItemHabitBinding
import com.example.spark.model.HabitItemUiModel

class HabitAdapter(
    private val onToggleComplete: (HabitItemUiModel, Boolean) -> Unit,
    private val onHabitClick: (Long) -> Unit
) : RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {

    private val items = mutableListOf<HabitItemUiModel>()

    fun submitList(newItems: List<HabitItemUiModel>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = items.size
            override fun getNewListSize(): Int = newItems.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition].habit.id == newItems[newItemPosition].habit.id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition] == newItems[newItemPosition]
            }
        })
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val binding = ItemHabitBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HabitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class HabitViewHolder(
        private val binding: ItemHabitBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HabitItemUiModel) {
            val context = binding.root.context
            binding.tvHabitTitle.text = item.habit.name
            binding.tvHabitSubtitle.text = item.subtitle
            binding.ivHabitIcon.setImageResource(item.categoryIconRes)

            val isDark = com.example.spark.util.PreferenceHelper(context).isDarkMode
            val habitColor = com.example.spark.model.HabitColors.getColorByKeyOrHex(item.habit.colorTag, isDark)
            val circleBg = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(habitColor)
            }
            binding.frameIconContainer.background = circleBg
            binding.ivHabitIcon.setColorFilter(android.graphics.Color.WHITE)

            if (item.isCompletedToday) {
                binding.tvHabitTitle.setTextColor(androidx.core.content.ContextCompat.getColor(context, com.example.spark.R.color.outline))
            } else {
                binding.tvHabitTitle.setTextColor(androidx.core.content.ContextCompat.getColor(context, com.example.spark.R.color.on_surface))
            }

            // Avoid callback trigger on recycle/bind
            binding.toggleHabit.onCheckedChangeListener = null
            binding.toggleHabit.setChecked(item.isCompletedToday, animate = false, notifyListener = false)

            binding.toggleHabit.onCheckedChangeListener = { _, isChecked ->
                onToggleComplete(item, isChecked)
            }

            binding.cardHabitRow.setOnClickListener {
                onHabitClick(item.habit.id)
            }
        }
    }
}
