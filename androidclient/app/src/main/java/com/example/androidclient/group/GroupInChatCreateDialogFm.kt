package com.example.androidclient.group

import android.app.*
import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.example.androidclient.MyApp
import com.example.androidclient.R
import com.example.androidclient.databinding.GroupInChatCreateDialogFmBinding
import com.example.androidclient.util.FileHelper
import com.example.androidclient.util.Http
import com.example.androidclient.util.ImageHelper
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GroupInChatCreateDialogFm : DialogFragment() {
    val tagName = "[GroupInChatCreateDialogFm]"
    lateinit var groupVm: GroupVm
    var mbinding: GroupInChatCreateDialogFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    var chatRoomImageUri : Uri? = null  //사용자 프로필이미지 이미지픽커로 선택한 파일의 Uri

    // chatRoomIv 클릭시 이미미 픽커로 클릭한 이미지 불러오는 콜백을 위한 런처 - 바로위의 chatRoomImageUri 에 이미지 Uri를 넣어둔다
//    var startForProfileImageResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
    var startForProfileImageResult = registerForActivityResult(ActivityResultContracts.GetContent()) {

//        val resultCode = result.resultCode
//        val data = result.data

//        if (resultCode == Activity.RESULT_OK) {
            //Image Uri will not be null for RESULT_OK
//            val fileUri = data?.data!!
        if (it != null) {
            val fileUri = it
            Log.e(tagName, "startForProfileImageResult fileUri: $fileUri")
            chatRoomImageUri = fileUri// 이 런처를 호출한 메소드로 돌아가(=chatRoomIv클릭) 로컬파일의 절대경로를 알아내 서버로 전송할 멀티파트를 알아내어 레트로핏으로 전송
            binding.chatRoomIv.setImageURI(fileUri) //이미지뷰에 사진 적용
        }

//        } else if (resultCode == ImagePicker.RESULT_ERROR) {
//            Toast.makeText(requireActivity(), ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
//        } else {
//            Toast.makeText(requireActivity(), "선택 취소", Toast.LENGTH_SHORT).show()
//        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater,  container: ViewGroup?,  savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        mbinding = GroupInChatCreateDialogFmBinding.inflate(inflater, container, false)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        binding.content.setText("${MyApp.userInfo.user_nick}")

        //타이틀 입력
        binding.titleInput.setOnClickListener {
            //소프트키보드 보이기 및 애니메이션 적용
            binding.titleInput.clearFocus()
            binding.titleInput.requestFocus() //et에 포커싱부터 맞춰야 showSoftInput 메소드가 반응함
            val imm = (requireActivity()).getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    //        imm.showSoftInput(binding.content, 0) //직접 뷰를 설정해서 보이는 방법
            imm.showSoftInput(requireActivity().currentFocus, 0) //직접 뷰를 설정해서 보이는 방법

        }

        //소개글 입력
        binding.descInput.setOnClickListener {
            //소프트키보드 보이기 및 애니메이션 적용
            binding.descInput.clearFocus()
            binding.descInput.requestFocus() //et에 포커싱부터 맞춰야 showSoftInput 메소드가 반응함
            val imm = (requireActivity()).getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            //        imm.showSoftInput(binding.content, 0) //직접 뷰를 설정해서 보이는 방법
            imm.showSoftInput(requireActivity().currentFocus, 0) //직접 뷰를 설정해서 보이는 방법

        }

        //이미지 입력
        binding.chatRoomIv.setOnClickListener {
            //이미지 선택기 실행
            ImagePicker.with(this)
                .galleryOnly()      //갤러리에서 가져옴
                .compress(1024)         //Final image size will be less than 1 MB(Optional)
                .maxResultSize(1080, 1080)  //Final image resolution will be less than 1080 x 1080(Optional)
                .createIntent { intent ->       //이미지 가져오는 런처에 포함될 인텐트 생성
//                    startForProfileImageResult.launch(intent)
                    startForProfileImageResult.launch("image/*")
                }

        }

        binding.confirmbt.setOnClickListener {
//            채팅방이미지선택()
            채팅방만들기()
        }
        binding.cancelbt.setOnClickListener {
            dismiss()
        }

    }


    fun 채팅방만들기(){
        val uploadO = mutableMapOf<String, RequestBody>()
        uploadO["user_no"] = MyApp.userInfo.user_no.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        uploadO["chat_room_title"] = binding.titleInput.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        uploadO["chat_room_desc"] = binding.descInput.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        uploadO["group_no"] = groupVm.groupInfo.get("group_no").asString.toRequestBody("text/plain".toMediaTypeOrNull())
        Log.e("오류태그", "????")

        //선택한 이미지의 uri를 분기점으로 있으면 멀티파트 객체를 만들고, 아니면 호출된 메소드를 종료함
        var uploadPart: MultipartBody.Part? = null
        if(chatRoomImageUri != null){
            val fileHelper = FileHelper()
            uploadPart = fileHelper.getPartBodyFromUri(requireActivity(), chatRoomImageUri!!, "chat_room_image")

            val retrofit = Http.getRetrofitInstance(Http.HOST_IP)
            val http = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
            val call = http.채팅방만들기(uploadO, uploadPart)
            call.enqueue(object : Callback<JsonObject?> {
                override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                    if (response.isSuccessful) {
                        val res = response.body()!!
                        //result == 서버에서 받아온 채팅방의 유저목록을 채팅방
                        if(!res.get("result").isJsonNull){
                            //채팅방정보 - 방제, 방장정보, 생성일...
                            groupVm.chatRoomInfo = res.get("result").asJsonObject.get("chat_room_info").asJsonArray.run {
                                if(size() == 0) JsonObject() else get(0).asJsonObject //php find()메소드라 0번인덱스로 받아야함..ㅠ
                            }
                            groupVm.liveChatRoomInfo.value = groupVm.chatRoomInfo
                            //참가유저목록
                            groupVm.chatRoomInfoL = res.get("result").asJsonObject.get("chat_room_userL").asJsonArray
                            groupVm.liveChatRoomInfoL.value = groupVm.chatRoomInfoL
                            //채팅목록 - 일단 더미 데이터를 넣는다. 만들어진 시점에 채팅이 없기 때문이다.
                            groupVm.chatL = JsonArray()
                            groupVm.liveChatL.value = groupVm.chatL

                            Toasty.success(requireActivity(), "${binding.titleInput.text} 채팅방이 만들어졌습니다."
//                                ,AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_baseline_done_24)
                            ).show();

                            //방만들어지면 그방으로 이동
                            CoroutineScope(Dispatchers.Main).launch {
                                groupVm.채팅방참가클릭(
                                    groupVm.chatRoomInfo.asJsonObject.get("chat_room_no").asInt,
                                    MyApp.userInfo.user_no,
                                    groupVm.groupInfo.get("group_no").asInt,
                                    true
                                )
                                //화면 이동할때 쓰이는 라이프사이클 관련 메소드(setCurrentState)는 navigation에 연관되는듯.
                                // 그래서 Method setCurrentState must be called on the main thread 예외가 뜬다. IO는 다른쓰레드풀에서 작동하기에 Main에서 돌려준다.
                                findNavController().navigate(R.id.action_global_groupChatInnerFm)
                            }
                        }
                    }
                }
                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    Log.e(tagName, "채팅방만들기 onFailure: " + t.message)
                }
            })
        }
    }



    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        dismiss()
        //true로바꾸면 방생성이 취소됐다는 것을 vm을 통해 옵저버에 알리고 groupInChatFm 채팅방목록갱신을 다시 시작함
        groupVm.liveChatCreateDialogFmIsDismiss.value = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }
}






//
////날짜 설정
//fun showDate() {
//    val datePickerDialog = DatePickerDialog(getContext(),
//        { view, year, month, dayOfMonth ->
//            y = year
//            m = month
//            d = dayOfMonth
//            Log.e("AlarmFmDialog", "alarmdto: $y$m$d")
//        }, y, m, d
//    )
//    datePickerDialog.setOnDismissListener {
//        showTime() //*********시간설정을 실행 시킴**********
//        //                Toast.makeText(getActivity(), "date사라짐",Toast.LENGTH_SHORT).show();
//        //                Log.e("AlarmFmDialog", "alarmdto: "+AlarmFmDialog.this.alarmFmDTO);
//    }
//    datePickerDialog.setMessage("경기알림")
//    datePickerDialog.show()
//    //                Log.e("AlarmFmDialog", "alarmdto: "+AlarmFmDialog.this.alarmFmDTO);
//}
//
////시간 설정
//fun showTime() {
//    val timePickerDialog = TimePickerDialog(getActivity(),
//        { view, hourOfDay, minute ->
//            h = hourOfDay
//            mi = minute
//            //                Log.e("AlarmFmDialog", "alarmdto: "+AlarmFmDialog.this.alarmFmDTO);
//        }, h, mi, true
//    )
//    timePickerDialog.setOnDismissListener { //마지막에 시간설정 후에는 갱신된 정보들을 vm에 보낼 객체에 업데이트해준다.
//        alarmFmDTO.y = y
//        alarmFmDTO.m = m
//        alarmFmDTO.d = d
//        alarmFmDTO.h = h
//        alarmFmDTO.mi = mi
//        if (getArguments().get("crud").equals("read")) {
//            alarmVm.modifyAlarm(alarmFmDTO, getArguments().get("currentItemPosition"))
//        } else if (getArguments().get("crud").equals("write")) {
//            alarmVm.writeAlarm(alarmFmDTO) //갱신된 정보들 업데이트 해줌.
//        }
//        Log.e("AlarmFmDialog", "time onDismiss() alarmdto: " + alarmFmDTO)
//        val ldt: LocalDateTime = LocalDateTime.of(
//            alarmFmDTO.y,
//            alarmFmDTO.m + 1,
//            alarmFmDTO.d,
//            alarmFmDTO.h,
//            alarmFmDTO.mi
//        ) //월이 1~12의 숫자라 알람매니저에 쓸려면 +1해야된다. 알람매니저는 0~11로 인식되기때문.(=Calendar.MONTH)
//        if (ldt.isAfter(LocalDateTime.now())) { //ldt의 시간이 인수의 시간과 비교해서 이후시간이면(지나지 않았다면)
//            startAlarm(alarmFmDTO)
//        }
//        dismiss()
//    }
//    timePickerDialog.setMessage("메시지")
//    timePickerDialog.show()
//    Log.e("AlarmFmDialog", "alarmdtoEnd: " + alarmFmDTO)
//}
//
//private fun getTime(yyMMddhhmm: String): Int {
//    val mNow = System.currentTimeMillis()
//    val mDate = Date(mNow)
//    var res = 0
//    if (yyMMddhhmm == "yy") {
//        res = Integer.valueOf(SimpleDateFormat("yyyy").format(mDate))
//    } else if (yyMMddhhmm == "MM") {
//        res = Integer.valueOf(SimpleDateFormat("MM").format(mDate))
//    } else if (yyMMddhhmm == "dd") {
//        res = Integer.valueOf(SimpleDateFormat("dd").format(mDate))
//    } else if (yyMMddhhmm == "hh") {
//        res =
//            Integer.valueOf(SimpleDateFormat("kk").format(mDate)) //kk가 1~24 , hh가 1~12  , HH가 0~23
//    } else if (yyMMddhhmm == "mm") {
//        res = Integer.valueOf(SimpleDateFormat("mm").format(mDate))
//    } else {
//        res = 0
//    }
//    return res
//}
//
//private fun startAlarm(dto: AlarmFmDTO) {
//    val alarmManager = getActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
//    val intent = Intent(getActivity(), AlarmReceiver::class.java)
//    //        Log.e("AlarmFmDialog", "AlarmFmDialogDTO: "+ dto.toString() );
//    intent.putExtra("customName", "여기에식별값을정하자..")
//    intent.putExtra("title", dto.title)
//    intent.putExtra("content", dto.content)
//    intent.putExtra("no", dto.no)
//
//
//    //PendingIntent 자체가 액티비티, 브로드캐스트, 서비스 등을 생성할 수 있는 메소드를 가짐. 그리고 이 PendingIntent를 실행하면 생성한 인스턴스에 해당하는
//    //액티비티 or 브로드캐스트 or 서비스가 실행됨. 밑의 경우엔 브로드캐스트.
//    val pendingIntent = PendingIntent.getBroadcast(getActivity(), dto.no, intent, 0)
//    val c: Calendar = Calendar.getInstance()
//    c.set(Calendar.HOUR_OF_DAY, dto.h)
//    c.set(Calendar.MINUTE, dto.mi)
//    c.set(Calendar.SECOND, 0)
//    c.set(Calendar.DAY_OF_MONTH, dto.d)
//    c.set(Calendar.MONTH, dto.m)
//    c.set(Calendar.YEAR, dto.y)
//    val ldt: LocalDateTime = LocalDateTime.of(
//        dto.y,
//        dto.m + 1,
//        dto.d,
//        dto.h,
//        dto.mi
//    ) //월이 1~12의 숫자라 알람매니저에 쓸려면 +1해야된다. 알람매니저는 0~11로 인식되기때문.(=Calendar.MONTH)
//    if (ldt.isBefore(LocalDateTime.now())) { //ldt의 시간이 인수의 시간과 비교해서 이전시간이면(지났으면), 하루 뒤에 다시 알림 처리
////            ldt = ldt.plusDays(1);
//        return  //지난 시간은 알림 예약 안하고 메소드 종료.
//    }
//    val longtime: Long =
//        ldt.toInstant(ZoneOffset.UTC).toEpochMilli() //ZoneId.of("Asia/Seoul").getId()
//    val longtime2: Long =
//        ldt.toInstant(ZoneOffset.of("+9")).toEpochMilli() //ZoneId.of("Asia/Seoul").getId()
//    //        alarmManager.setExact(AlarmManager.RTC_WAKEUP, /*longtime*/c.getTimeInMillis(), pendingIntent);  //실제로 알람을 울리게 하는 작업. 지정된 c.시간에 pendingintent를 수행함.
//    alarmManager.setExact(
//        AlarmManager.RTC_WAKEUP,
//        longtime2,
//        pendingIntent
//    ) //실제로 알람을 울리게 하는 작업. 지정된 c.시간에 pendingintent를 수행함.
//    //alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), 1*60*1000 ,  pendingIntent);
////        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, longtime2, 300000/*5분*/, pendingIntent); //5분간격 cancel명령 전까지 무한 반복
//    Log.e("AlarmFmDialog", "ldt longtime UTC: $longtime")
//    Log.e("AlarmFmDialog", "ldt longtime2 UTC+9: $longtime2")
//    Log.e("AlarmFmDialog", "c.getTimeInMillis(): " + c.getTimeInMillis())
//    Log.e(
//        "AlarmFmDialog",
//        "ldt 1: " + ldt.toString()
//    ) //월이 1~12의 숫자라 알람매니저에 쓸려면 +1해야된다. 알람매니저는 0~11로 인식되기때문.(=Calendar.MONTH)
//    Log.e("AlarmFmDialog", "c 1: " + c.toString())
//}
//
//private fun cancelAlarm(dto: AlarmFmDTO) {
//    val alarmManager = getActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
//    val intent = Intent(getActivity(), AlarmReceiver::class.java)
//    val pendingIntent = PendingIntent.getBroadcast(getActivity(), dto.no, intent, 0)
//    alarmManager.cancel(pendingIntent)
//    //        mTextView.setText("Alarm canceled");
//}