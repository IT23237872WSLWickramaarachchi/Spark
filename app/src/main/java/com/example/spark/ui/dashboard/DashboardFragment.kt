package com.example.spark.ui.dashboard

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.spark.R
import com.example.spark.SparkApplication
import com.example.spark.databinding.FragmentDashboardBinding
import com.example.spark.ui.habit.AddHabitBottomSheetFragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    val binding get() = _binding!!

    private lateinit var viewModel: DashboardViewModel
    private lateinit var habitAdapter: HabitAdapter
    private var pendingDeleteHabitId: Long? = null
    private var currentSnackbar: Snackbar? = null
    private var deleteIcon: Drawable? = null
    private var lastRenderedTasks: List<com.example.spark.data.local.entity.TodoEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as SparkApplication
        viewModel = ViewModelProvider(
            this,
            DashboardViewModel.Factory(
                habitRepository = app.habitRepository,
                moodRepository = app.moodRepository,
                userRepository = app.userRepository,
                currentUserIdProvider = { app.sessionManager.currentUserId }
            )
        )[DashboardViewModel::class.java]

        setupToolbar()
        setupMoodPill()
        setupTasksCard()
        setupHabitsRecyclerView()
        setupFab()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: DashboardUiState) {
        // Greeting text
        binding.tvGreeting.text = "${state.greeting}, ${state.userName}"

        // Progress ring values
        binding.circularProgressView.setProgress(state.progressPercent.toFloat(), animated = true)
        binding.tvProgressPercent.text = "${state.progressPercent}%"
        binding.tvProgressHabitsCount.text = "${state.completedToday} of ${state.totalToday} habits"

        // Streak badge
        binding.tvStreakBadge.text = "${state.streakDays} day streak"

        // Tasks Overview
        if (state.pendingTasksCount > 0) {
            binding.tvTasksSubtitle.text = getString(R.string.dashboard_tasks_pending_count, state.pendingTasksCount)
            binding.layoutTasksPreview.visibility = View.VISIBLE

            if (state.topTasks != lastRenderedTasks) {
                lastRenderedTasks = state.topTasks
                binding.layoutTasksPreview.removeAllViews()
                val inflater = LayoutInflater.from(requireContext())
                state.topTasks.forEach { task ->
                    val taskView = inflater.inflate(R.layout.item_todo, binding.layoutTasksPreview, false)
                    val card = taskView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardTodo)
                    val toggle = taskView.findViewById<com.example.spark.ui.custom.CircleToggleView>(R.id.toggleTodo)
                    val title = taskView.findViewById<android.widget.TextView>(R.id.tvTodoTitle)
                    val deleteBtn = taskView.findViewById<android.view.View>(R.id.btnDeleteTodo)
                    val metaLayout = taskView.findViewById<android.view.View>(R.id.layoutMeta)

                    deleteBtn.visibility = View.GONE
                    metaLayout.visibility = View.GONE
                    title.text = task.title
                    toggle.isChecked = task.isCompleted

                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_container_low))
                    card.strokeWidth = 0

                    toggle.setOnClickListener {
                        viewModel.toggleTask(task)
                    }
                    card.setOnClickListener {
                        findNavController().navigate(R.id.action_dashboard_to_todo)
                    }
                    binding.layoutTasksPreview.addView(taskView)
                }
            }
        } else {
            binding.tvTasksSubtitle.text = getString(R.string.dashboard_tasks_all_done)
            lastRenderedTasks = emptyList()
            binding.layoutTasksPreview.removeAllViews()
            binding.layoutTasksPreview.visibility = View.GONE
        }

        // Habit list & empty state
        habitAdapter.submitList(state.habits)
        binding.rvHabits.visibility = if (state.isEmpty) View.GONE else View.VISIBLE
        binding.layoutEmptyState.visibility = if (state.isEmpty) View.VISIBLE else View.GONE

        // Mood label (if present)
        if (state.moodLabel != null) {
            binding.cardMoodPill.visibility = View.VISIBLE
            binding.tvMoodText.text = state.moodLabel
        }
    }

    private fun setupTasksCard() {
        val navigateToTasks = {
            findNavController().navigate(R.id.action_dashboard_to_todo)
        }
        binding.btnViewAllTasks.setOnClickListener { navigateToTasks() }
        binding.cardTasksOverview.setOnClickListener { navigateToTasks() }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_notifications -> {
                    // Notification click placeholder
                    true
                }
                else -> false
            }
        }

        val searchItem = binding.toolbar.menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView
        searchView?.apply {
            queryHint = getString(R.string.search_habits_hint)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    viewModel.onSearchQueryChange(query.orEmpty())
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel.onSearchQueryChange(newText.orEmpty())
                    return true
                }
            })
        }

        searchItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                viewModel.onSearchQueryChange("")
                return true
            }
        })
    }

    private fun setupMoodPill() {
        binding.btnDismissMood.setOnClickListener {
            binding.cardMoodPill.visibility = View.GONE
        }

        binding.cardMoodPill.setOnClickListener {
            showMoodCheckInDialog()
        }
    }

    private fun showMoodCheckInDialog() {
        com.example.spark.ui.mood.MoodCheckInDialogFragment.newInstance().show(
            childFragmentManager,
            com.example.spark.ui.mood.MoodCheckInDialogFragment.TAG
        )
    }

    private fun setupHabitsRecyclerView() {
        binding.rvHabits.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        habitAdapter = HabitAdapter(
            onToggleHabit = { habitId, isChecked ->
                viewModel.onHabitToggle(habitId, isChecked)
            },
            onItemClick = { habitUi ->
                val bundle = com.example.spark.ui.habitdetail.HabitDetailFragment.createBundle(habitUi.id)
                findNavController().navigate(R.id.action_dashboard_to_habitDetail, bundle)
            }
        )
        binding.rvHabits.adapter = habitAdapter
        setupSwipeToDelete()
    }

    private fun setupSwipeToDelete() {
        deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)

        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION || position >= habitAdapter.currentList.size) {
                    return
                }

                val swipedHabit = habitAdapter.currentList[position]

                // If another deletion was pending, commit it immediately before processing the new one
                pendingDeleteHabitId?.let { prevId ->
                    viewModel.deleteHabit(prevId)
                }

                pendingDeleteHabitId = swipedHabit.id
                currentSnackbar?.dismiss()

                val snackbar = Snackbar.make(
                    binding.root,
                    getString(R.string.habit_deleted_message),
                    Snackbar.LENGTH_LONG
                )
                    .setAnchorView(binding.fabAddHabit)
                    .setAction(R.string.action_undo) {
                        pendingDeleteHabitId = null
                        habitAdapter.notifyItemChanged(position)
                    }
                    .addCallback(object : Snackbar.Callback() {
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            super.onDismissed(transientBottomBar, event)
                            if (event != DISMISS_EVENT_ACTION && pendingDeleteHabitId == swipedHabit.id) {
                                viewModel.deleteHabit(swipedHabit.id)
                                pendingDeleteHabitId = null
                            }
                        }
                    })

                currentSnackbar = snackbar
                snackbar.show()
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    val itemView = viewHolder.itemView
                    val context = itemView.context
                    val density = context.resources.displayMetrics.density

                    val cornerRadius = 20f * density
                    val bottomMargin = 12f * density

                    val backgroundRect = RectF(
                        itemView.right + dX,
                        itemView.top.toFloat(),
                        itemView.right.toFloat(),
                        itemView.bottom.toFloat() - bottomMargin
                    )

                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = ContextCompat.getColor(context, R.color.error)
                    }
                    c.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, paint)

                    deleteIcon?.let { icon ->
                        val iconMargin = (24f * density).toInt()
                        val iconIntrinsicWidth = icon.intrinsicWidth
                        val iconIntrinsicHeight = icon.intrinsicHeight

                        val itemHeight = (itemView.bottom - bottomMargin - itemView.top).toInt()
                        val iconTop = itemView.top + (itemHeight - iconIntrinsicHeight) / 2
                        val iconBottom = iconTop + iconIntrinsicHeight
                        val iconRight = itemView.right - iconMargin
                        val iconLeft = iconRight - iconIntrinsicWidth

                        if (iconLeft > backgroundRect.left) {
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                            icon.setTint(ContextCompat.getColor(context, R.color.white))
                            icon.draw(c)
                        }
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvHabits)
    }

    private fun setupFab() {
        binding.fabAddHabit.setOnClickListener {
            showAddHabitSheet()
        }
    }

    private fun showAddHabitSheet() {
        AddHabitBottomSheetFragment.newInstance().show(
            childFragmentManager,
            AddHabitBottomSheetFragment.TAG
        )
    }

    override fun onPause() {
        super.onPause()
        pendingDeleteHabitId?.let { id ->
            viewModel.deleteHabit(id)
            pendingDeleteHabitId = null
        }
        currentSnackbar?.dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
