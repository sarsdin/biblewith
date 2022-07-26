package com.example.androidclient.group

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.accessibility.AccessibilityEventCompat.setAction
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide.init
import com.example.androidclient.MyApp
import com.example.androidclient.R
import com.example.androidclient.databinding.GroupBoardDetailReplyListAnswerVhBinding
import com.example.androidclient.databinding.GroupBoardDetailReplyListVhBinding
import com.example.androidclient.databinding.GroupInFmVhImageRvaVhBinding
import com.example.androidclient.util.ImageHelper
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

class GroupBoardDetailReplyRva (val groupVm: GroupVm, /*val images : JsonArray,*/ val groupBoardDetail: GroupBoardDetail)
    : RecyclerView.Adapter<GroupBoardDetailReplyRva.GroupBoardDetailReplyRvaVh>() {

    var snackBar : Snackbar? = null
    var snackBarReplyCancel : Snackbar? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupBoardDetailReplyRva.GroupBoardDetailReplyRvaVh {
        /*val binding = if(viewType == 0){
            GroupBoardDetailReplyListVhBinding.inflate(LayoutInflater.from(parent.context), parent,false)
        } else {
            GroupBoardDetailReplyListAnswerVhBinding.inflate(LayoutInflater.from(parent.context), parent,false)
        }
        val vh = GroupBoardDetailReplyRvaVh(binding)
        return vh*/
        return GroupBoardDetailReplyRvaVh(GroupBoardDetailReplyListVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    /*override fun getItemViewType(position: Int): Int {
//        super.getItemViewType(position)
        val tmpReplyInfo = groupVm.gboardReplyL[position] as JsonObject
        return if(tmpReplyInfo.get("parent_no").isJsonNull == true){
            0
        } else {
            1
        }
    }*/

    override fun onBindViewHolder(holder: GroupBoardDetailReplyRva.GroupBoardDetailReplyRvaVh, position: Int) {
        holder.bind(groupVm.gboardReplyL[position] as JsonObject)
    }

    override fun getItemCount(): Int {
        return groupVm.gboardReplyL.asJsonArray.size()
    }


    inner class GroupBoardDetailReplyRvaVh(var binding: GroupBoardDetailReplyListVhBinding) : RecyclerView.ViewHolder(binding.root) {
        lateinit var mItem: JsonObject

        init {
        }

        //mItem --
        fun bind(mItem: JsonObject) {
            this.mItem = mItem
//        Log.e("[GroupBoardDetailReplyRva]", "test1231 : ${mItem.get("parent_reply_no").isJsonNull}")

            //최상위 댓글이면
            if(mItem.get("parent_reply_no").isJsonNull == true){
                binding.answer.visibility = View.GONE
                binding.reply.visibility = View.VISIBLE
                //내 댓글이면 수정,삭제버튼 활성
                if(mItem.get("user_no").asInt == MyApp.userInfo.user_no){
                    binding.replyProfileWriterFab.visibility = View.VISIBLE
                    binding.replyProfileWriterFab.setOnClickListener {
                        댓글수정삭제버튼메뉴활성(it, "reply")
                    }
                }
                if (mItem.get("user_image").isJsonNull == false) {
                    ImageHelper.getImageUsingGlide(groupBoardDetail.requireContext(), mItem.get("user_image").asString, binding.replyIv)
                }
                binding.replyWriterTv.text = mItem.get("user_nick").asString
                binding.replyDateTv.text = MyApp.getTime("ui",mItem.get("reply_writedate").asString)
                binding.replyContentTv.text = mItem.get("reply_content").asString
//                Log.e("[GroupBoardDetailReplyRva]", "test reply : $mItem")


            //하위 댓글이면
            } else {
                binding.reply.visibility = View.GONE
                binding.answer.visibility = View.VISIBLE
                //내 댓글이면 수정,삭제버튼 활성
                if(mItem.get("user_no").asInt == MyApp.userInfo.user_no){
                    binding.answerProfileWriterFab.visibility = View.VISIBLE
                    binding.answerProfileWriterFab.setOnClickListener {
                        댓글수정삭제버튼메뉴활성(it, "answer")
                    }
                }
                if (mItem.get("user_image").isJsonNull == false) {
                    ImageHelper.getImageUsingGlide(groupBoardDetail.requireContext(), mItem.get("user_image").asString, binding.answerIv)
                }

                binding.answerToWriterTv.text = " @${mItem.get("parent_nick").asString}"
                binding.answerWriterTv.text = mItem.get("user_nick").asString
                binding.answerDateTv.text = MyApp.getTime("ui",mItem.get("reply_writedate").asString)
                binding.answerContentTv.text = mItem.get("reply_content").asString
//                Log.e("[GroupBoardDetailReplyRva]", "test answer : $mItem")
            }


            //댓글 클릭시 스낵바 실행 - 클릭시 그 댓글 유저에게 보낼 정보를 셋팅하고 취소시 초기화하여 새로운 댓글을 씀
            binding.root.setOnClickListener {
                //수정이었을 경우 쓰기로 초기화하기
                snackBarReplyCancel?.dismiss()
                groupBoardDetail.binding.gboardDetailWriteReplyEtLayout.hint = "댓글을 남겨주세요"
                groupBoardDetail.binding.gboardDetailReplyWriteEt.setText("") //입력창 초기화
                groupBoardDetail.binding.gboardDetailReplyWriteEt.clearFocus() //et에 포커싱부터 맞춰야 showSoftInput 메소드가 반응함
                groupBoardDetail.binding.gboardDetailReplyModifyIbt.visibility = View.GONE // 댓글 수정버튼 숨기기
                groupBoardDetail.binding.gboardDetailReplyWriteIbt.visibility = View.VISIBLE // 댓글 쓰기버튼 보이기. 아이콘 모양은 같다

                //소프트키보드 보이기 및 애니메이션 적용
                groupBoardDetail.binding.gboardDetailReplyWriteEt.requestFocus() //et에 포커싱부터 맞춰야 showSoftInput 메소드가 반응함
                val imm = (groupBoardDetail.requireActivity()).getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(groupBoardDetail.binding.gboardDetailReplyWriteEt, 0) //직접 뷰를 설정해서 보이는 방법
//                imm.showSoftInput( (groupBoardDetail.requireActivity()).currentFocus , 0) //포커싱메소드로 보이는 방법
                선택애니메이션적용(mItem)

                groupVm.currentReplyNoToReply = mItem.get("reply_no").asInt
                groupVm.currentUserNoToReply = mItem.get("user_no").asInt //이 댓글쓴 유저의 번호 - 부모 번호가 됨
                groupVm.currentReplyGroupNoToReply = mItem.get("reply_group").asInt // 이 댓글의 최상위 부모 번호
//                Log.e("[GroupBoardDetailReplyRva]", "test groupVm.currentReplyNoToReply : ${groupVm.currentReplyNoToReply}")
//                Log.e("[GroupBoardDetailReplyRva]", "test groupVm.currentUserNoToReply : ${groupVm.currentUserNoToReply}")
//                Log.e("[GroupBoardDetailReplyRva]", "test groupVm.currentReplyGroupNoToReply : ${groupVm.currentReplyGroupNoToReply}")
                snackBar = Snackbar.make(groupBoardDetail.binding.gboardDetailWriteReplyEtLayout,
                    "${mItem.get("user_nick")} 님께 쓰기", Snackbar.LENGTH_INDEFINITE).apply {
                    setAction("취소") { //확인을 눌렀을 때 동작을 정의
                        //소프트키보드 숨기기
                        groupBoardDetail.binding.gboardDetailReplyWriteEt.clearFocus() //et에 포커싱부터 맞춰야 showSoftInput 메소드가 반응함
                        val imm = (groupBoardDetail.requireActivity()).getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(groupBoardDetail.binding.gboardDetailReplyWriteEt.windowToken, 0)
//                        Toast.makeText(applicationContext, "Hello Snackbar!", Toast.LENGTH_SHORT).show()
                        groupVm.currentReplyNoToReply = 0
                        groupVm.currentUserNoToReply = 9999 //0일때는 답글이 아닌 댓글로 판단한다. 댓글 api를 보내야함
                        groupVm.currentReplyGroupNoToReply = 0
                    }
                }
                setSnackBarOption(snackBar!!) // 스낵바 옵션 설정
                snackBar!!.show()
            }

            //게시물의 댓글 클릭시 groupVm.clickedReplyNoGroupIn에 그 댓글번호 저장후 홀더의 댓글번호와 비교하여 맞으면 그 홀더포지션으로 스크롤 이동
            if(mItem.get("reply_no").asInt == groupVm.clickedReplyNoGroupIn){
//                Toast.makeText(groupBoardDetail.requireActivity(),
//                    "${mItem.get("reply_no").asInt } ${groupVm.clickedReplyNoGroupIn} $absoluteAdapterPosition" ,Toast.LENGTH_SHORT).show()
                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    //이 홀더뷰의 번호가 이전페이지에서 클릭한 댓글의 번호랑 같기 때문에 이 홀더뷰의 절대위치로 스크롤을 이용하면된다.
                    //핸들러 사용이유: 스크롤뷰의 오류때문에 약간의 시간격차를 두고 코드를 실행시켜야 정상적으로 스크롤이 이동한다.
                    val childY = groupBoardDetail.binding.gboardDetailReplyList.y +
                            groupBoardDetail.binding.gboardDetailReplyList.getChildAt(absoluteAdapterPosition).y
//                    groupBoardDetail.binding.gboardDetailReplyList.scrollTo(200, 1000)
//                    groupBoardDetail.binding.gboardDetailReplyList.scrollToPosition(7)
//                    Log.i ("정보태그", "${groupBoardDetail.binding.gboardDetailReplyList.getChildAdapterPosition(binding.root)}")
//                    groupBoardDetail.binding.gboardDetailReplyList.scrollToPosition(absoluteAdapterPosition)
                    //위의 리사이클러뷰의 메소드를 이용한 코드들은 작동하지 않는다. 스크롤뷰안에 있기때문이다.
                    groupBoardDetail.binding.gboardDetailNestedScrollView.scrollTo(0, childY.toInt())
                    //해당 댓글 애니메이션 적용
                    선택애니메이션적용(mItem)
                    groupVm.clickedReplyNoGroupIn = 0 //초기화 - 댓글을 눌러서만 들어갈때 실행해주고 그페이지에서 새로고침시부터는 실행안함
                }, 250)
            }






        } //bind() end



        private fun 댓글수정삭제버튼메뉴활성(it:View, replyOrAnswer:String) {
            //댓글 팝업 메뉴 클릭시 - 수정, 삭제

            //팝업 메뉴 생성 후 각 메뉴에 대한 xml 파일 인플레이트
            val groupPopupMenu = PopupMenu(MyApp.application, it)
            groupBoardDetail.requireActivity().menuInflater.inflate(R.menu.group_in_popup, groupPopupMenu.menu)
            //각 메뉴항목 클릭했을때의 동작 설정
            groupPopupMenu.setOnMenuItemClickListener { menuItem ->
                when(menuItem.itemId){
                    //수정 클릭시
                    R.id.group_in_popup_update -> {
                        snackBar?.dismiss()
//                        Toast.makeText(groupBoardDetail.requireContext(), "수정", Toast.LENGTH_SHORT).show()
                        //View.GONE 으로 버튼을 없앴다가 다른 버튼을 살리면 에디트텍스트의 width가 쪼그라드는 현상이 생김. INVISIBLE로 설정하면 괜찮아짐.
                        groupBoardDetail.binding.gboardDetailReplyWriteIbt.visibility = View.INVISIBLE // 댓글 쓰기버튼 숨기기. 아이콘 모양은 같다
                        groupBoardDetail.binding.gboardDetailReplyModifyIbt.visibility = View.VISIBLE // 댓글 수정버튼 보이기. 아이콘 모양은 같다
                        groupVm.gboardUpdateO = mItem //게시글 수정용 임시 객체
                        //현재 수정할 댓글 내용을 댓글입력창에 옴기기
                        groupBoardDetail.binding.gboardDetailReplyWriteEt.setText(groupVm.gboardUpdateO.get("reply_content").asString)
                        groupBoardDetail.binding.gboardDetailWriteReplyEtLayout.setHint("댓글을 수정해주세요")

                        //스낵바에 취소 셋팅
                        //소프트키보드 보이기 및 애니메이션 적용
                        groupBoardDetail.binding.gboardDetailReplyWriteEt.requestFocus() //et에 포커싱부터 맞춰야 showSoftInput 메소드가 반응함
                        val imm = (groupBoardDetail.requireActivity()).getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(groupBoardDetail.binding.gboardDetailReplyWriteEt, 0) //직접 뷰를 설정해서 보이는 방법
//                       imm.showSoftInput( (groupBoardDetail.requireActivity()).currentFocus , 0) //포커싱메소드로 보이는 방법
                        선택애니메이션적용(mItem)
//                            groupVm.currentReplyNoToReply = mItem.get("reply_no").asInt
//                            groupVm.currentUserNoToReply = mItem.get("user_no").asInt //이 댓글쓴 유저의 번호 - 부모 번호가 됨
//                            groupVm.currentReplyGroupNoToReply = mItem.get("reply_group").asInt // 이 댓글의 최상위 부모 번호
                        Log.e("[GroupBoardDetailReplyRva]", "test groupVm.currentReplyNoToReply : ${groupVm.currentReplyNoToReply}")
                        Log.e("[GroupBoardDetailReplyRva]", "test groupVm.currentUserNoToReply : ${groupVm.currentUserNoToReply}")
                        Log.e("[GroupBoardDetailReplyRva]", "test groupVm.currentReplyGroupNoToReply : ${groupVm.currentReplyGroupNoToReply}")
                        val toTmp = if(mItem.get("parent_nick").isJsonNull == true){
                            "댓글을 수정하는 중입니다."
                        } else {
                            "${mItem.get("parent_nick").asString}님에게 쓴 댓글 수정중"
                        }
                        snackBarReplyCancel = Snackbar.make(groupBoardDetail.binding.gboardDetailWriteReplyEtLayout,
                            toTmp, Snackbar.LENGTH_INDEFINITE).apply {
                            setAction("취소") { //취소 눌렀을 때 동작을 정의
                                //소프트키보드 숨기기
                                groupBoardDetail.binding.gboardDetailWriteReplyEtLayout.hint = "댓글을 남겨주세요"
                                groupBoardDetail.binding.gboardDetailReplyWriteEt.setText("") //입력창 초기화
                                groupBoardDetail.binding.gboardDetailReplyWriteEt.clearFocus() //et에 포커싱부터 맞춰야 showSoftInput 메소드가 반응함
                                val imm = (groupBoardDetail.requireActivity()).getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                imm.hideSoftInputFromWindow(groupBoardDetail.binding.gboardDetailReplyWriteEt.windowToken, 0)
                                groupBoardDetail.binding.gboardDetailReplyModifyIbt.visibility = View.INVISIBLE // 댓글 수정버튼 숨기기
                                groupBoardDetail.binding.gboardDetailReplyWriteIbt.visibility = View.VISIBLE // 댓글 쓰기버튼 보이기. 아이콘 모양은 같다
                            }

                            //댓글버튼클릭 이벤트안에 수정상황에 쓰여져있던 내용과 수정버튼을 그저 댓글쓰기용 버튼으로 초기화하고 쓰기버튼으로 visibility만 초기화 해주면 되는것을
                            //착각으로 인해 콜백까지 넣으면서 쓸대없는 삽질을 하였다.
                            /*addCallback(object :BaseTransientBottomBar.BaseCallback<Snackbar>(){
                                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                    super.onDismissed(transientBottomBar, event)
                                    if(event != Snackbar.Callback.DISMISS_EVENT_ACTION){
                                        if(snackBarReplyCancel == null){
                                            //소프트키보드 숨기기
                                            groupBoardDetail.binding.gboardDetailWriteReplyEtLayout.hint = "댓글을 남겨주세요"
                                            groupBoardDetail.binding.gboardDetailReplyWriteEt.setText("") //입력창 초기화
                                            groupBoardDetail.binding.gboardDetailReplyWriteEt.clearFocus() //et에 포커싱부터 맞춰야 showSoftInput 메소드가 반응함
    //                                        val imm = (groupBoardDetail.requireActivity()).getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    //                                        imm.hideSoftInputFromWindow(groupBoardDetail.binding.gboardDetailReplyWriteEt.windowToken, 0)
                                            groupBoardDetail.binding.gboardDetailReplyModifyIbt.visibility = View.GONE // 댓글 수정버튼 숨기기
                                            groupBoardDetail.binding.gboardDetailReplyWriteIbt.visibility = View.VISIBLE // 댓글 쓰기버튼 보이기. 아이콘 모양은 같다
                                        }
                                        if(snackBar != null ){
                                            groupBoardDetail.binding.gboardDetailWriteReplyEtLayout.hint = "댓글을 남겨주세요"
                                            groupBoardDetail.binding.gboardDetailReplyWriteEt.setText("") //입력창 초기화
                                            groupBoardDetail.binding.gboardDetailReplyWriteEt.clearFocus() //et에 포커싱부터 맞춰야 showSoftInput 메소드가 반응함
                                            groupBoardDetail.binding.gboardDetailReplyModifyIbt.visibility = View.GONE // 댓글 수정버튼 숨기기
                                            groupBoardDetail.binding.gboardDetailReplyWriteIbt.visibility = View.VISIBLE // 댓글 쓰기버튼 보이기. 아이콘 모양은 같다
                                        }
                                    }
                                }
                            })*/
                        }
                        setSnackBarOption(snackBarReplyCancel!!) // 스낵바 옵션 설정
                        snackBarReplyCancel!!.show()
//                            Navigation.findNavController(it).navigate(R.id.action_groupBoardDetail_to_groupInUpdateFm)
                        return@setOnMenuItemClickListener true
                    }
                    //삭제 클릭시
                    R.id.group_in_popup_delete -> {
                        // 다이얼로그 생성 - 삭제 여부를 물음
                        val alertdialog = AlertDialog.Builder(groupBoardDetail.requireContext())
                        // 확인버튼 클릭시
                        alertdialog.setPositiveButton("확인") { dialog, which ->
                            // Toast toast = Toast.makeText(getActivity(), "확인 버튼 눌림", Toast.LENGTH_SHORT ).show;
                            val params = JsonObject()
                            params.addProperty("to", replyOrAnswer) //reply 인지 answer인지 구분
                            params.addProperty("user_no", MyApp.userInfo.user_no)
                            params.addProperty("reply_no", mItem.get("reply_no").asString)

                            //비동기 정보 가져옴
                            CoroutineScope(Dispatchers.Main).launch {
                                groupVm.모임댓글삭제(params, true)
                                groupVm.모임글상세가져오기(mItem.get("gboard_no").asInt, "GroupBoardDetail",true)
                                Toast.makeText(groupBoardDetail.requireContext(), "댓글을 삭제하였습니다", Toast.LENGTH_SHORT).show()
                            }
                        }
                        // 취소버튼 클릭시
                        alertdialog.setNegativeButton("취소" ) { dialog, which ->
                        }
                        // 다이얼로그 빌더로 설정한 내용을 이용해 인스턴스 생성
                        val alert = alertdialog.create()
                        // 아이콘 설정
                        alert.setIcon(R.drawable.ic_comment)
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


        private fun 선택애니메이션적용(mItem: JsonObject) {
            val ani = AnimationUtils.loadAnimation(groupBoardDetail.requireContext(), R.anim.blink)
            val anim = ValueAnimator.ofArgb(
                /*Color.RED, Color.YELLOW, Color.GREEN, Color.MAGENTA, Color.BLUE, Color.WHITE*/Color.GREEN
            ).apply {
                duration = 350
                repeatCount = 1
                repeatMode = ValueAnimator.REVERSE
                addUpdateListener {
                    if(mItem.get("parent_reply_no").isJsonNull == true){
                        binding.reply.setBackgroundColor(it.animatedValue as Int)
                    //분기점을 둔이유: 위의 로직이 뷰홀더를 리플과 답변으로 나눠서 어쩔수 없이..ㅠㅠ 담부턴 createVH 단계에서 바인딩객체를 던지고 차라리 캐스팅을 하여 로직을 짜자!
                    } else {
                        binding.answer.setBackgroundColor(it.animatedValue as Int)
                    }
                }
            }
            anim.start()
            if(mItem.get("parent_reply_no").isJsonNull == true){
                binding.reply.startAnimation(ani)
            } else {
                binding.answer.startAnimation(ani)
            }
        }

        // 스낵바 옵션 설정
        private fun setSnackBarOption(snackBar: Snackbar) {
            snackBar.animationMode = BaseTransientBottomBar.ANIMATION_MODE_SLIDE // 애니메이션 설정
            snackBar.setActionTextColor(Color.WHITE) // 액션 버튼 색 지정
            snackBar.setTextColor(Color.WHITE) // 안내 텍스트 색 지정
            snackBar.setBackgroundTint(Color.DKGRAY) // 백그라운드 컬러 지정

            val snackBarView = snackBar.view
            val snackBarText = snackBarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            val snackBarLayout = snackBarView.layoutParams as CoordinatorLayout.LayoutParams

            snackBarLayout.gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP/*Gravity.BOTTOM*/ // 레이아웃 위치 조정
//        snackBarLayout.width = 800 // 너비 조정
//        snackBarLayout.height = 130 // 높이 조정
            snackBarText.textAlignment = View.TEXT_ALIGNMENT_CENTER // 안내 텍스트 위치 조정
//        snackBarText.typeface = Typeface.createFromAsset(this.assets, "context.ttf") // 폰트 지정
        }

    }
}