package com.example.androidclient.moreinfo

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.androidclient.databinding.MyProfileFmBinding
import com.example.androidclient.group.GroupVm
import com.example.androidclient.MainActivity
import com.example.androidclient.login.LoginActivity
import com.example.androidclient.util.FileHelper
import com.example.androidclient.util.Http
import com.example.androidclient.util.ImageHelper
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.gson.JsonObject
import es.dmoral.toasty.Toasty
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyProfileFm : Fragment() {

    val tagName = "[MyProfileFm]"
    lateinit var bibleVm: BibleVm
    lateinit var groupVm: GroupVm
    lateinit var moreVm: MoreVm
    lateinit var rva: MyProfileRva
    lateinit var rv: RecyclerView
    var mbinding: MyProfileFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    var profileImageUri : Uri? = null  //사용자 프로필이미지 이미지픽커로 선택한 파일의 Uri

    // image_fab 클릭시 이미미 픽커로 클릭한 이미지 불러오는 콜백을 위한 런처
    var startForProfileImageResult = registerForActivityResult(ActivityResultContracts.GetContent()) {
//        val resultCode = result.resultCode
//        val data = result.data

//        if (resultCode == Activity.RESULT_OK) {
        if (it != null) {
            //Image Uri will not be null for RESULT_OK
//            val fileUri = data?.data!!
            val fileUri = it
            Log.e(tagName, "startForProfileImageResult fileUri: $fileUri")
            profileImageUri = fileUri// 이 런처를 호출한 메소드로 돌아가(=image_fab클릭) 로컬파일의 절대경로를 알아내 서버로 전송할 멀티파트를 알아내어 레트로핏으로 전송
            binding.profileIv.setImageURI(fileUri) //이미지뷰에 사진 적용

//            MediaScannerConnection.scanFile(requireActivity(), )

            유저프로필이미지선택()

        }
//        else if (resultCode == ImagePicker.RESULT_ERROR) {
//            Toast.makeText(requireActivity(), ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
//        } else {
//            Toast.makeText(requireActivity(), "선택 취소", Toast.LENGTH_SHORT).show()
//        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
//        callback = object : OnBackPressedCallback(true){
//            override fun handleOnBackPressed() {
//            }
//        }
//        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bibleVm = ViewModelProvider(requireActivity()).get(BibleVm::class.java)
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
        moreVm = ViewModelProvider(requireActivity()).get(MoreVm::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mbinding = MyProfileFmBinding.inflate(inflater, container, false)

        rv = binding.recentList
        rv.layoutManager = LinearLayoutManager(binding.root.context)
        rv.adapter = MyProfileRva(bibleVm, groupVm, this)
        rva = rv.adapter as MyProfileRva

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //모임 툴바 셋팅
        val navController = Navigation.findNavController(view)
//        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val appBarConfiguration = AppBarConfiguration.Builder(R.id.more_fm).build()
//        binding.groupMainCollapsingToolbar.setupWithNavController(binding.groupMainToolbar, navController, appBarConfiguration)
//        binding.groupInToolbar.setupWithNavController(navController, appBarConfiguration)
        NavigationUI.setupWithNavController( binding.toolbar, navController, appBarConfiguration )


        //스크롤 이벤트로 밑으로 갈시 바텀네비게이션 감추기
        binding.recentList.setOnScrollChangeListener(View.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
//            Log.e("[GroupFm]", "scroll ev : $scrollY , $oldScrollY" );
            if(scrollY > oldScrollY){ //이전 위치보다 높다는 말은 밑으로 스크롤 한다는 말
                (requireActivity() as MainActivity).binding.mainBottomNav.visibility = View.GONE
            } else {
                (requireActivity() as MainActivity).binding.mainBottomNav.visibility = View.VISIBLE
            }
        })

        //초대확인 클릭시 초대확인 페이지로 가기
        binding.inviteBt.setOnClickListener {
            findNavController().navigate(R.id.action_myProfileFm_to_groupInviteVerifyFm)
        }

        //이미지 픽커로 얻어온 이미지를 선택하여 db의 user_image에 적용한다.
        binding.imageFab.setOnClickListener {
            //이미지 선택기 실행
            ImagePicker.with(this)
                .galleryOnly()      //갤러리에서 가져옴
                .compress(1024)         //Final image size will be less than 1 MB(Optional)
                .maxResultSize(1080, 1080)  //Final image resolution will be less than 1080 x 1080(Optional)
                .createIntent { intent ->       //이미지 가져오는 런처에 포함될 인텐트 생성
                    startForProfileImageResult.launch("image/*")
//                    startForProfileImageResult.launch(intent)
                }
        }

        //사용자 닉네임 변경시 ui갱신
        moreVm.liveUserNick.observe(viewLifecycleOwner, Observer {
            binding.nickTv.text = it
        })

        //로그아웃 클릭시
        binding.logoutBt.setOnClickListener {
            로그아웃()
        }

    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).binding.mainBottomNav.visibility = View.GONE
        rva.notifyDataSetChanged()

        //프로필 이미지 적용한다.
        ImageHelper.getImageUsingGlide(requireContext(), MyApp.userInfo.user_image, binding.profileIv)

        //닉네임 보여주기 & 변경버튼
        binding.nickTv.text = MyApp.userInfo.user_nick
        binding.nickBt.setOnClickListener {
            findNavController().navigate(R.id.action_global_nickModifyDialogFm)
        }


    }


    fun 유저프로필이미지선택(){
        val uploadO = mutableMapOf<String, RequestBody>()
        uploadO["user_no"] = MyApp.userInfo.user_no.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        Log.e("오류태그", "????")

        //선택한 이미지의 uri를 분기점으로 있으면 멀티파트 객체를 만들고, 아니면 호출된 메소드를 종료함
        var uploadPart: MultipartBody.Part? = null
        if(profileImageUri != null){
            val fileHelper = FileHelper()
            uploadPart = fileHelper.getPartBodyFromUri(requireActivity(), profileImageUri!!, "user_image")

            val retrofit = Http.getRetrofitInstance(Http.HOST_IP)
            val http = retrofit.create(Http.HttpLogin::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
            val call = http.사용자이미지선택(uploadO, uploadPart)
            call.enqueue(object : Callback<JsonObject?> {
                override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                    if (response.isSuccessful) {
                        val res = response.body()!!
                        val result = res.get("result").asString
                        if(result != ""){
                            //db에 이미지 업데이트 후 갱신된 정보를 다시 받아서 유저정보 갱신 후, 프로필 이미지 적용한다.
                            MyApp.userInfo.user_image = result
                            ImageHelper.getImageUsingGlide(requireContext(), MyApp.userInfo.user_image, binding.profileIv)
                            Log.e(tagName, "사용자이미지선택 성공: $result")
//                                Toast.makeText(requireActivity(),"프로필이미지가 적용되었습니다.",Toast.LENGTH_SHORT).show()
                            Toasty.success(requireActivity(), "프로필이미지가 적용되었습니다"
//                                ,AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_baseline_done_24)
                            ).show();
                        }
                    }
                }
                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    Log.e(tagName, "사용자이미지선택 onFailure: " + t.message)
                }
           })

        }
    }

    fun 로그아웃(){
        //쉐어드 프리퍼런스에서 자동로그인 정보 해제하기
//        SharedPreferences spAutologin = MyApp.getApplication().getSharedPreferences("autologin", MODE_PRIVATE);
//        SharedPreferences.Editor editor = spAutologin.edit();
//        String user_email = spAutologin.getString("user_email", "");
        val spAutologin = MyApp.application.getSharedPreferences("autologin", MODE_PRIVATE)
        val editor = spAutologin.edit()
        val user_email = spAutologin.getString("user_email", "")

        if (!user_email.equals("")) {    //null 아니면(자동로긴상태라면) 내용 삭제
            editor.clear();
            editor.apply();
        }

        val movelogin = Intent(MyApp.application, LoginActivity::class.java)
        Toasty.success(requireActivity(), "로그아웃 하였습니다.").show()
        startActivity(movelogin)
        requireActivity().finish()
    }


    override fun onPause() {
        super.onPause()
//        (requireActivity() as MainActivity).binding.mainBottomNav.visibility = View.VISIBLE
    }

    override fun onStop() {
        super.onStop()
//        (requireActivity() as MainActivity).binding.mainBottomNav.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}