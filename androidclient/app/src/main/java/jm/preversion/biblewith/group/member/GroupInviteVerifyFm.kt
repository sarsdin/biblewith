package jm.preversion.biblewith.group.member
import android.content.res.ColorStateList
import android.util.Log

import android.os.Bundle
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
import androidx.navigation.ui.NavigationUI
import com.ccc.vcv.VerificationCodeView
import jm.preversion.biblewith.MyApp
import jm.preversion.biblewith.R
import jm.preversion.biblewith.databinding.GroupInviteVerifyFmBinding
import jm.preversion.biblewith.group.GroupVm
import jm.preversion.biblewith.util.Http
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GroupInviteVerifyFm : Fragment() {

    val tagName = "[GroupInviteVerifyFm]"
    lateinit var groupVm: GroupVm
    var mbinding: GroupInviteVerifyFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    var invite_code = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mbinding = GroupInviteVerifyFmBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = Navigation.findNavController(view)
//        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val appBarConfiguration = AppBarConfiguration.Builder(R.id.group_fm).build()
        NavigationUI.setupWithNavController(binding.toolbar, navController, appBarConfiguration)


        //초대하기 버튼
//        binding.toolbarAddBt.setOnClickListener {
//            val sendIntent: Intent = Intent().apply {
//                action = Intent.ACTION_SEND
//                putExtra(Intent.EXTRA_TEXT, "http://biblewith.com/invite/${groupVm.groupInfo.get("group_no").asString}")
////                putExtra(Intent.EXTRA_TEXT, "http://biblewith.com/group/${groupVm.groupInfo.get("group_no").asString}")
//                type = "text/plain"
//            }
//            val shareIntent = Intent.createChooser(sendIntent, "성경with 모임 멤버 초대링크 공유")
//            startActivity(shareIntent)
//        }


        //초대 확인 상자 모두 입력시 실행됨 -
        binding.verificationView.setOnInputVerificationCodeListener(object:VerificationCodeView.OnInputVerificationCodeListener {
            override fun onInputVerificationCodeComplete() {
//                Toast.makeText(requireActivity(),"${binding.verificationView.text}",Toast.LENGTH_SHORT).show()
                binding.verifyCard.isClickable = true //클릭 가능하게 함
                binding.verifyCard.backgroundTintList= ColorStateList.valueOf(R.color.colorInvite_green)
//                binding.verifyCard.tin(ColorStateList.valueOf(R.color.colorInvite_green))

                invite_code = binding.verificationView.text.toString()

            }
        })
        
        //위의 isClickable 이 활성화되어 클릭가능하게 될때 서버와 통신하여 멤버 추가로직 진행
        binding.verifyCard.setOnClickListener {

            //서버와 통신하여 번호가 유효한지 확인 후 모임 참가 멤버로 추가해줌
            val retrofit = Http.getRetrofitInstance(Http.HOST_IP)
            val httpGroup = retrofit.create(Http.HttpGroup::class.java)

            val call = httpGroup.모임초대번호로멤버추가하기전유효성확인(/*group_no,*/ invite_code, MyApp.userInfo.user_no)
            call.enqueue(object : Callback<JsonObject?> {
                override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                    if (response.isSuccessful) {
                        val res = response.body()!!
                        if(!res.get("result").isJsonNull){
                            val resObj = res.get("result").asJsonObject
                            Toast.makeText(requireActivity(),"유효한 비밀번호입니다.",Toast.LENGTH_SHORT).show()

                            //모임 참가 여부 묻기
                            AlertDialog.Builder(requireActivity())
                                .setTitle("${resObj.get("group_name").asString}")
                                .setMessage("모임에 참가하시겠습니까?")
                                .setNeutralButton("거부") { dialogInterface, i ->
                                    Toast.makeText(requireActivity(),"참가하지 않습니다.",Toast.LENGTH_SHORT).show()
                                }
                                .setPositiveButton("참가하기") { dialogInterface, i ->
                                    val call = httpGroup.모임초대번호로멤버추가하기(/*group_no,*/ invite_code, MyApp.userInfo.user_no)
                                    call.enqueue(object : Callback<JsonObject?> {
                                        override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                            if (response.isSuccessful) {
                                                val res = response.body()!!
                                                if(!res.get("result").isJsonNull){
//                                                    val resObj = res.get("result").asJsonObject
                                                    Toast.makeText(requireActivity(),"모임에 참가했습니다.",Toast.LENGTH_SHORT).show()
                                                    findNavController().navigateUp()
                                                }
                                                Log.e(tagName, "모임초대번호로멤버추가하기 onResponse: $res")
                                            }
                                        }
                                        override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                            Log.e(tagName, "모임초대번호로멤버추가하기 onFailure: " + t.message)
                                        }
                                    })
                                }
                                .setCancelable(false)
                                .create()
                                .show()


                        } else {
                            //유효하지 않을때 - jsonNull null 일때
                            Toast.makeText(requireActivity(),"유효하지 않은 번호입니다.",Toast.LENGTH_SHORT).show()

                        }
                        Log.e(tagName, "모임초대번호로멤버추가하기전유효성확인 onResponse: $res")
                    }
                }
                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    Log.e(tagName, "모임초대번호로멤버추가하기전유효성확인 onFailure: " + t.message)
                }
            })



        }



    }


    override fun onResume() {
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }











}