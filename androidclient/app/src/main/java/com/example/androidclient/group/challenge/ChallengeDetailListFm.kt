package com.example.androidclient.group.challenge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.MyApp
import com.example.androidclient.R
import com.example.androidclient.databinding.ChallengeDetailListFmBinding
import com.example.androidclient.group.GroupVm
import com.example.androidclient.util.ImageHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChallengeDetailListFm : Fragment() {
    lateinit var groupVm: GroupVm
    lateinit var rva: ChallengeDetailListRva
    lateinit var rv: RecyclerView
    var mbinding: ChallengeDetailListFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
        mbinding = ChallengeDetailListFmBinding.inflate(inflater, container, false)

        rv = binding.detailList
        rv.layoutManager = LinearLayoutManager(context);
        rv.adapter = ChallengeDetailListRva(groupVm, this)
        rva = rv.adapter as ChallengeDetailListRva

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = Navigation.findNavController(view)
//        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val appBarConfiguration = AppBarConfiguration.Builder(R.id.group_fm).build()
        NavigationUI.setupWithNavController(
            binding.toolbar,
            navController,
            appBarConfiguration
        )
        CoroutineScope(Dispatchers.IO).launch {
//            Thread.sleep(100)
            groupVm.챌린지상세목록가져오기(groupVm.chalLInfo .get("chal_no").asInt, true)
        }

        //ui 갱신
        groupVm.liveChalDetailL.observe(viewLifecycleOwner, Observer {
            rva.notifyDataSetChanged()
        })



    }

    override fun onResume() {
        super.onResume()
        binding.toolbarTv.text = "${groupVm.chalLInfo.get("user_nick").asString}님의 챌린지: ${groupVm.chalLInfo.get("chal_title").asString}"
        rva.notifyDataSetChanged()

        //상단바 프로필 이미지 클릭시
        binding.toolbarIv.setOnClickListener {
            findNavController().navigate(com.example.androidclient.R.id.action_global_myProfileFm)
        }
        //상단바 프로필 이미지 로딩
        ImageHelper.getImageUsingGlide(requireActivity(), MyApp.userInfo.user_image, binding.toolbarIv)


    }

    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }
}