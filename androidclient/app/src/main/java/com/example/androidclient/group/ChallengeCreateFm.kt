package com.example.androidclient.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.R
import com.example.androidclient.databinding.ChallengeCreateFmBinding

class ChallengeCreateFm : Fragment() {
    lateinit var groupVm: GroupVm
    lateinit var rva: ChallengeCreateFmRva
    lateinit var rv: RecyclerView
    var mbinding: ChallengeCreateFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
        mbinding = ChallengeCreateFmBinding.inflate(inflater, container, false)

        rv = binding.createList
        rv.layoutManager = GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false);
        rv.adapter = ChallengeCreateFmRva(groupVm, this)
        rva = rv.adapter as ChallengeCreateFmRva

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = Navigation.findNavController(view)
//        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val appBarConfiguration = AppBarConfiguration.Builder(R.id.group_fm).build()
        NavigationUI.setupWithNavController(
            binding.createToolbar,
            navController,
            appBarConfiguration
        )
//        binding.groupMainCollapsingToolbar.setupWithNavController(binding.groupMainToolbar, navController, appBarConfiguration)
//        binding.groupInToolbar.setupWithNavController(navController, appBarConfiguration)
//        NavigationUI.setupWithNavController(
//            binding.groupInToolbar,
//            navController,
//            appBarConfiguration
//        )
    }
}