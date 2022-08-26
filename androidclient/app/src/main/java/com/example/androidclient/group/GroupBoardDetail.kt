package com.example.androidclient.group

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat.setNestedScrollingEnabled
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
import com.example.androidclient.databinding.GroupBoardDetailBinding
import com.example.androidclient.util.ImageHelper
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

class GroupBoardDetail : Fragment() {
    lateinit var groupVm: GroupVm
    lateinit var imageRva: GroupBoardDetailImageRva
    lateinit var replyRva: GroupBoardDetailReplyRva
    lateinit var imageRv: RecyclerView
    lateinit var replyRv: RecyclerView
    var mbinding: GroupBoardDetailBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mbinding = GroupBoardDetailBinding.inflate(inflater, container, false)

        //이미지 리사이클러뷰 셋팅 - gboardL-gboard_image 리스트가 있을때만 어댑터 설정 & 보이기
        if(groupVm.gboardInfo.get("gboard_image").asJsonArray.size() > 0) { //jsonArray 타입은 isJsonNull 로 검사가 안됨. 요소가 없어도 []로 표시됨
            imageRv = binding.gboardDetailFmImageList
            imageRv.visibility = View.VISIBLE
            imageRv.layoutManager = LinearLayoutManager(context).apply { orientation = LinearLayoutManager.HORIZONTAL }
            imageRv.adapter = GroupBoardDetailImageRva(groupVm, this)
            imageRva  = imageRv.adapter as GroupBoardDetailImageRva
        }

        replyRv = binding.gboardDetailReplyList
        replyRv.layoutManager = LinearLayoutManager(context)
        replyRv.adapter = GroupBoardDetailReplyRva(groupVm, this)
        replyRva  = replyRv.adapter as GroupBoardDetailReplyRva

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
        NavigationUI.setupWithNavController(binding.gboardDetailToolbar, navController, appBarConfiguration)

//        binding.gboardDetailNestedScrollView.setNestedScrollingEnabled( false);
//        setNestedScrollingEnabled(replyRv, false);

        //수정,삭제 팝업메뉴
        팝업메뉴()

        //댓글 작성 버튼 클릭시
        binding.gboardDetailReplyWriteIbt.setOnClickListener {
            val sendJsonO = JsonObject()

            //0이라면 댓글로 , 아니라면 누군가의 번호에 답글. replyRva vh 에서 스낵바로 값 조정함.
            if(groupVm.currentUserNoToReply == 9999){
                sendJsonO.addProperty("user_no", MyApp.userInfo.user_no)
                sendJsonO.addProperty("gboard_no", groupVm.gboardInfo.get("gboard_no").asString)
                sendJsonO.addProperty("reply_content", binding.gboardDetailReplyWriteEt.text.toString())
                sendJsonO.addProperty("to", "reply")
    //            sendJsonO.add("note_verseL", JsonParser.parseString(gson.toJson(rva.newL)).asJsonArray)

            } else { //답글
                sendJsonO.addProperty("user_no", MyApp.userInfo.user_no)
                sendJsonO.addProperty("gboard_no", groupVm.gboardInfo.get("gboard_no").asString)
                sendJsonO.addProperty("parent_reply_no", groupVm.currentReplyNoToReply)
                sendJsonO.addProperty("parent_reply_writer_no", groupVm.currentUserNoToReply)
                sendJsonO.addProperty("reply_group", groupVm.currentReplyGroupNoToReply)
                sendJsonO.addProperty("reply_content", binding.gboardDetailReplyWriteEt.text.toString())
                sendJsonO.addProperty("to", "answer")
            }

            CoroutineScope(Dispatchers.Main).launch {
                groupVm.모임댓글쓰기(sendJsonO, true)
                groupVm.모임글상세가져오기(groupVm.gboardInfo.get("gboard_no").asInt, "GroupBoardDetail",true)
                replyRva.notifyDataSetChanged()

            }
            binding.gboardDetailReplyWriteEt.setText("") //댓글입력창 초기화
            replyRva.snackBar?.dismiss()            //스낵바 해제
            //            binding.fmWhiteBoardTvTitle.setSelection(binding.fmWhiteBoardTvTitle.length());
            //소프트키보드 내리기
            val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.gboardDetailReplyWriteEt.windowToken, 0)
        }

        //댓글 수정완료 버튼 클릭시
        binding.gboardDetailReplyModifyIbt.setOnClickListener {
            val params = JsonObject()
            params.addProperty("user_no", MyApp.userInfo.user_no)
            params.addProperty("reply_no", groupVm.gboardUpdateO.get("reply_no").asString)
            params.addProperty("gboard_no", groupVm.gboardUpdateO.get("gboard_no").asString)
            params.addProperty("reply_content", binding.gboardDetailReplyWriteEt.text.toString())

            CoroutineScope(Dispatchers.Main).launch {
                groupVm.모임댓글수정(params, true)
                groupVm.모임글상세가져오기(groupVm.gboardInfo.get("gboard_no").asInt, "GroupBoardDetail",true)
                Toast.makeText(requireContext(), "댓글을 수정하였습니다", Toast.LENGTH_SHORT).show()
            }
            binding.gboardDetailWriteReplyEtLayout.hint = "댓글을 남겨주세요"
            binding.gboardDetailReplyWriteEt.setText("") //댓글입력창 초기화
            replyRva.snackBarReplyCancel?.dismiss()            //스낵바 해제
            //            binding.fmWhiteBoardTvTitle.setSelection(binding.fmWhiteBoardTvTitle.length());
            //소프트키보드 내리기
            val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.gboardDetailReplyWriteEt.windowToken, 0)
            binding.gboardDetailReplyModifyIbt.visibility = View.INVISIBLE // 댓글 수정버튼 숨기기
            binding.gboardDetailReplyWriteIbt.visibility = View.VISIBLE // 댓글 쓰기버튼 보이기. 아이콘 모양은 같다
        }

        //좋아요 클릭시
        binding.gboardDetailFmLLLikeEaBt.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                groupVm.모임좋아요클릭(groupVm.gboardInfo.get("gboard_no").asInt, true)
                groupVm.모임글상세가져오기(groupVm.gboardInfo.get("gboard_no").asInt,"GroupBoardDetail",true)
            }
        }

        //좋아요 ui 갱신 - 내 사용자 번호가 이글에 좋아요를 누른상태라면 색 바꿈
        groupVm.liveGboardInfo.observe(viewLifecycleOwner){
            if( groupVm.gboardInfo.get("is_like").asBoolean){
                binding.gboardDetailFmLLLikeEaBt.setIconTintResource(R.color.colorLike_green)
            } else {
                binding.gboardDetailFmLLLikeEaBt.setIconTintResource(R.color.colorLike_gray)
            }
            binding.gboardDetailFmLLLikeEaBt.setText(groupVm.gboardInfo.get("like_count").asString)

        }

        //댓글 ui 갱신
        groupVm.liveGboardReplyL.observe(viewLifecycleOwner, Observer {
            replyRva.notifyDataSetChanged()
        })


    }


    

    fun 팝업메뉴(){
        //글 팝업 메뉴 클릭시 - 수정, 삭제
        binding.gboardDetailFmMenuIbt.setOnClickListener {
            //팝업 메뉴 생성 후 각 메뉴에 대한 xml 파일 인플레이트
            val groupPopupMenu = PopupMenu(MyApp.application, it)
            requireActivity().menuInflater.inflate(R.menu.group_in_popup, groupPopupMenu.menu)
            //각 메뉴항목 클릭했을때의 동작 설정
            groupPopupMenu.setOnMenuItemClickListener { menuItem ->
                when(menuItem.itemId){
                    //수정 클릭시
                    R.id.group_in_popup_update -> {
                        Toast.makeText(context, "수정", Toast.LENGTH_SHORT).show()
                        groupVm.gboardUpdateO = groupVm.gboardInfo //게시글 수정용 임시 객체
                        Navigation.findNavController(it).navigate(R.id.action_groupBoardDetail_to_groupInUpdateFm)
                        return@setOnMenuItemClickListener true
                    }
                    //삭제 클릭시
                    R.id.group_in_popup_delete -> {
                        // 다이얼로그 생성 - 삭제 여부를 물음
                        val alertdialog = AlertDialog.Builder(requireContext())
                        // 확인버튼 클릭시
                        alertdialog.setPositiveButton("확인") { dialog, which ->
                            // Toast toast = Toast.makeText(getActivity(), "확인 버튼 눌림", Toast.LENGTH_SHORT ).show;
                            //비동기 정보 가져옴
                            CoroutineScope(Dispatchers.Main).launch {
                                val resp = suspendCoroutine { cont: Continuation<Unit> ->

                                    groupVm.모임글삭제(groupVm.gboardInfo.get("gboard_no").asInt, false)!!.enqueue(object : Callback<JsonObject?> {
                                        override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                            if (response.isSuccessful) {
                                                val res = response.body()
                                                if(res!!.get("msg").asString == "ok"){
                                                    Toast.makeText(context, "글을 삭제하였습니다", Toast.LENGTH_SHORT).show()
                                                    //여기서 코루틴 써보자 안쓰면 false로 바꿔서 여기서 notifiy 불러오는 작성해야됨.
                                                    cont.resumeWith(Result.success(Unit))
                                                }
                                            }
                                        }
                                        override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                            Log.e("[groupVm]", "글삭제 onFailure: " + t.message)
                                        }
                                    })
                                }
                                groupVm.모임상세불러오기(true)
                                /*   launch {
                                       suspendCoroutine { cont: Continuation<Unit> ->
                                           groupVm.모임상세불러오기(true)
                                           cont.resumeWith(Result.success(Unit))
                                       }
                                       notifyDataSetChanged()
                                   }
                                   notifyDataSetChanged()*/
                                Navigation.findNavController(it).navigateUp() //삭제완료하면 모임상세화면(바로뒷페이지)으로 가기
                            }
                        }
                        // 취소버튼 클릭시
                        alertdialog.setNegativeButton("취소" ) { dialog, which ->
                        }
                        // 다이얼로그 빌더로 설정한 내용을 이용해 인스턴스 생성
                        val alert = alertdialog.create()
                        // 아이콘 설정
//                            alert.setIcon(R.drawable.ic_baseline_dashboard_24)
                        // 타이틀
                        alert.setTitle("삭제하시겠습니까?")
                        // 다이얼로그 띄우기
                        alert.show()

                        return@setOnMenuItemClickListener true
                    }
                    else -> {
                        return@setOnMenuItemClickListener false
                    }
                }
            }
            //위에서 설정된 글 팝업 메뉴를 화면(해당뷰)에 띄움
            groupPopupMenu.show()
        }

    }

    override fun onResume() {
        super.onResume()
        //댓글이 있을 때만 리사이클러뷰 보이기 - 댓글리사이클러뷰는 할필요없나?
//        if(groupVm.gboardInfo.get("gboard_image").asJsonArray.size() > 0) { //jsonArray 타입은 isJsonNull 로 검사가 안됨. 요소가 없어도 []로 표시됨
//            replyRv.visibility = View.VISIBLE
//        }

        val gboardInfo = groupVm.gboardInfo
        Log.e("[GroupBoardDetail]", "gboardInfo: ${groupVm.gson.toJson(gboardInfo)}")
        Log.e("[GroupBoardDetail]", "gboardReplyL: ${groupVm.gson.toJson(groupVm.gboardReplyL)}")

        //모임명 + 글쓴이
        binding.gboardDetailToolbarTv.text = "${groupVm.groupInfo.get("group_name").asString}  ${gboardInfo.get("user_nick").asString}"

        binding.gboardDetailFmWriterTv.text = gboardInfo.get("user_nick").asString
        if(!gboardInfo.get("user_image").isJsonNull){
            ImageHelper.getImageUsingGlide(requireContext(), gboardInfo.get("user_image").asString, binding.gboardDetailFmIv)
        }

        //날짜, 내용, 좋아요수, 댓글수, 히트수
        binding.gboardDetailFmDateTv.text = MyApp.getTime("ui", gboardInfo.get("create_date").asString)
        binding.gboardDetailFmContentTv.text = gboardInfo.get("gboard_content").asString
        binding.gboardDetailFmLLLikeEaBt.text = gboardInfo.get("like_count").asString
        if(gboardInfo.get("is_like").asBoolean){
            binding.gboardDetailFmLLLikeEaBt.setIconTintResource(R.color.colorLike_green)
        } else {
            binding.gboardDetailFmLLLikeEaBt.setIconTintResource(R.color.colorLike_gray)
        }
        binding.gboardDetailFmLLHitBt.text = if (!gboardInfo.get("gboard_hit").isJsonNull) gboardInfo.get("gboard_hit").asString else 0.toString()
        binding.gboardDetailFmLLReplyEaBt.text = groupVm.gboardReplyL.size().toString()


        //상단바 프로필 이미지 클릭시
        binding.gboardDetailToolbarIv.setOnClickListener {
            findNavController().navigate(R.id.action_global_myProfileFm)
        }
        //상단바 프로필 이미지 로딩
        ImageHelper.getImageUsingGlide(requireActivity(), MyApp.userInfo.user_image, binding.gboardDetailToolbarIv)



    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }
}