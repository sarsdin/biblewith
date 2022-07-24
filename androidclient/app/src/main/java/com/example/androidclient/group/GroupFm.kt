package com.example.androidclient.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.R
import com.example.androidclient.databinding.GroupFmBinding
import com.example.androidclient.home.MainActivity
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


class GroupFm : Fragment() {
    lateinit var groupVm: GroupVm
    lateinit var groupVpa: GroupFmVpa
//    lateinit var rva: GroupRva
    lateinit var rv: RecyclerView
    var mbinding: GroupFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언
    val pageFmList = ArrayList<Fragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageFmList.add(GroupListFm())
        pageFmList.add(GroupChatFm())
        pageFmList.add(GroupNotifyFm())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mbinding = GroupFmBinding.inflate(inflater, container, false)
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)

        //----------------------------------------------------그룹 뷰페이저2 셋팅

        groupVpa = GroupFmVpa(pageFmList, childFragmentManager, lifecycle)
        binding.groupTabLayoutViewpager.offscreenPageLimit = 3
        binding.groupTabLayoutViewpager.adapter = groupVpa

        val tym = TabLayoutMediator(binding.groupMainTabLayout, binding.groupTabLayoutViewpager,
            TabLayoutMediator.TabConfigurationStrategy { tab, position ->
                when (position) {
                    0 -> {
//                        tab.text = "모임"
                        tab.setIcon(R.drawable.ic_group_icon)
                    }
                    1 -> {
//                        tab.text = "채팅"
                        tab.setIcon(R.drawable.ic_baseline_chat_24)
                    }
                    2 -> {
//                        tab.text = "알림"
                        tab.setIcon(R.drawable.ic_notifications_black_24dp)
                    }
                } /*else {
                    tab.text = "기타"
                }*/
            })
        tym.attach()
        //-----------------------------------------------------------------------

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //메인액티비티의 툴바는 감춤
//        (requireActivity() as MainActivity).binding.mainToolbar.visibility = View.GONE

        //모임 툴바 셋팅
        val navController = Navigation.findNavController(view)
//        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val appBarConfiguration = AppBarConfiguration.Builder(R.id.group_fm).build()
//        binding.groupMainCollapsingToolbar.setupWithNavController(binding.groupMainToolbar, navController, appBarConfiguration)
        binding.groupMainToolbar.setupWithNavController(navController, appBarConfiguration)

        //스크롤 이벤트로 밑으로 갈시 바텀네비게이션 감추기
        binding.groupMainNestedScrollView.setOnScrollChangeListener(View.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
//            Log.e("[GroupFm]", "scroll ev : $scrollY , $oldScrollY" );
            if(scrollY > oldScrollY){ //이전 위치보다 높다는 말은 밑으로 스크롤 한다는 말
                (requireActivity() as MainActivity).binding.mainBottomNav.visibility = View.GONE
            } else {
                (requireActivity() as MainActivity).binding.mainBottomNav.visibility = View.VISIBLE
            }
        })

        //탭 선택시 툴바의 제목 바꾸기
        binding.groupMainTabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
//                Toast.makeText(requireContext(), "${tab?.id}, ${tab?.contentDescription}, ${tab?.position} ",Toast.LENGTH_SHORT).show()
                when(tab?.position){
                    0 -> {
                        binding.groupMainToolbarTv.text = "모임 홈"
                    }
                    1 -> {
                        binding.groupMainToolbarTv.text = "모임 채팅"
                    }
                    2 -> {
                        binding.groupMainToolbarTv.text = "모임 알림"
                    }
                  else -> {}
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

        })


    }

    override fun onResume() {
        super.onResume()
//        (requireActivity() as MainActivity).supportActionBar?.hide() //지원바를 설정시에만 사용(setSupportActionBar). 그냥 toolbar를 사용했을때는 작동안함.
        //스크롤 이벤트로 감춰진 바텀네비게이션 다시 보이게(이벤트의 의도된작동인지모르는데 네비가 사라짐. 이때 이페이지로 복귀했을때 첫화면에서는 보여야하므로)
        (requireActivity() as MainActivity).binding.mainBottomNav.visibility = View.VISIBLE

    }

    override fun onPause() {
        super.onPause()
        //onViewCreated에서 감추었던 메인액티비티의 툴바를 다시 보이게 함(다른 화면에서는 보여야하므로)
//        (requireActivity() as MainActivity).binding.mainToolbar.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }
}