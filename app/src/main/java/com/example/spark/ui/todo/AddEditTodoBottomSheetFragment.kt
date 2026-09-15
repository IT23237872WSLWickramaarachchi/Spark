package com.example.spark.ui.todo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.spark.R
import com.example.spark.data.local.entity.TodoEntity
import com.example.spark.databinding.FragmentAddEditTodoBinding
import com.example.spark.di.ServiceLocator
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AddEditTodoBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAddEditTodoBinding? = null
    private val binding get() = _binding!!

    private var todoId: Long = -1L
    private var existingTodo: TodoEntity? = null
    private var selectedDueDate: LocalDate? = null
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        todoId = arguments?.getLong(ARG_TODO_ID, -1L) ?: -1L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditTodoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()

        if (savedInstanceState == null && todoId != -1L) {
            loadExistingTodo()
        } else if (todoId == -1L) {
            binding.tvHeaderTitle.text = getString(R.string.todo_new_task)
            binding.btnDelete.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.etTitle.doAfterTextChanged {
            binding.layoutTitle.error = null
        }

        // Date Picker Click
        binding.chipDueDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnClearDueDate.setOnClickListener {
            selectedDueDate = null
            updateDueDateChip()
        }

        // Save Click
        binding.btnSave.setOnClickListener {
            saveTodo()
        }

        // Delete Click
        binding.btnDelete.setOnClickListener {
            deleteTodo()
        }
    }

    private fun loadExistingTodo() {
        lifecycleScope.launch {
            val repo = ServiceLocator.provideTodoRepository(requireContext())
            val todo = repo.getTodoById(todoId)
            if (todo != null && isAdded) {
                existingTodo = todo
                binding.tvHeaderTitle.text = getString(R.string.todo_edit_task)
                binding.etTitle.setText(todo.title)
                binding.etDescription.setText(todo.description ?: "")

                when (todo.priority.lowercase()) {
                    "high" -> binding.chipPriorityHigh.isChecked = true
                    "low" -> binding.chipPriorityLow.isChecked = true
                    else -> binding.chipPriorityMedium.isChecked = true
                }

                selectedDueDate = todo.dueDate
                updateDueDateChip()

                binding.btnDelete.visibility = View.VISIBLE
            }
        }
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.todo_due_date_label))
            .build()

        picker.addOnPositiveButtonClickListener { epochMillis ->
            val date = Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.of("UTC"))
                .toLocalDate()
            selectedDueDate = date
            updateDueDateChip()
        }

        if (childFragmentManager.findFragmentByTag("TODO_DATE_PICKER") == null) {
            picker.show(childFragmentManager, "TODO_DATE_PICKER")
        }
    }

    private fun updateDueDateChip() {
        val date = selectedDueDate
        if (date != null) {
            val today = LocalDate.now()
            binding.chipDueDate.text = when {
                date.isEqual(today) -> getString(R.string.todo_due_today)
                else -> date.format(dateFormatter)
            }
            binding.btnClearDueDate.visibility = View.VISIBLE
        } else {
            binding.chipDueDate.text = getString(R.string.todo_no_due_date)
            binding.btnClearDueDate.visibility = View.GONE
        }
    }

    private fun getSelectedPriority(): String {
        return when (binding.chipGroupPriority.checkedChipId) {
            R.id.chipPriorityHigh -> "High"
            R.id.chipPriorityLow -> "Low"
            else -> "Medium"
        }
    }

    private fun saveTodo() {
        val title = binding.etTitle.text?.toString()?.trim().orEmpty()
        if (title.isEmpty()) {
            binding.layoutTitle.error = getString(R.string.todo_title_error)
            return
        }

        val description = binding.etDescription.text?.toString()?.trim()?.ifBlank { null }
        val priority = getSelectedPriority()
        val userId = ServiceLocator.sessionManager?.currentUserId ?: 1L

        lifecycleScope.launch {
            val repo = ServiceLocator.provideTodoRepository(requireContext())
            val existing = existingTodo
            if (existing != null) {
                val updated = existing.copy(
                    title = title,
                    description = description,
                    priority = priority,
                    dueDate = selectedDueDate
                )
                repo.updateTodo(updated)
            } else {
                val newTodo = TodoEntity(
                    userId = userId,
                    title = title,
                    description = description,
                    priority = priority,
                    dueDate = selectedDueDate
                )
                repo.insertTodo(newTodo)
            }
            dismiss()
        }
    }

    private fun deleteTodo() {
        val existing = existingTodo ?: return
        lifecycleScope.launch {
            val repo = ServiceLocator.provideTodoRepository(requireContext())
            repo.deleteTodo(existing)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddEditTodoBottomSheet"
        private const val ARG_TODO_ID = "arg_todo_id"

        fun newInstance(todoId: Long? = null): AddEditTodoBottomSheetFragment {
            val fragment = AddEditTodoBottomSheetFragment()
            if (todoId != null) {
                val args = Bundle().apply {
                    putLong(ARG_TODO_ID, todoId)
                }
                fragment.arguments = args
            }
            return fragment
        }
    }
}
