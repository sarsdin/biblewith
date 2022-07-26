package com.example.androidclient.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.R
import com.example.androidclient.databinding.ChallengeFmBinding
import com.example.androidclient.databinding.GroupChatFmBinding

class ChallengeFm : Fragment() {
    lateinit var groupVm: GroupVm
    lateinit var rva: ChallengeFmRva
    lateinit var rv: RecyclerView
    var mbinding: ChallengeFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
        mbinding = ChallengeFmBinding.inflate(inflater, container, false)

        rv = binding.chalList
        rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        rv.adapter = ChallengeFmRva(groupVm, this)
        rva = rv.adapter as ChallengeFmRva

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = Navigation.findNavController(view)
//        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val appBarConfiguration = AppBarConfiguration.Builder(R.id.group_fm).build()

//        binding.groupMainCollapsingToolbar.setupWithNavController(binding.groupMainToolbar, navController, appBarConfiguration)
//        binding.groupInToolbar.setupWithNavController(navController, appBarConfiguration)
//        NavigationUI.setupWithNavController(
//            binding.groupInToolbar,
//            navController,
//            appBarConfiguration
//        )

        binding.chalToolbarWriteBt.setOnClickListener {
            findNavController().navigate(R.id.action_group_in_challenge_fm_to_challengeCreateFm)
        }


    }
}