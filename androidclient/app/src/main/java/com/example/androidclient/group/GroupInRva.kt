package com.example.androidclient.group
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.Log

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide.init
import com.example.androidclient.MyApp
import com.example.androidclient.R
import com.example.androidclient.databinding.GroupInFmVhBinding

import com.example.androidclient.moreinfo.MyNoteRvaInner
import com.example.androidclient.util.ImageHelper
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GroupInRva(val groupVm: GroupVm, val groupInFm: GroupInFm) : RecyclerView.Adapter<GroupInRva.GroupInFmVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupInRva.GroupInFmVh {
        return GroupInFmVh(GroupInFmVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: GroupInRva.GroupInFmVh, position: Int) {
        holder.bind(groupVm.gboardL[position] as JsonObject)
    }

    override fun getItemCount(): Int {
        return groupVm.gboardL.size()
    }

    override fun onViewAttachedToWindow(holder: GroupInFmVh) {
//        holder.setIsRecyclable(false) //홀더뷰 재사용 안함으로 설정하여 홀더의 절 ui들이 독립적으로 작동하게 함
        super.onViewAttachedToWindow(holder)
    }


    inner class GroupInFmVh(var binding: GroupInFmVhBinding) : RecyclerView.ViewHolder(binding.root) {
        var rva: GroupInVhImageRva? = null
        var rv: RecyclerView? = null

        init {
        }

        //mItem -- 모임상세가져오기() - 데이터 구조 제일 아래에 예시 써놓음 gboardL[i] == mItem
        @SuppressLint("ClickableViewAccessibility")
        fun bind(mItem: JsonObject) {
//            Log.e("오류태그", "하하하하하핳 ${bindingAdapterPosition} ${groupVm.gson.toJson(mItem.get("gboard_image").asJsonArray)} ")
            //이미지 리사이클러뷰 셋팅 - gboardL-gboard_image 리스트가 있을때만 어댑터 설정 & 보이기
            if(mItem.get("gboard_image").asJsonArray.size() != 0) { //jsonArray 타입은 isJsonNull 로 검사가 안됨. 요소가 없어도 []로 표시됨
                //여기 매우 중요!! 지금 배열 요소가 없으면 이 코드가 실행이 안되는 조건문인데, 요소(데이터)가 없으면 자식 뷰홀더가 새로 생성되지 않기에
                //기존의 자식 뷰홀더를 리사이클러뷰풀에서 찾아서 재활용하게되는 기본 리사이클러뷰 로직을 따르게 된다.
                //그러면 다른 프래그먼트에서 썼던(다른모임) 리사이클러뷰(부모) 뷰홀더의 리사이클러뷰(자식)의 홀더는 재생성되지 않았기에 재활용되게 되며
                //기존의 해당 부모뷰홀더 인덱스의 리사이클러뷰(자식)에 이미지가 있었다면, 현재 리사이클러뷰(자식)에 이미지가 그대로 재활용되어 보이게 된다.
//                Log.e("[GroupInRva]", "mItem: ${groupVm.gson.toJson(mItem)}")
                rv = binding.groupInFmVhImageList
                rv?.visibility = View.VISIBLE
                rv?.layoutManager = LinearLayoutManager(groupInFm.requireContext()).apply { orientation = LinearLayoutManager.HORIZONTAL }
                rv?.adapter = GroupInVhImageRva(groupVm, mItem.get("gboard_image").asJsonArray, groupInFm) //이미지 리스트를 이미지 어댑터로 넘김
                rva = rv?.adapter as GroupInVhImageRva
            } else {
//                rv = binding.groupInFmVhImageList
                rv?.visibility = View.GONE
//                rv!!.adapter?.notifyDataSetChanged()
            }

            //닉네임, 프로필 이미지
            binding.groupInFmVhWriterTv.text = mItem.get("user_nick").asString
            if(!mItem.get("user_image").isJsonNull){
                ImageHelper.getImageUsingGlide(groupInFm.requireContext(), mItem.get("user_image").asString, binding.groupInFmVhIv)
            }
            //날짜, 내용, 좋아요 수, 댓글 수
            binding.groupInFmVhDateTv.text = MyApp.getTime("ui", mItem.get("create_date").asString)
            binding.groupInFmVhContentTv.text = mItem.get("gboard_content").asString
            binding.groupInFmVhLLLikeEaBt.text = mItem.get("gboard_like_count").asString
            binding.groupInFmVhLLReplyEaBt.text = mItem.get("reply_count").asString
            if(mItem.get("is_like").asBoolean){
//                binding.groupInFmVhLLBottomLikeBt.iconTintMode =PorterDuff.Mode.DST_OVER
//                binding.groupInFmVhLLBottomLikeBt.iconTint = ColorStateList.valueOf(R.color.colorLike_green)
                binding.groupInFmVhLLBottomLikeBt.setIconTintResource(R.color.colorLike_green)
            }

            //댓글 유무 확인
            val replyL = mItem.get("replyL").asJsonArray
            if(replyL.size() > 0){
                binding.groupInFmVhReplyCl.visibility = View.VISIBLE
                binding.groupInFmVhReplyClWriterTv.text = replyL.get(0).asJsonObject.get("user_nick").asString
                if(!replyL.get(0).asJsonObject.get("user_image").isJsonNull){
                    ImageHelper.getImageUsingGlide(groupInFm.requireContext(), replyL.get(0).asJsonObject.get("user_image").asString, binding.groupInFmVhReplyClIv)
                }
                binding.groupInFmVhReplyClContentTv.text = replyL.get(0).asJsonObject.get("reply_content").asString
                binding.groupInFmVhReplyClDateTv.text = MyApp.getTime("ui",replyL.get(0).asJsonObject.get("reply_writedate").asString)
                //댓글 클릭시
//                        Log.e("[GroupInRva]", "댓글 유무확인:  ${replyL.get(0).asJsonObject}")
                binding.groupInFmVhReplyCl.setOnTouchListener { _, event ->
                    groupVm.clickedReplyNoGroupIn = replyL.get(0).asJsonObject.get("reply_no").asInt
                    Log.e("[GroupInRva]", "1번째 reply_no ${groupVm.clickedReplyNoGroupIn}")
//                    if(event.getAction() == MotionEvent.ACTION_UP){ // actionButton 은 ACTION_BUTTON_PRESS 와 대응되는 메소드임 주의!
//                    binding.root.requestDisallowInterceptTouchEvent(false)
//                    }
//                    else {
//                    binding.root.requestDisallowInterceptTouchEvent(true)
//                    }
                    return@setOnTouchListener false
                }


                //2번째 댓글도 있으면 보이게 처리
                if(replyL.size() == 2){
                    binding.groupInFmVhReplyClSec.visibility = View.VISIBLE
                    binding.groupInFmVhReplyClSecWriterTv.text = replyL.get(1).asJsonObject.get("user_nick").asString
                    if(!replyL.get(1).asJsonObject.get("user_image").isJsonNull){
                        ImageHelper.getImageUsingGlide(groupInFm.requireContext(), replyL.get(1).asJsonObject.get("user_image").asString, binding.groupInFmVhReplyClSecIv)
                    }
                    binding.groupInFmVhReplyClSecContentTv.text = replyL.get(1).asJsonObject.get("reply_content").asString
                    binding.groupInFmVhReplyClSecDateTv.text = MyApp.getTime("ui",replyL.get(1).asJsonObject.get("reply_writedate").asString)
                    //댓글 클릭시
                    binding.groupInFmVhReplyClSec.setOnTouchListener { _, event ->
                        groupVm.clickedReplyNoGroupIn = replyL.get(1).asJsonObject.get("reply_no").asInt
                        Log.e("[GroupInRva]", "2번째 reply_no  ${groupVm.clickedReplyNoGroupIn}")
                        return@setOnTouchListener false
                    }
                }
            }





            //좋아요 클릭시
            binding.groupInFmVhLLBottomLikeBt.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    groupVm.모임좋아요클릭(mItem.get("gboard_no").asInt, true)
                    groupVm.모임상세불러오기(true)
                }
            }

            //게시글 클릭시 - 게시글 상세화면으로 가야함
            binding.root.setOnClickListener {
//                groupVm.gboardInfo = mItem //게시글
                binding.progressBar.visibility = View.VISIBLE
                CoroutineScope(Dispatchers.Main).launch {
                    groupVm.모임글상세가져오기(mItem.get("gboard_no").asInt, "GroupInRva",true)/*?.enqueue(object : Callback<JsonObject?> {
                        override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                            if (response.isSuccessful) {
                                val res = response.body()!!
                                groupVm.gboardInfo = res.get("result").asJsonObject.get("gboardInfo").asJsonObject
                                groupVm.liveGboardInfo.value = groupVm.gboardInfo
                                groupVm.gboardReplyL = res.get("result").asJsonObject.get("gboardReplyL").asJsonArray
                                groupVm.liveGboardReplyL.value = groupVm.gboardReplyL
                                Log.e("[GroupInRva]", "모임글상세가져오기 완료: $res")
                            }
                            cont.resumeWith(Result.success(Unit))
                        }
                        override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                            Log.e("[GroupVm]", "모임글상세가져오기 onFailure: " + t.message)
                        }
                    })*/
                    binding.progressBar.visibility = View.GONE
                    Navigation.findNavController(it).navigate(R.id.action_groupInFm_to_groupBoardDetail)
                }
            }



            if(mItem.get("user_no").asInt == MyApp.userInfo.user_no){
                binding.groupInFmVhMenuIbt.visibility = View.VISIBLE
            } else {
                binding.groupInFmVhMenuIbt.visibility = View.GONE
            }

            //글 팝업 메뉴 클릭시 - 수정, 삭제
            binding.groupInFmVhMenuIbt.setOnClickListener {
                //팝업 메뉴 생성 후 각 메뉴에 대한 xml 파일 인플레이트 
                val groupPopupMenu = PopupMenu(MyApp.application, it)
                groupInFm.requireActivity().menuInflater.inflate(R.menu.group_in_popup, groupPopupMenu.menu)
                //각 메뉴항목 클릭했을때의 동작 설정
                groupPopupMenu.setOnMenuItemClickListener { menuItem ->
                    when(menuItem.itemId){
                        //수정 클릭시
                        R.id.group_in_popup_update -> {
                            Toast.makeText(groupInFm.context, "수정", Toast.LENGTH_SHORT).show()
                            groupVm.gboardUpdateO = mItem //게시글 수정용 임시 객체
                            Navigation.findNavController(it).navigate(R.id.action_groupInFm_to_groupInUpdateFm)
                            return@setOnMenuItemClickListener true
                        }
                        //삭제 클릭시
                        R.id.group_in_popup_delete -> {
                            // 다이얼로그 생성 - 삭제 여부를 물음
                            val alertdialog = AlertDialog.Builder(groupInFm.requireContext())
                            // 확인버튼 클릭시
                            alertdialog.setPositiveButton("확인") { dialog, which ->
                                // Toast toast = Toast.makeText(getActivity(), "확인 버튼 눌림", Toast.LENGTH_SHORT ).show;
                                //비동기 정보 가져옴
                                CoroutineScope(Dispatchers.Main).launch {
                                    val resp = suspendCoroutine { cont: Continuation<Unit> ->

                                        groupVm.모임글삭제(mItem.get("gboard_no").asInt, false)!!.enqueue(object : Callback<JsonObject?> {
                                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                                if (response.isSuccessful) {
                                                    val res = response.body()
                                                    if(res!!.get("msg").asString == "ok"){
                                                        Toast.makeText(groupInFm.context, "글을 삭제하였습니다", Toast.LENGTH_SHORT).show()
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
                                }
                                notifyDataSetChanged()
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


    }
}


//gboardL - mItem 데이터 구조
// {
//    "gboard_no": "6",
//    "group_no": "1",
//    "user_no": "0",
//    "gboard_title": null,
//    "gboard_content": "테스트6",
//    "create_date": "2022-07-19 21:20:22",
//    "user_email": "sjeys14@gmail.com",
//    "user_pwd": "!",
//    "user_nick": "정목",
//    "user_create_date": "2022-06-21 12:26:20",
//    "user_name": "설정목",
//    "user_image": null,
//    "gboard_image": [
//    {
//        "gboard_image_no": "1",
//        "gboard_no": "6",
//        "original_file_name": null,
//        "stored_file_name": "20220719/1658233222_b166279651bca7727557.jpeg",
//        "file_size": "92048",
//        "create_date": "2022-07-19 21:20:22"
//    },