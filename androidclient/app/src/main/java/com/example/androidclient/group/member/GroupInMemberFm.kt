package com.example.androidclient.group.member

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.example.androidclient.bible.BibleVm
import com.example.androidclient.databinding.GroupInMemberFmBinding
import com.example.androidclient.group.GroupVm
import com.example.androidclient.MainActivity
import com.example.androidclient.util.ImageHelper
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GroupInMemberFm : Fragment() {
    lateinit var groupVm: GroupVm
    lateinit var bibleVm: BibleVm
    lateinit var rva: GroupInMemberRva
    lateinit var rv: RecyclerView
    var mbinding: GroupInMemberFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mbinding = GroupInMemberFmBinding.inflate(inflater, container, false)

        rv = binding.memberList
        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = GroupInMemberRva(groupVm, this)
        rva = rv.adapter as GroupInMemberRva

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = Navigation.findNavController(view)
//        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val appBarConfiguration = AppBarConfiguration.Builder(R.id.group_fm).build()
        NavigationUI.setupWithNavController(binding.toolbar, navController, appBarConfiguration)


        CoroutineScope(Dispatchers.IO).launch {
//            Thread.sleep(100)
            val jo = JsonObject()
            jo.addProperty("group_no", groupVm.groupInfo.get("group_no").asString)
            groupVm.모임멤버목록로드(jo, true)
        }

        //바텀네비 리스너 설정
        binding.bottomNavi.setOnItemSelectedListener((requireActivity() as MainActivity).모임네비게이션리스너)
//        binding.bottomNavi.setOnItemSelectedListener {
////            onNavDestinationSelected(it, navController)  << navigate()와 충돌함.
//            if(it.itemId == R.id.groupInFm){
//                Navigation.findNavController(view).navigate(R.id.action_global_groupInFm)
//            } else if(it.itemId == R.id.group_in_challenge_fm){
//                Navigation.findNavController(view).navigate(R.id.action_global_group_in_challenge_fm)
//            } else if(it.itemId == R.id.groupInChatFm){
//                Navigation.findNavController(view).navigate(R.id.action_global_groupInChatFm)
//            }
//            return@setOnItemSelectedListener false
//        }


        //초대하기 버튼
        binding.toolbarAddBt.setOnClickListener {
//            val sendIntent: Intent = Intent().apply {
//                action = Intent.ACTION_SEND
//                putExtra(Intent.EXTRA_TEXT, "http://biblewith.com/invite/${groupVm.groupInfo.get("group_no").asString}")
////                putExtra(Intent.EXTRA_TEXT, "http://biblewith.com/group/${groupVm.groupInfo.get("group_no").asString}")
//                type = "text/plain"
//            }
//              //순수 텍스트를 공유하는 인텐트 - type에 따라 이미지도 공유가능할듯
//            val shareIntent = Intent.createChooser(sendIntent, "성경with 모임 멤버 초대링크 공유")
//            startActivity(shareIntent)

            findNavController().navigate(R.id.action_global_groupInInviteFm)
        }

        //멤버목록 탈퇴, 추방시 업데이트 해줌.
        groupVm.liveMemberL.observe(viewLifecycleOwner, Observer {
            rva.notifyDataSetChanged()
        })

        //멤버 검색
        binding.searchView.setOnQueryTextListener(object: androidx.appcompat.widget.SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }
            override fun onQueryTextChange(query: String): Boolean {
                CoroutineScope(Dispatchers.IO).launch {
//            Thread.sleep(100)
                    val jo = JsonObject()
                    jo.addProperty("user_no", MyApp.userInfo.user_no)
                    jo.addProperty("group_no", groupVm.groupInfo.get("group_no").asString)
                    jo.addProperty("search_word", query)
                    groupVm.모임멤버검색(jo, true)
                }
                return true
            }
        })




    }

    override fun onResume() {
        super.onResume()

        //멤버 목록 제일 상단의 내정보 view를 업데이트함 - 모임상세정보가져오기의 memberL 에서 내정보만 뽑아내어 넣음
        binding.writerTv.text = MyApp.userInfo.user_nick
        val myInfo = groupVm.memberL.run {
            val me = this.filter {
                (it as JsonObject).get("user_no").asInt == MyApp.userInfo.user_no
            }
            me.get(0).asJsonObject/*.get("join_date").asString*/
        }
        binding.dateTv.text = "참가일 ${MyApp.getTime(".ui", myInfo.get("join_date").asString)}"
        if(!myInfo.get("user_image").isJsonNull){
            ImageHelper.getImageUsingGlide(requireActivity(), myInfo.get("user_image").asString, binding.iv)
        }

        //자신이 모임장일 경우 탈퇴버튼 안보이게하기, 아니면 보이게 하기
        if (groupVm.groupInfo.get("user_no").asInt == MyApp.userInfo.user_no) {
            binding.optionBt.visibility = View.GONE
        } else {
            binding.optionBt.visibility = View.VISIBLE
        }
        //탈퇴하기 클릭시
        binding.optionBt.setOnClickListener {
            //모임 참가 여부 묻기
            AlertDialog.Builder(requireActivity())
                .setTitle("모임탈퇴")
                .setMessage("모임에서 탈퇴하시겠습니까?")
                .setNeutralButton("취소") { dialogInterface, i ->
                    Toast.makeText(requireActivity(),"취소하였습니다.",Toast.LENGTH_SHORT).show()
                }
                .setPositiveButton("확인") { dialogInterface, i ->
                    CoroutineScope(Dispatchers.Main).launch {
                        val jo = JsonObject()
                        jo.addProperty("user_no", MyApp.userInfo.user_no)
                        jo.addProperty("group_no", groupVm.groupInfo.get("group_no").asInt)
                        groupVm.모임멤버탈퇴(jo, true)
                        findNavController().navigate(R.id.action_global_group_fm) //탈퇴후 모임홈으로 가게한다
                    }
                }
                .setCancelable(false)
                .create()
                .show()
        }


        //상단바 프로필 이미지 클릭시
        binding.toolbarIv.setOnClickListener {
            findNavController().navigate(R.id.action_global_myProfileFm)
        }
        //상단바 프로필 이미지 로딩
        ImageHelper.getImageUsingGlide(requireActivity(), MyApp.userInfo.user_image, binding.toolbarIv)


    }

    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }











}