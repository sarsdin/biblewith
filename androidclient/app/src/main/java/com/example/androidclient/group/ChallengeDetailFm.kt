package com.example.androidclient.group
import android.util.Log

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.MyApp
import com.example.androidclient.R
import com.example.androidclient.databinding.ChallengeDetailFmBinding
import com.example.androidclient.util.FileHelper
import com.example.androidclient.util.ImageHelper
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutionException


class ChallengeDetailFm : Fragment() {
    private val tagName = "ChallengeDetailFm"

    lateinit var groupVm: GroupVm
    lateinit var rva: ChallengeDetailRva
    lateinit var rv: RecyclerView
    var mbinding: ChallengeDetailFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    private var PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO) //필요 권한 배열
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider> //카메라제공자
    private lateinit var videoCapture: VideoCapture
    private lateinit var bRecord: Button
    private lateinit var bCapture: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //카메라 초기화
        initCamera()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
        mbinding = ChallengeDetailFmBinding.inflate(inflater, container, false)
        
        //권한 확인 후 없으면 권한 요청
        if (!hasPermissions(requireContext())) {
            권한요청.launch(PERMISSIONS_REQUIRED)
        }

        rv = binding.verseList
        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = ChallengeDetailRva(groupVm, this)
        rva = rv.adapter as ChallengeDetailRva

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = Navigation.findNavController(view)
//        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val appBarConfiguration = AppBarConfiguration.Builder(R.id.group_fm).build()
        NavigationUI.setupWithNavController( binding.toolbar, navController, appBarConfiguration)

        //영상 저장 버튼 활성화 조건 걸기 - 체크 클릭하여 체크될때마다 검사하여 리스트 사이즈만큼 모든 요소들이 체크됐다면 저장버튼 활성시킴
        groupVm.liveChalDetailVerseL.observe(viewLifecycleOwner, Observer {
            rva.notifyDataSetChanged()
            var count = 0;
            it.forEach { it2 ->
                if(it2.asJsonObject.get("is_checked").asString == "1"){
                    count++
                }
            }
            if(count == it.size()){
                binding.saveBt.visibility = View.VISIBLE
                Toast.makeText(requireActivity(),"인증을 완료하기위해 보내기를 해주세요.",Toast.LENGTH_SHORT).show()
            } else {
                binding.saveBt.visibility = View.INVISIBLE
            }
        })
        
        //인증하기 버튼(녹화) 클릭시 녹화 시작 -- 현재 사용안함
       /* binding.videoCaptureButton.setOnClickListener{
            if(binding.videoCaptureButton.text == "인증시작"){
                recordVideo()
                binding.videoCaptureButton.text = "진행중.."
            } else {
                //인증하기가 아닐 경우 레코딩 중지하고 인증하기로 바꿈
//                videoCapture.stopRecording()
//                binding.videoCaptureButton.text = "인증시작"
            }
        }*/
        //상단바의 보내기 버튼 클릭시 녹화 중지 및 서버 전송 작업 시작
        binding.saveBt.setOnClickListener {
            binding.videoCaptureButton.text = "완료작업중.."
            videoCapture.stopRecording()
        }

//        binding.toolbarIv.setOnClickListener{
//            Navigation.findNavController(it).navigate(R.id.action_challengeDetailFm_to_challengeDetailAfterFm)
//        }

    }

    override fun onResume() {
        super.onResume()
        binding.toolbarTv.text = arguments?.get("verseScope").toString().substring(8)

        AlertDialog.Builder(requireActivity())
            .setTitle("인증녹화")
            .setMessage("확인을 누르면 녹화가 시작됩니다. 체크과정이 마무리되면 보내기버튼으로 인증을 완료합니다.")
            .setNeutralButton("취소") { dialogInterface, i ->
                //취소누르면 뒤로가기함
//                findNavController().popBackStack(R.id.challengeDetailListFm, false)
                findNavController().navigateUp()
            }
            .setPositiveButton("확인") { dialogInterface, i ->
                recordVideo()
                binding.videoCaptureButton.text = "진행중.."
            }
            .setCancelable(false)
            .create()
            .show()

        //상단바 프로필 이미지 클릭시
        binding.toolbarIv.setOnClickListener {
            findNavController().navigate(com.example.androidclient.R.id.action_global_myProfileFm)
        }
        //상단바 프로필 이미지 로딩
        ImageHelper.getImageUsingGlide(requireActivity(), MyApp.userInfo.user_image, binding.toolbarIv)


    }

    //한번 카메라 동작 관련 리스너를 등록하면 된다
    private fun initCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                startCameraX(cameraProvider)
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun startCameraX(cameraProvider: ProcessCameraProvider) {
        cameraProvider.unbindAll()
        //카메라 셀렉터 셋팅
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        //미리보기뷰 셋팅
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()
        preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)

        // Video capture use case
        //비디오 유스케이스 셋팅
        videoCapture = VideoCapture.Builder()
            .setTargetResolution( Size(640, 480))
//            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setVideoFrameRate(7)
            .build()

        //bind to lifecycle: 카메라 프로바이더에 현재 fm의 생명주기 적용 + 사용할 카메라등록 + 적용할 유스케이스 등록
        cameraProvider.bindToLifecycle(
            (this as LifecycleOwner),
            cameraSelector,
            preview,
//            imageCapture,
            videoCapture
        )
    }

    private fun recordVideo() {
        //보여줄 이름 정하기, 미디어 정보,타입설정
        val name = "challenge-" + SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.KOREAN)
            .format(System.currentTimeMillis()) + ".mp4"
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")

        //녹화 시작 - 파일로 저장할 위치 & 방법 정하기 , 녹화실행자(스레드) 지정,  녹화 성공 콜백 등록
        Toast.makeText(requireActivity(),"인증녹화를 시작합니다. 체크가 완료되면 인증보내기를 할 수 있습니다.",Toast.LENGTH_LONG).show()
        videoCapture.startRecording(
            VideoCapture.OutputFileOptions.Builder(
                requireActivity().contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build(),
            ContextCompat.getMainExecutor(requireActivity()),
            //보내기 버튼 클릭시 - VideoCapture.stopRecording 이 실행되고 녹화가 완료절차에 들어간다. 그후 저장되면 이 콜백이 실행된다.
            object : VideoCapture.OnVideoSavedCallback {
                override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                    //특정 생명주기동안 실행 - 생명주기중 detach 된후 이 콜백이 도착하면 NullPointerException이 뜨기에 위험하다.
                    // mbinding의 유무로 생명주기를 파악하기도 가능하고 코루틴스코프로 제약조건을 걸수도 있다.
//                        lifecycle.coroutineScope.launchWhenResumed {}
                    if(mbinding != null){

                        Toast.makeText(requireActivity(),"인증보내기 중입니다..",Toast.LENGTH_SHORT).show()
                        // E/ChallengeDetailFm: outputFileResults.savedUri: content://media/external/video/media/230
//                        Log.e(tagName, "outputFileResults.savedUri: ${outputFileResults.savedUri}")

                        val uploadO = mutableMapOf<String, RequestBody>()
                        uploadO["chal_detail_no"] = groupVm.chalDetailInfo.get("chal_detail_no").asString.toRequestBody("text/plain".toMediaTypeOrNull())
                        uploadO["chal_no"] = groupVm.chalDetailInfo.get("chal_no").asString.toRequestBody("text/plain".toMediaTypeOrNull())
                        uploadO["progress_day"] = groupVm.chalDetailInfo.get("progress_day").asString.toRequestBody("text/plain".toMediaTypeOrNull())
                        uploadO["user_no"] = MyApp.userInfo.user_no.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                        val fileHelper = FileHelper()
                        binding.progressBar.visibility = View.VISIBLE
                        CoroutineScope(Dispatchers.IO).launch {
                            groupVm.챌린지인증영상업로드(uploadO,
                                fileHelper.getPartBodyFromUriForVideo(requireActivity(), outputFileResults.savedUri!!, "chal_video"),
                                true
                            )
                            groupVm.챌린지상세목록가져오기(groupVm.chalDetailInfo.get("chal_no").asInt, true)
//                            val handler = Handler(Looper.getMainLooper())
//                            handler.post {
//                                binding.progressBar.visibility = View.GONE
//                            }
                        }
                        //밑의 코드가 위의 코루틴보다 먼저 완료된다 - 업로드가 오래걸리고, 메인스레드를 블락하지 않기 때문
                        //업로드가 완료 되면 상세목록을 가져오고 목록의 리사이클러뷰가 갱신된다. 업로드 완료 여부를 확인할 수 있다.
                        CoroutineScope(Dispatchers.Main).launch {
                            groupVm.챌린지인증영상업로드사전작업(groupVm.chalDetailInfo.get("chal_detail_no").asInt, true)
                            Toast.makeText(requireActivity(),"인증보내기를 완료했습니다",Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp() //완료후 이전페이지로 돌아감 - 메인스레드에서만 실행가능 아니면 오류남
                        }
                    }
                }
                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                    Toast.makeText(requireActivity(),"다시 시도해주세요. 오류 발생: $message",Toast.LENGTH_SHORT).show()
                    Log.e(tagName, "비디오캡처오류: $message")
                }
            }
        )
    }



    override fun onStop() {
        super.onStop()
    }

    override fun onDestroyView() {
        mbinding = null
        super.onDestroyView()
    }



//    companion object {
//        /** Convenience method used to check if all permissions required by this app are granted */
//    }
    fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    private val 권한요청 =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in PERMISSIONS_REQUIRED && it.value == false)//key가 배열안에 있고, 그 value가 false 라면 권한이 아직 없는것.
                    permissionGranted = false
            }
            if (!permissionGranted) { //권한 없을때 없다는 메시지 띄움
                Toast.makeText(context, "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }





}



//                      getChallengeDetailList
//                                  "result": [
//2022-07-30 17:16:26.711 D/OkHttp:         {
//    2022-07-30 17:16:26.711 D/OkHttp:             "chal_no": "12",
//    2022-07-30 17:16:26.711 D/OkHttp:             "progress_day": "1",
//    2022-07-30 17:16:26.711 D/OkHttp:             "is_checked": "0",
//    2022-07-30 17:16:26.711 D/OkHttp:             "start_date": "2022-07-30 16:57:08",
//    2022-07-30 17:16:26.711 D/OkHttp:             "progress_date": "2022-07-30 16:57:08",
//    2022-07-30 17:16:26.711 D/OkHttp:             "chal_detail_no": "10192",
//    2022-07-30 17:16:26.711 D/OkHttp:             "first_verse": [
//    2022-07-30 17:16:26.711 D/OkHttp:                 {
//        2022-07-30 17:16:26.711 D/OkHttp:                     "chal_detail_verse_no": "1407",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "chal_no": "12",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "bible_no": "6511",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "is_checked": "0",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "progress_day": "1",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "book": "7",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "chapter": "1",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "verse": "1",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "content": "여호수아가 죽은 후에 이스라엘 자손이 여호와께 묻자와 가로되 우리 중 누가 먼저 올라가서 가나안 사람과 싸우리이까",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "book_no": "7",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "book_name": "사사기",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "book_category": "구약"
//        2022-07-30 17:16:26.711 D/OkHttp:                 }
//    2022-07-30 17:16:26.711 D/OkHttp:             ],
//    2022-07-30 17:16:26.711 D/OkHttp:             "last_verse": [
//    2022-07-30 17:16:26.711 D/OkHttp:                 {
//        2022-07-30 17:16:26.711 D/OkHttp:                     "chal_detail_verse_no": "1420",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "chal_no": "12",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "bible_no": "6524",
//        2022-07-30 17:16:26.712 D/OkHttp:                     "is_checked": "0",
//        2022-07-30 17:16:26.712 D/OkHttp:                     "progress_day": "1",
//        2022-07-30 17:16:26.712 D/OkHttp:                     "book": "7",
//        2022-07-30 17:16:26.712 D/OkHttp:                     "chapter": "1",
//        2022-07-30 17:16:26.712 D/OkHttp:                     "verse": "14",
//        2022-07-30 17:16:26.712 D/OkHttp:                     "content": "악사가 출가할 때에 그에게 청하여 자기 아비에게 밭을 구하자 하고 나귀에서 내리매 갈렙이 묻되 네가 무엇을 원하느냐",
//        2022-07-30 17:16:26.712 D/OkHttp:                     "book_no": "7",
//        2022-07-30 17:16:26.712 D/OkHttp:                     "book_name": "사사기",
//        2022-07-30 17:16:26.712 D/OkHttp:                     "book_category": "구약"
//        2022-07-30 17:16:26.712 D/OkHttp:                 }
//    2022-07-30 17:16:26.712 D/OkHttp:             ],
//    2022-07-30 17:16:26.712 D/OkHttp:             "verse_count": 14,
//    2022-07-30 17:16:26.712 D/OkHttp:             "progress_percent": 0
//    2022-07-30 17:16:26.712 D/OkHttp:         },