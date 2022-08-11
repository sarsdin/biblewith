package com.example.androidclient.group

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.MyApp
import com.example.androidclient.R
import com.example.androidclient.databinding.GroupFmBinding
import com.example.androidclient.home.MainActivity
import com.example.androidclient.util.Http
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class GroupFm : Fragment() {

    val tagName = "[GroupFm]"
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

        //초대 링크 처리 부분 - MainActivity(onCreate)에서 전달받은 번들의 정보를 이용해 해당 모임을 보여주고 초대에 응할건지 물어보는 dialog를 띄운다
        arguments?.let {
            val group_no = arguments?.get("group_no").toString().toInt()
            val invite_code = arguments?.get("invite_code").toString()
            Log.e(tagName, "아규먼트 확인 : $group_no, $invite_code")

            val retrofit = Http.getRetrofitInstance(Http.HOST_IP)
            val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)

            val call = httpGroup.모임초대링크유효한지확인(group_no, invite_code, MyApp.userInfo.user_no)
            call.enqueue(object : Callback<JsonObject?> {
                override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                    if (response.isSuccessful) {
                        val res = response.body()!!
                        if(!res.get("result").isJsonNull){
                            Log.e(tagName, "모임초대링크유효한지확인 onResponse: $res")

                            //다이얼로그에 참가를 클릭하면 초대 링크에 해당하는 모임의 멤버로 사용자를 추가한다
                            AlertDialog.Builder(requireActivity())
                                .setTitle("${res.get("result").asJsonObject.get("group_info").asJsonObject.get("group_name").asString}")
                                .setMessage("해당 모임에 참가하시겠습니까?")
                                .setNeutralButton("초대거부") { dialogInterface, i ->
                                    it.clear() //초대 거부 시 초대장 번들 제거하기 - 다시 실행되지 않도록..
                                    arguments = null
                                }
                                .setPositiveButton("참가하기") { dialogInterface, i ->
                                    //참가하면 서버와 통신하여 멤버로 추가!
                                    val call = httpGroup.모임초대링크로멤버추가하기(group_no, invite_code, MyApp.userInfo.user_no)
                                    call.enqueue(object : Callback<JsonObject?> {
                                        override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                            if (response.isSuccessful) {
                                                val res = response.body()!!
                                                if(!res.get("result").isJsonNull){
//                                                    val resObj = res.get("result").asJsonObject
                                                    Toast.makeText(requireActivity(),"모임에 참가했습니다.",Toast.LENGTH_SHORT).show()
//                                                    (pageFmList.get(0) as GroupListFm).rva.notifyDataSetChanged() // 중요!!!
                                                    groupVm.모임목록가져오기(MyApp.userInfo.user_no, true) //옵져버가 있어서 GroupListFm은 알아서 갱신될것!
                                                }
                                                Log.e(tagName, "모임초대링크로멤버추가하기 onResponse: $res")
                                            }
                                        }
                                        override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                            Log.e(tagName, "모임초대링크로멤버추가하기 onFailure: " + t.message)
                                        }
                                    })
                                }
                                .setCancelable(false)
                                .create()
                                .show()
                        }

                    }
                }
                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    Log.e(tagName, "모임초대링크유효한지확인 onFailure: " + t.message)
                }
            })
        }


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