package com.example.spark.ui.todo

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.spark.R
import com.example.spark.data.local.entity.TodoEntity
import com.example.spark.databinding.ItemTodoBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TodoAdapter(
    private val onToggle: (TodoEntity) -> Unit,
    private val onClick: (TodoEntity) -> Unit,
    private val onDelete: (TodoEntity) -> Unit
) : ListAdapter<TodoEntity, TodoAdapter.TodoViewHolder>(TodoDiffCallback()) {

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val binding = ItemTodoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TodoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TodoViewHolder(private val binding: ItemTodoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(todo: TodoEntity) {
            val context = binding.root.context

            // Title & Completion Strikethrough
            binding.tvTodoTitle.text = todo.title
            if (todo.isCompleted) {
                binding.tvTodoTitle.paintFlags =
                    binding.tvTodoTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvTodoTitle.setTextColor(
                    ContextCompat.getColor(context, R.color.outline)
                )
                binding.cardTodo.alpha = 0.65f
            } else {
                binding.tvTodoTitle.paintFlags =
                    binding.tvTodoTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvTodoTitle.setTextColor(
                    ContextCompat.getColor(context, R.color.on_surface)
                )
                binding.cardTodo.alpha = 1.0f
            }

            // Description
            if (!todo.description.isNullOrBlank()) {
                binding.tvTodoDescription.text = todo.description
                binding.tvTodoDescription.visibility = View.VISIBLE
            } else {
                binding.tvTodoDescription.visibility = View.GONE
            }

            // Priority Badge
            binding.tvPriorityBadge.text = todo.priority
            val tintColor = when (todo.priority.lowercase()) {
                "high" -> {
                    binding.tvPriorityBadge.setTextColor(ContextCompat.getColor(context, R.color.error))
                    Color.parseColor("#20BA1A1A")
                }
                "low" -> {
                    binding.tvPriorityBadge.setTextColor(ContextCompat.getColor(context, R.color.tertiary))
                    Color.parseColor("#20006A3F")
                }
                else -> { // Medium
                    binding.tvPriorityBadge.setTextColor(ContextCompat.getColor(context, R.color.primary))
                    Color.parseColor("#2052559C")
                }
            }
            binding.tvPriorityBadge.backgroundTintList =
                android.content.res.ColorStateList.valueOf(tintColor)

            // Due Date
            val dueDate = todo.dueDate
            if (dueDate != null) {
                binding.layoutDueDate.visibility = View.VISIBLE
                val today = LocalDate.now()
                when {
                    dueDate.isEqual(today) -> {
                        binding.tvDueDate.text = context.getString(R.string.todo_due_today)
                        binding.tvDueDate.setTextColor(ContextCompat.getColor(context, R.color.primary))
                        binding.ivDueIcon.setColorFilter(ContextCompat.getColor(context, R.color.primary))
                    }
                    dueDate.isBefore(today) && !todo.isCompleted -> {
                        binding.tvDueDate.text = context.getString(R.string.todo_overdue)
                        binding.tvDueDate.setTextColor(ContextCompat.getColor(context, R.color.error))
                        binding.ivDueIcon.setColorFilter(ContextCompat.getColor(context, R.color.error))
                    }
                    else -> {
                        binding.tvDueDate.text = dueDate.format(dateFormatter)
                        binding.tvDueDate.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
                        binding.ivDueIcon.setColorFilter(ContextCompat.getColor(context, R.color.on_surface_variant))
                    }
                }
            } else {
                binding.layoutDueDate.visibility = View.GONE
            }

            // Completion Toggle
            binding.toggleTodo.isChecked = todo.isCompleted
            binding.toggleTodo.setOnClickListener {
                onToggle(todo)
            }

            // Click Item for Edit
            binding.cardTodo.setOnClickListener {
                onClick(todo)
            }

            // Delete Action
            binding.btnDeleteTodo.setOnClickListener {
                onDelete(todo)
            }
        }
    }

    class TodoDiffCallback : DiffUtil.ItemCallback<TodoEntity>() {
        override fun areItemsTheSame(oldItem: TodoEntity, newItem: TodoEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TodoEntity, newItem: TodoEntity): Boolean =
            oldItem == newItem
    }
}
