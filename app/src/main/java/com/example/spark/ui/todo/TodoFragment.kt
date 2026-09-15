package com.example.spark.ui.todo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spark.R
import com.example.spark.databinding.FragmentTodoBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class TodoFragment : Fragment() {

    private var _binding: FragmentTodoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TodoViewModel by viewModels { TodoViewModel.provideFactory() }
    private lateinit var adapter: TodoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeUiState()
    }

    private fun setupRecyclerView() {
        adapter = TodoAdapter(
            onToggle = { todo ->
                viewModel.toggleTodo(todo)
            },
            onClick = { todo ->
                AddEditTodoBottomSheetFragment.newInstance(todo.id)
                    .show(childFragmentManager, AddEditTodoBottomSheetFragment.TAG)
            },
            onDelete = { todo ->
                viewModel.deleteTodo(todo)
                Snackbar.make(
                    binding.root,
                    R.string.todo_deleted_message,
                    Snackbar.LENGTH_LONG
                ).setAction(R.string.action_undo) {
                    viewModel.insertTodo(todo)
                }.setAnchorView(binding.fabAddTodo)
                .show()
            }
        )

        binding.rvTodos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTodos.adapter = adapter
    }

    private fun setupListeners() {
        // FAB -> Add Task
        binding.fabAddTodo.setOnClickListener {
            AddEditTodoBottomSheetFragment.newInstance()
                .show(childFragmentManager, AddEditTodoBottomSheetFragment.TAG)
        }

        // Search Input
        binding.etSearch.doAfterTextChanged { text ->
            viewModel.onSearchQueryChange(text?.toString().orEmpty())
        }

        // Filter Chips
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chipAll
            val filter = when (checkedId) {
                R.id.chipPending -> TodoFilter.PENDING
                R.id.chipCompleted -> TodoFilter.COMPLETED
                else -> TodoFilter.ALL
            }
            viewModel.onFilterChange(filter)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderUi(state)
                }
            }
        }
    }

    private fun renderUi(state: TodoUiState) {
        adapter.submitList(state.todos)

        // Progress bar and summary
        binding.tvProgressSummary.text = getString(
            R.string.todo_progress_summary,
            state.completedCount,
            state.totalCount,
            state.completionPercent
        )
        binding.progressIndicator.setProgressCompat(state.completionPercent, true)

        // Empty state toggle
        if (state.todos.isEmpty() && !state.isLoading) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvTodos.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvTodos.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
