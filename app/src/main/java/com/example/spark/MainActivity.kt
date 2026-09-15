package com.example.spark

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.spark.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.navHostFragment.setPadding(0, systemBars.top, 0, 0)
            binding.bottomNav.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.dashboard -> {
                    if (navController.currentDestination?.id != R.id.dashboard) {
                        navController.navigate(R.id.dashboard)
                    }
                    true
                }
                R.id.stats -> {
                    if (navController.currentDestination?.id != R.id.stats) {
                        navController.navigate(R.id.stats)
                    }
                    true
                }
                R.id.settings -> {
                    if (navController.currentDestination?.id != R.id.settings) {
                        navController.navigate(R.id.settings)
                    }
                    true
                }
                R.id.add -> {
                    com.example.spark.ui.habit.AddHabitBottomSheetFragment.newInstance()
                        .show(supportFragmentManager, com.example.spark.ui.habit.AddHabitBottomSheetFragment.TAG)
                    false
                }
                else -> false
            }
        }

        // Show BottomNavigationView only on primary tab destinations
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.dashboard,
                R.id.stats,
                R.id.settings -> {
                    binding.bottomNav.visibility = View.VISIBLE
                }
                else -> {
                    binding.bottomNav.visibility = View.GONE
                }
            }
        }
    }
}