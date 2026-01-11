package com.ict.expensemanagement.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.ict.expensemanagement.stats.CategoriesFragment
import com.ict.expensemanagement.stats.SpendsFragment

class HomePagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    private val fragments = mutableMapOf<Int, Fragment>()

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        val fragment = when (position) {
            0 -> SpendsFragment()
            1 -> CategoriesFragment()
            else -> SpendsFragment()
        }
        fragments[position] = fragment
        return fragment
    }

    fun getFragment(position: Int): Fragment? {
        return fragments[position]
    }
}