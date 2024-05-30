package jm.preversion.biblewith.moreinfo

import android.app.*
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import jm.preversion.biblewith.MyApp
import jm.preversion.biblewith.R
import jm.preversion.biblewith.databinding.MyProfileFmBinding
import jm.preversion.biblewith.databinding.NickModifyDialogFmBinding
import jm.preversion.biblewith.group.GroupVm
import jm.preversion.biblewith.util.FileHelper
import jm.preversion.biblewith.util.Http
import jm.preversion.biblewith.util.ImageHelper
import com.google.gson.JsonObject
import es.dmoral.toasty.Toasty
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NickModifyDialogFm : DialogFragment() {
    val tagName = "[NickModifyDialogFm]"
    lateinit var moreVm: MoreVm
    var mbinding: NickModifyDialogFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        moreVm = ViewModelProvider(requireActivity()).get(MoreVm::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater,  container: ViewGroup?,  savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        mbinding = NickModifyDialogFmBinding.inflate(inflater, container, false)
        return binding.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //유저 닉넴 로딩
        binding.content.setText("${MyApp.userInfo.user_nick}")
        
        //소프트키보드 보이기 및 애니메이션 적용
        binding.content.clearFocus()
        binding.content.requestFocus() //et에 포커싱부터 맞춰야 showSoftInput 메소드가 반응함
        val imm = (requireActivity()).getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.showSoftInput(binding.content, 0) //직접 뷰를 설정해서 보이는 방법
        imm.showSoftInput(requireActivity().currentFocus, 0) //직접 뷰를 설정해서 보이는 방법

        binding.confirmbt.setOnClickListener {
            유저닉네임수정()
        }
        binding.cancelbt.setOnClickListener {
            dismiss()
        }

    }
    fun 유저닉네임수정(){

            val retrofit = Http.getRetrofitInstance(Http.HOST_IP)
            val http = retrofit.create(Http.HttpLogin::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
            val call = http.유저닉네임수정(MyApp.userInfo.user_no, binding.content.text.toString())
            call.enqueue(object : Callback<JsonObject?> {
                override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                    if (response.isSuccessful) {
                        val res = response.body()!!
                        val result = res.get("result").asString
                        if(result != ""){
                            //db에 이미지 업데이트 후 갱신된 정보를 다시 받아서 유저정보 갱신 후, 프로필 이미지 적용한다.
                            MyApp.userInfo.user_nick = result
                            moreVm.userNick = result
                            moreVm.liveUserNick.value = moreVm.userNick
                            Log.e(tagName, "유저닉네임수정 성공: $result")
//                                Toast.makeText(requireActivity(),"프로필이미지가 적용되었습니다.",Toast.LENGTH_SHORT).show()
                            Toasty.success(requireActivity(), "유저닉네임 변경되었습니다"
//                                AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_baseline_done_24)
                            ).show()
                            dismiss()
                        }
                    }
                }
                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    Log.e(tagName, "유저닉네임수정 onFailure: " + t.message)
                }
            })

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