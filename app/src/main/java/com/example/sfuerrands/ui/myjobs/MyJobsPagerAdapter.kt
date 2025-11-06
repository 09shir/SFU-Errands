package com.example.sfuerrands.ui.myjobs

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

// This adapter manages the two tabs/pages in myJobs
class MyJobsPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> RequestsFragment()
            1 -> TasksFragment()
            else -> RequestsFragment()
        }
    }
}