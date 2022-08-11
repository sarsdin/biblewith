package com.example.androidclient.group
import android.util.Log

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.launch
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.get
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.MyApp
import com.example.androidclient.R
import com.example.androidclient.databinding.GroupInMemberFmListVhBinding
import com.example.androidclient.databinding.GroupInUpdateImageListVhBinding
import com.example.androidclient.moreinfo.MyNoteRvaInner
import com.example.androidclient.util.ImageHelper
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

class GroupInMemberRva(val groupVm: GroupVm, val groupInMemberFm: GroupInMemberFm) : RecyclerView.Adapter<GroupInMemberRva.GroupInMemberRvaVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupInMemberRva.GroupInMemberRvaVh {
        return GroupInMemberRvaVh(GroupInMemberFmListVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: GroupInMemberRva.GroupInMemberRvaVh, position: Int) {
        holder.bind(groupVm.memberL.get(position) as JsonObject)
    }

    override fun getItemCount(): Int {
        return groupVm.memberL.size() ?: 0
    }

    override fun getItemViewType(position: Int): Int {
        return super.getItemViewType(position)
    }

    inner class GroupInMemberRvaVh(var binding: GroupInMemberFmListVhBinding) : RecyclerView.ViewHolder(binding.root) {
        var rva: MyNoteRvaInner? = null
        var rv: RecyclerView? =null

        init {
        }

        //mItem -- 멤버목록의 요소
        fun bind(mItem: JsonObject) {

            //홀더 클릭시
//            binding.root.setOnClickListener {
//                Toast.makeText(groupInUpdateFm.requireContext(),"position: $absoluteAdapterPosition",Toast.LENGTH_SHORT).show()
//            }

            //모임장일때 추방하기할 수 있는 버튼 보이기
            if(groupVm.groupInfo.get("user_no").asInt == MyApp.userInfo.user_no){
                binding.optionBt.visibility = View.VISIBLE
                //하지만, 모임장 본인의 홀더일때는 다시 숨기기
                if(mItem.get("user_no").asInt == groupVm.groupInfo.get("user_no").asInt){
                    binding.optionBt.visibility = View.GONE
                }
            }
//            binding.memberDateTv.append("df")
            binding.memberWriterTv.text = mItem.get("user_nick").asString
            binding.memberDateTv.text = "참가일 ${MyApp.getTime(".ui", mItem.get("join_date").asString)}"
            if(!mItem.get("user_image").isJsonNull){
                ImageHelper.getImageUsingGlide(groupInMemberFm.requireActivity(), mItem.get("user_image").asString, binding.memberIv)
            }




            //멤버 메뉴
            팝업메뉴(mItem)




        }

        fun 팝업메뉴(mItem: JsonObject){
            //글 팝업 메뉴 클릭시 - 수정, 삭제
            binding.optionBt.setOnClickListener {
                //팝업 메뉴 생성 후 각 메뉴에 대한 xml 파일 인플레이트
                val groupPopupMenu = PopupMenu(MyApp.application, it)
                groupInMemberFm.requireActivity().menuInflater.inflate(R.menu.group_in_member_popup, groupPopupMenu.menu)
                //현재 홀더의 유저 번호가 로그인한 사용자의 번호와 같으면 본인이기에 체크해줌
//                if(mItem.get("user_no").asInt == MyApp.userInfo.user_no){
                if(groupVm.groupInfo.get("user_no").asInt == MyApp.userInfo.user_no){ //모임장일때 추방하기 버튼 활성화
                    groupPopupMenu.menu.get(1).isVisible = true
                }

                //각 메뉴항목 클릭했을때의 동작 설정
                groupPopupMenu.setOnMenuItemClickListener { menuItem ->
                    when(menuItem.itemId){
                        //탈퇴 클릭시
                        R.id.group_in_member_popup_0 -> {
                            Toast.makeText(groupInMemberFm.requireActivity(), "탈퇴", Toast.LENGTH_SHORT).show()

                            Navigation.findNavController(it).navigate(R.id.action_global_group_fm)
                            return@setOnMenuItemClickListener true
                        }
                        //추방 클릭시
                        R.id.group_in_member_popup_1 -> {
                            // 다이얼로그 생성 - 삭제 여부를 물음
                            val alertdialog = AlertDialog.Builder(groupInMemberFm.requireActivity())
                            // 확인버튼 클릭시
                            alertdialog.setPositiveButton("확인") { dialog, which ->
                                // Toast toast = Toast.makeText(getActivity(), "확인 버튼 눌림", Toast.LENGTH_SHORT ).show;
                                //비동기 정보 가져옴
                                CoroutineScope(Dispatchers.Main).launch {
                                    val jo = JsonObject()
                                    jo.addProperty("user_no", mItem.get("user_no").asString)
                                    jo.addProperty("group_no", mItem.get("group_no").asString)
                                    groupVm.모임멤버추방(jo, true)
//                                    Navigation.findNavController(it).navigateUp() //삭제완료하면 모임상세화면(바로뒷페이지)으로 가기
//                                    notifyDataSetChanged() //갱신하기
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
                            alert.setTitle("해당 멤버를 추방하시겠습니까?")
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


//"0": { // 모임 정보 -- groupInfo
//    "group_no": "1",
//    "chat_room_no": null,
//    "user_no": "0", -- 모임장번호
//    "group_name": "테스트모임1",
//    "group_desc": "설명1",
//    "group_main_image": "20220716/1657964677_583fb954bab2aefb90d1.jpg",
//    "create_date": "2022-07-16 18:44:37"