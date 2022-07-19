package com.example.androidclient.group
import android.util.Log

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.R
import com.example.androidclient.databinding.GroupInFmBinding
import com.example.androidclient.home.MainActivity
import com.example.androidclient.util.ImageHelper

class GroupInFm : Fragment() {
    lateinit var groupVm: GroupVm
    lateinit var rva: GroupInRva
    lateinit var rv: RecyclerView
    var mbinding: GroupInFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    val imageHelper = ImageHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
        groupVm.모임상세불러오기(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mbinding = GroupInFmBinding.inflate(inflater, container, false)

        rv = binding.groupInList
        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = GroupInRva(groupVm, this)
        rva = rv.adapter as GroupInRva

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        //모임 툴바 셋팅
        val navController = Navigation.findNavController(view)
//        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val appBarConfiguration = AppBarConfiguration.Builder(R.id.group_fm).build()
//        binding.groupMainCollapsingToolbar.setupWithNavController(binding.groupMainToolbar, navController, appBarConfiguration)
//        binding.groupInToolbar.setupWithNavController(navController, appBarConfiguration)
        setupWithNavController(binding.groupInToolbar, navController, appBarConfiguration)

        //스크롤 이벤트로 밑으로 갈시 바텀네비게이션 감추기
        binding.groupInNestedScrollView.setOnScrollChangeListener(View.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
//            Log.e("[GroupFm]", "scroll ev : $scrollY , $oldScrollY" );
            if(scrollY > oldScrollY){ //이전 위치보다 높다는 말은 밑으로 스크롤 한다는 말
                binding.groupInBottomNavi.visibility = View.GONE
            } else {
                binding.groupInBottomNavi.visibility = View.VISIBLE
            }
        })

        // Ui 갱신
        groupVm.liveGroupInfo.observe(viewLifecycleOwner, Observer {
            //모임 이미지 받아오기
            imageHelper.getImageUsingGlide(requireContext(), groupVm.groupInfo.get("group_main_image").asString
                , binding.groupInCollapsingToolbarIv )
            //모임 이름 적용
            binding.groupInSummaryNameTv.text = groupVm.groupInfo.get("group_name").asString
            binding.groupInToolbarTv.text = groupVm.groupInfo.get("group_name").asString
        })
        groupVm.liveMemberL.observe(viewLifecycleOwner, Observer {
            //멤버수
            binding.groupInSummaryMemberTv.text = groupVm.memberL.size().toString()
        })
        groupVm.liveGboardL.observe(viewLifecycleOwner, Observer {
            //게시물 갱신
            rva.notifyDataSetChanged()
        })

        //글쓰기 버튼 클릭시
        binding.groupInSummaryWriteBt.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_groupInFm_to_groupInWriteFm)
        }
        binding.groupInToolbarWriteBt.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_groupInFm_to_groupInWriteFm)
        }

    }

    override fun onResume() {
        super.onResume()
        //메인액티비티의 툴바는 감춤
//        (requireActivity() as MainActivity).binding.mainToolbar.visibility = View.GONE
//        (requireActivity() as MainActivity).binding.mainBottomNav.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        //onViewCreated에서 감추었던 메인액티비티의 툴바를 다시 보이게 함(다른 화면에서는 보여야하므로)
//        (requireActivity() as MainActivity).binding.mainToolbar.visibility = View.VISIBLE
//        (requireActivity() as MainActivity).binding.mainBottomNav.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }
}