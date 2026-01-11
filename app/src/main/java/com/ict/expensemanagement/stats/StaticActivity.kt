package com.ict.expensemanagement.stats

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ict.expensemanagement.HomeActivity
import com.ict.expensemanagement.ProfileActivity
import com.ict.expensemanagement.R
import com.ict.expensemanagement.goal.SavingsActivity
import com.ict.expensemanagement.adapter.ViewPagerAdapter
import com.ict.expensemanagement.databinding.ActivityStaticBinding

class StaticActivity : AppCompatActivity() {
    private lateinit var binding : ActivityStaticBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaticBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val bottomNavigationView = binding.bottomNavView
        // StaticActivity is not in bottom navigation, so no item is selected

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.item_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.item_savings -> {
                    startActivity(Intent(this, SavingsActivity::class.java))
                    true
                }
                R.id.item_notification -> {
                    // TODO: Navigate to Notification screen
                    true
                }
                R.id.item_settings -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
        val adapter = ViewPagerAdapter(supportFragmentManager)

        // Add your fragments to the adapter
        adapter.addFragment(DayFragment(), "By date")
        adapter.addFragment(WeekFragment(), "By week")
        adapter.addFragment(MonthFragment(), "By month")

        binding.viewPager.adapter = adapter
        binding.tabLayout.setupWithViewPager(binding.viewPager)
    }
}