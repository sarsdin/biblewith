package com.example.androidclient.moreinfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.R
import com.example.androidclient.bible.BibleVm
import com.example.androidclient.databinding.MyProfileFmBinding
import com.example.androidclient.group.GroupVm
import com.example.androidclient.home.MainActivity

class MyProfileFm : Fragment() {
    lateinit var bibleVm: BibleVm
    lateinit var groupVm: GroupVm
    lateinit var rva: MyProfileRva
    lateinit var rv: RecyclerView
    var mbinding: MyProfileFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bibleVm = ViewModelProvider(requireActivity()).get(BibleVm::class.java)
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mbinding = MyProfileFmBinding.inflate(inflater, container, false)

        rv = binding.recentList
        rv.layoutManager = LinearLayoutManager(binding.root.context)
        rv.adapter = MyProfileRva(bibleVm, groupVm, this)
        rva = rv.adapter as MyProfileRva

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //모임 툴바 셋팅
        val navController = Navigation.findNavController(view)
//        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val appBarConfiguration = AppBarConfiguration.Builder(R.id.more_fm).build()
//        binding.groupMainCollapsingToolbar.setupWithNavController(binding.groupMainToolbar, navController, appBarConfiguration)
//        binding.groupInToolbar.setupWithNavController(navController, appBarConfiguration)
        NavigationUI.setupWithNavController( binding.toolbar, navController, appBarConfiguration )


        //스크롤 이벤트로 밑으로 갈시 바텀네비게이션 감추기
        binding.recentList.setOnScrollChangeListener(View.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
//            Log.e("[GroupFm]", "scroll ev : $scrollY , $oldScrollY" );
            if(scrollY > oldScrollY){ //이전 위치보다 높다는 말은 밑으로 스크롤 한다는 말
                (requireActivity() as MainActivity).binding.mainBottomNav.visibility = View.GONE
            } else {
                (requireActivity() as MainActivity).binding.mainBottomNav.visibility = View.VISIBLE
            }
        })

        //초대확인 클릭시 초대확인 페이지로 가기
        binding.inviteBt.setOnClickListener {
            findNavController().navigate(R.id.action_myProfileFm_to_groupInviteVerifyFm)
        }



    }

    override fun onResume() {
        super.onResume()
        rva.notifyDataSetChanged()
    }

    override fun onStop() {
        super.onStop()
        (requireActivity() as MainActivity).binding.mainBottomNav.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}