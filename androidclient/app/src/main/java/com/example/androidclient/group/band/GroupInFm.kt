package com.example.androidclient.group.band
import android.net.Uri
import android.util.Log

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.MyApp
import com.example.androidclient.R
import com.example.androidclient.databinding.GroupInFmBinding
import com.example.androidclient.group.GroupVm
import com.example.androidclient.util.FileHelper
import com.example.androidclient.util.Http
import com.example.androidclient.util.ImageHelper
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.gson.JsonObject
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GroupInFm : Fragment() {

    val tagName = "[GroupInFm]"
    lateinit var groupVm: GroupVm
    lateinit var rva: GroupInRva
    lateinit var rv: RecyclerView
    var mbinding: GroupInFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
//        groupVm.모임상세불러오기(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mbinding = GroupInFmBinding.inflate(inflater, container, false)

        CoroutineScope(Dispatchers.Main).launch {
            //모임상세불러오기() - groupInfo 가 챌린지목록가져오기()안에서 참조되기 때문에 모임상세불러오기()의 안전(임시)?한 로딩을 위해 약간의 지연을 둠
            //원래는 모임상세불러오기() 자체가 코루틴등으로 안전한 비동기 로직이 수행되어야함..
            groupVm.모임상세불러오기2(true)
//            Thread.sleep(100)
            groupVm.챌린지목록가져오기(true)
//            val handler = Handler(Looper.getMainLooper())
//            handler.post {
//                rva.notifyDataSetChanged()
//            }
        }

        rv = binding.groupInList
        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = GroupInRva(groupVm, this)
        rva = rv.adapter as GroupInRva
        rv.recycledViewPool.setMaxRecycledViews(0, 20) //리사이클러뷰풀(뷰홀더보관수) 설정. max값은 default 5

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        //모임 툴바 셋팅
//        val navController = Navigation.findNavController(view)
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_main_activity)
//        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val appBarConfiguration = AppBarConfiguration.Builder(R.id.group_fm).build()
//        binding.groupMainCollapsingToolbar.setupWithNavController(binding.groupMainToolbar, navController, appBarConfiguration)
//        binding.groupInToolbar.setupWithNavController(navController, appBarConfiguration)
        setupWithNavController(binding.groupInToolbar, navController, appBarConfiguration)
//        setupWithNavController(binding.groupInBottomNavi, navController)

        //바텀네비 리스너 설정
        binding.groupInBottomNavi.setOnItemSelectedListener {
//            onNavDestinationSelected(it, navController)  << navigate()와 충돌함.
            if(it.getItemId() == R.id.group_in_challenge_fm){
                Navigation.findNavController(view).navigate(R.id.action_groupInFm_to_group_in_challenge_fm)
            } else if (it.itemId == R.id.groupInMemberFm){
                Navigation.findNavController(view).navigate(R.id.action_global_groupInMemberFm)
            } else if(it.itemId == R.id.groupInChatFm){
                Navigation.findNavController(view).navigate(R.id.action_global_groupInChatFm)
            }
            return@setOnItemSelectedListener false
        }



        //스크롤 이벤트로 밑으로 갈시 바텀네비게이션 감추기
        binding.groupInNestedScrollView.setOnScrollChangeListener(View.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
//            Log.e("[GroupFm]", "scroll ev : $scrollY , $oldScrollY" );
            if(scrollY > oldScrollY){ //이전 위치보다 높다는 말은 밑으로 스크롤 한다는 말
                binding.groupInBottomNavi.visibility = View.GONE
            } else {
                binding.groupInBottomNavi.visibility = View.VISIBLE
            }
        })

        // Ui 갱신
        groupVm.liveGroupInfo.observe(viewLifecycleOwner, Observer {
            //모임 이미지 받아오기
            ImageHelper.getImageUsingGlide(requireContext(), groupVm.groupInfo.get("group_main_image").asString
                , binding.groupInCollapsingToolbarIv )
            //모임 이름 적용
            binding.groupInSummaryNameTv.text = groupVm.groupInfo.get("group_name").asString
            binding.groupInToolbarTv.text = groupVm.groupInfo.get("group_name").asString
        })
        groupVm.liveMemberL.observe(viewLifecycleOwner, Observer {
            //멤버수
            binding.groupInSummaryMemberTv.text = groupVm.memberL.size().toString()
        })
        groupVm.liveGboardL.observe(viewLifecycleOwner, Observer {
            //게시물 갱신
            Log.e(tagName, "게시물 갱신: ${groupVm.gson.toJson(it)}")
            rva.notifyDataSetChanged()
        })

        //글쓰기 버튼 클릭시
        binding.groupInSummaryWriteBt.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_groupInFm_to_groupInWriteFm)
        }
        binding.groupInToolbarWriteBt.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_groupInFm_to_groupInWriteFm)
        }





    }

    override fun onResume() {
        super.onResume()
        //메인액티비티의 툴바는 감춤
//        (requireActivity() as MainActivity).binding.mainToolbar.visibility = View.GONE
//        (requireActivity() as MainActivity).binding.mainBottomNav.visibility = View.GONE
        binding.groupInBottomNavi.visibility = View.VISIBLE
//        groupVm.모임상세불러오기(true)
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed( Runnable {
            //모임 이미지 바꾸기 - 모임장일 때만 클릭 가능하게 설정
            // - GroupListRva에서 모임뷰홀더를 클릭하여 들어올때의 서버통신이 코루틴이 아니라 groupInfo npe 위험때문에 핸들러를 이용해 지연을 살짝줌..
            //그리고 네트워크응답이 안올경우에 대비해 !isJsonNull 로 null 이 아닐때만 실행하도록함
            if(!groupVm.groupInfo.isJsonNull && groupVm.groupInfo.get("user_no").asInt == MyApp.userInfo.user_no){
                binding.groupInCollapsingToolbarIv.setOnClickListener {
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
            }
        }, 200)


        //상단바 프로필 이미지 클릭시
        binding.groupInToolbarIv.setOnClickListener {
            findNavController().navigate(R.id.action_global_myProfileFm)
        }
        //상단바 프로필 이미지 로딩
        ImageHelper.getImageUsingGlide(requireActivity(), MyApp.userInfo.user_image, binding.groupInToolbarIv)


    }

    var profileImageUri : Uri? = null  //사용자 프로필이미지 이미지픽커로 선택한 파일의 Uri

    // image_fab 클릭시 이미미 픽커로 클릭한 이미지 불러오는 콜백을 위한 런처
    var startForProfileImageResult = registerForActivityResult(ActivityResultContracts.GetContent()) {
        if (it != null) {
            //Image Uri will not be null for RESULT_OK
            val fileUri = it
            Log.e(tagName, "startForProfileImageResult fileUri: $fileUri")
            profileImageUri = fileUri// 이 런처를 호출한 메소드로 돌아가(=image_fab클릭) 로컬파일의 절대경로를 알아내 서버로 전송할 멀티파트를 알아내어 레트로핏으로 전송
            binding.groupInCollapsingToolbarIv.setImageURI(fileUri) //이미지뷰에 사진 적용

            모임프로필이미지선택()

        }
    }

    fun 모임프로필이미지선택(){
        val uploadO = mutableMapOf<String, RequestBody>()
        uploadO["group_no"] = groupVm.groupInfo.get("group_no").asString.toRequestBody("text/plain".toMediaTypeOrNull())
        Log.e("오류태그", "????")

        //선택한 이미지의 uri를 분기점으로 있으면 멀티파트 객체를 만들고, 아니면 호출된 메소드를 종료함
        var uploadPart: MultipartBody.Part? = null
        if(profileImageUri != null){
            val fileHelper = FileHelper()
            uploadPart = fileHelper.getPartBodyFromUri(requireActivity(), profileImageUri!!, "group_main_image")

            val retrofit = Http.getRetrofitInstance(Http.HOST_IP)
            val http = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
            val call = http.모임이미지선택(uploadO, uploadPart)
            call.enqueue(object : Callback<JsonObject?> {
                override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                    if (response.isSuccessful) {
                        val res = response.body()!!
                        val result = res.get("result").asString
                        if(result != ""){
                            //db에 이미지 업데이트 후 갱신된 정보를 다시 받아서 유저정보 갱신 후, 프로필 이미지 적용한다.
//                            MyApp.userInfo.user_image = result
                            groupVm.groupInfo.addProperty("group_main_image", result)
                            ImageHelper.getImageUsingGlide(requireContext(), result, binding.groupInCollapsingToolbarIv)
                            Log.e(tagName, "모임이미지선택 성공: $result")
//                                Toast.makeText(requireActivity(),"프로필이미지가 적용되었습니다.",Toast.LENGTH_SHORT).show()
                            Toasty.success(requireActivity(), "프로필이미지가 적용되었습니다"
//                                AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_baseline_done_24)
                            ).show();
                        }
                    }
                }
                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    Log.e(tagName, "모임이미지선택 onFailure: " + t.message)
                }
            })

        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        //onViewCreated에서 감추었던 메인액티비티의 툴바를 다시 보이게 함(다른 화면에서는 보여야하므로)
//        (requireActivity() as MainActivity).binding.mainToolbar.visibility = View.VISIBLE
//        (requireActivity() as MainActivity).binding.mainBottomNav.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }
}