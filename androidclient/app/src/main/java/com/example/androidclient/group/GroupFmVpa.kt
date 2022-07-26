package com.example.androidclient.group

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class GroupFmVpa (var pageFmList: List<Fragment>, fragmentManager: FragmentManager, lifecycle: Lifecycle)//childFragmentManager
    : FragmentStateAdapter(fragmentManager, lifecycle) {


    override fun createFragment(position: Int): Fragment {
        return pageFmList[position]
    }

    override fun getItemCount(): Int {
        return pageFmList.size
    }
}
