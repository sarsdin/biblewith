package com.example.androidclient.group

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.activity.result.registerForActivityResult
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.androidclient.MyApp
import com.example.androidclient.R
import com.example.androidclient.databinding.GroupInWriteFmBinding
import com.example.androidclient.home.MainActivity
import com.example.androidclient.moreinfo.MyNoteRva
import com.example.androidclient.util.FileHelper
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.imageview.ShapeableImageView
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import java.util.concurrent.ExecutionException

class GroupInWriteFm : Fragment() {
    lateinit var groupVm: GroupVm
    var mbinding: GroupInWriteFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    //리사이클러뷰 관련
    lateinit var rva: GroupInWriteRva
    lateinit var rv: RecyclerView

    //랜덤이미지 api
    val url = "https://random.responsiveimages.io/v1/docs"
    val url2 = "https://picsum.photos/200/300"
    var groupWriteImageUriL : List<Uri>? = null  //모임 글쓰기 이미지 추가용 리스트

    lateinit var startForImageResult : ActivityResultLauncher<Intent>

    //이미지+ 버튼 클릭시 - 포토앱에서 선택한 이미지 uri 목록을 받아오는 intent result 의 콜백을 받는 리스너 등록
    val getImageContent = registerForActivityResult(ActivityResultContracts.GetMultipleContents(), "image/*"){
        if(it.size == 0){
            Toast.makeText(requireActivity(), "선택을 취소했습니다.", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        it?.let {
//            Toast.makeText(requireActivity(), "uri get! ", Toast.LENGTH_SHORT).show()
//            Log.e("getImageContent", "$it")
            groupWriteImageUriL = it
            groupVm.groupWriteImageUriL = it //이미지 리사이클러뷰 어뎁터에 쓰임
            binding.groupInWriteImageList.visibility = View.VISIBLE //리사이클러 뷰 보이게 처리
//            binding.groupInWriteToolbarAddImageBt.text = "이미지 ${groupVm.groupWriteImageUriL?.size?:"+" }"
            binding.groupInWriteToolbarAddImageBt.text = "이미지 ${ if (groupVm.groupWriteImageUriL?.size==0) "+" else groupVm.groupWriteImageUriL?.size }"
            rva.notifyDataSetChanged()
        }
    }
    //뷰홀더에서 이미지추가 이미지 클릭시
    val addImageContent = registerForActivityResult(ActivityResultContracts.GetMultipleContents(), "image/*"){
        it?.let {
//            Toast.makeText(requireActivity(), "uri get! ", Toast.LENGTH_SHORT).show()
//            Log.e("getImageContent", "$it")
            //참조하는 주소가 같아서 2중 추가됨.. 주의 - getImageContent에서 받아온 it의 주소를 둘다 참조하고 있음.
            (groupVm.groupWriteImageUriL as MutableList<Uri>).addAll(it)//이미지 리사이클러뷰 어뎁터에 쓰임
//            (groupWriteImageUriL as MutableList<Uri>).addAll(it)
            binding.groupInWriteToolbarAddImageBt.text = "이미지 ${ if (groupVm.groupWriteImageUriL?.size==0) "+" else groupVm.groupWriteImageUriL?.size }"
            rva.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startForImageResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val resultCode = result.resultCode
            val data = result.data

            if (resultCode == Activity.RESULT_OK) {
                //Image Uri will not be null for RESULT_OK
                val fileUri = data?.data!!
//                groupWriteImageUri = fileUri
                //이미지뷰에 사진 넣기
//                (binding.groupInWriteIv as ShapeableImageView).setImageURI(fileUri)
                Log.e("startForImageResult", "$groupWriteImageUriL")
            } else if (resultCode == ImagePicker.RESULT_ERROR) {
                Toast.makeText(requireActivity(), ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireActivity(), "선택 취소", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mbinding = GroupInWriteFmBinding.inflate(inflater, container, false)
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
        checkPermission()

        binding.groupInWriteToolbar.setNavigationIcon(R.drawable.ic_xmark) // 버그로 인해 작동안하는중 구글에서 3년동안 안고침

        //이미지 리사이클러 뷰 셋팅
        rv = binding.groupInWriteImageList
        rv.layoutManager = LinearLayoutManager(binding.root.context).apply { orientation = LinearLayoutManager.HORIZONTAL }
        //        recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
        rv.adapter = GroupInWriteRva(groupVm, this)
        rva = rv.adapter as GroupInWriteRva


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //툴바 셋팅
        val navController = Navigation.findNavController(view)
        val appBarConfiguration = AppBarConfiguration.Builder(com.example.androidclient.R.id.group_fm).build()
        binding.groupInWriteToolbar.setupWithNavController(navController, appBarConfiguration)

        //툴바 이미지+ 버튼 클릭시
        binding.groupInWriteToolbarAddImageBt.setOnClickListener {
            Toast.makeText(requireActivity(),"사진을 선택해주세요.", Toast.LENGTH_SHORT).show()
            
            //contentProvider 로 선택 이미지 uri 를 가져오는 인텐트 명령 - 변수 groupWriteImageUri 에 담김
            getImageContent.launch()
            // 일반적인 startForImageResult.launch(intent) 를 이용하는 방법도 content:// 로 시작하는 document/의 uri를 가져옴. 실제 path는 알아내야함.
//            var intent = Intent(Intent.ACTION_PICK)
//            intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
//            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
//            intent.action = Intent.ACTION_GET_CONTENT
//            startForImageResult.launch(intent)


//            ImagePicker.with(this)
//                .galleryOnly()
//                .compress(1024)         //Final image size will be less than 1 MB(Optional)
//                .maxResultSize(1080, 1080)  //Final image resolution will be less than 1080 x 1080(Optional)
//                .createIntent { intent ->
//                    startForProfileImageResult.launch(intent)
//                }
            //갤러리앱에서 이미지가져오기 - 이미지픽커로 대체 - 사진 하나만 가능한듯. 멀티픽은 갤러리앱을 활용해야함
//            val intent = Intent(Intent.ACTION_PICK) //멀티픽은 따로 액션이 있는 듯
//            intent.type = "image/*"
//            intent.type = MediaStore.Images.Media.CONTENT_TYPE
//            startForProfileImageResult.launch(intent)

//            Glide.with(this) //해당 환경의 Context나 객체 입력
//                .load(url).toString() //URL, URI 등등 이미지를 받아올 경로
//                .diskCacheStrategy(DiskCacheStrategy.NONE) //disk cache 전략을 off
//                .skipMemoryCache(true) //memory cache 전략을 off. disk와 같이 꺼야 같은 url 에서 이미지를 반복적으로 로딩가능.캐시전략 on: url이 같으면 같은 이미지취급
//                .override(184,153) // override(width, height) : 받아온 이미지의 크기를 조절할 수 있는 함수
//                .centerCrop() // 외부에서 받아온 이미지가 있다면, 가운데에서 이미지를 잘라 보여주는 함수
//                .placeholder(R.drawable.ic_baseline_cloud_24) // 이미지가 로딩하는 동안 보여질 이미지를 정함
//                .error(R.drawable.ic_xmark) //이미지를 불러오는데 실패 했을때 보여질 이미지를 정함
//                .into(it as ShapeableImageView) //받아온 이미지를 받을 공간(ex. ImageView)

//            val newsT = Thread {
//                loadFile() //api로 랜덤 이미지 받아오기
//            }
//            newsT.start()

        }

        //완료 버튼 클릭시
        binding.groupInWriteToolbarAddBt.setOnClickListener {
            Toast.makeText(requireActivity(),"test bt", Toast.LENGTH_SHORT).show()
            //todo 서버로 보내는 로직 작성. groupImage - 모임 메인 이미지.   uploadO - 모임장, 모임명, 모임설명 등의 requestBody들을 가진 맵
            var writeImage = mutableListOf<MultipartBody.Part>()
            val uploadO = mutableMapOf<String, RequestBody>()
            uploadO["group_no"] = groupVm.currentGroupIn.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            uploadO["user_no"] = MyApp.userInfo.user_no.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            uploadO["gboard_content"] = binding.groupInWriteContentEt.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            val fileHelper = FileHelper()
            if(groupWriteImageUriL != null){
                //FileHelper 에서 uri를 이용해 MultipartBody.Part 객체를 생성해서 가져옴
                //formData를 보낼때 여러개의 파일이면 key이름뒤에 [] 배열표시를 꼭 붙여야 서버에서 정상 인식한다. 주의!
                writeImage = fileHelper.getPartBodyFromUriList(
                    requireContext(),
                    groupWriteImageUriL!!,
                    "gboard_image[]") as MutableList<MultipartBody.Part>

                //리졸버를 이용해 이미지픽커로 받은 사진 파일의 uri를 인풋스트림에 넣고, 또 그것을 bitmap factory를 이용해 bitmap을 생성함.
                //비트맵을 압축함. 그리고, 압축한 것을 바이트배열아웃스트림에 전달함. 그 후 multipart용 requestBody를 byteArrayOutS에서 얻어온 파일을 포함시켜 생성.
                //MultipartBody.Part로 다시 requestBody를 감싸서 레트로핏 인터페이스에 form-data로써 전달함.
                // 압축안할려면 그냥 File(uri.path)를 만들어 requestBody에 전달하면 끝 -- 다만 uri.path 의 절대경로를 만들어내는 코드를 따로 써야함..(FileHelper.class에 만듦)
                /*val bitmap = BitmapFactory.decodeStream(requireActivity().contentResolver.openInputStream(groupMainImageUri!!))
                val byteArrayOutS = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutS)
                val requestBody = byteArrayOutS.toByteArray().run {
                    toRequestBody("image/jpeg".toMediaTypeOrNull(),0, size )
                }
                val groupImage = MultipartBody.Part.createFormData("group_main_image", file.name, requestBody)*/

            } else {
                //이미지를 등록안하고 모임을 만들경우 noimage vector 파일을 bitmap 파일로 변환하여 서버로 전송.
//                val resUri = Uri.parse("android.resource://${requireContext().packageName}/${R.drawable.ic_noimage}")
//                groupImage = fileHelper.getPartBodyFromDrawableResource(requireContext(), R.drawable.ic_noimage, "group_main_image", "svg")
//                (binding.groupInWriteIv as ShapeableImageView).setImageURI(resUri)
            }

            //서버로 만들 모임 정보 전송
            groupVm.모임글쓰기(uploadO, writeImage,false)?.enqueue(object : Callback<JsonObject?> {
                override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                    if (response.isSuccessful) {
                        val res = response.body()!!
                        if(res.get("msg").asString == "ok" ){
                            Log.e("[GroupInWriteFm]", "모임글쓰기 onResponse: $res")
                            Toast.makeText(requireActivity(),"글을 등록하였습니다.", Toast.LENGTH_SHORT).show()
                            //글쓰기 성공하면 모임 게시물 목록을 갱신하기 위해 다시 서버로부터 가져와야한다.
                            //Ui 갱신은 GroupInFm observer 에서 갱신되는 데이터 목록들을 보고 갱신한다.
                            groupVm.모임상세불러오기(true)
                            Navigation.findNavController(view).navigateUp() //만들기 성공 후 이전페이지로 돌아가기
                        }
                    }
                }
                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    Log.e("[GroupInWriteFm]", "모임글쓰기 onFailure: " + t.message)
                }
            })
        }



    }



    fun loadFile() {
        try {
            //글라이드를 통해 파일 다운로드
            val requestManager = Glide.with(this)
            val loaded = requestManager.asBitmap().load(url)
                .diskCacheStrategy(DiskCacheStrategy.NONE) //disk cache 전략을 off
                .skipMemoryCache(true)
            //받은 비트맵을 외부스토리지에 저장.
            loaded.into(object: CustomTarget<Bitmap>(){
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    saveImage(resource )
                }
                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })
//            val file: File = requestManager.downloadOnly().load(url).submit().get()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    //글라이드로 받은 bitmap 이미지 압축하여 파일로 외부스토리지에 저장
    private fun saveImage(image: Bitmap) {
        var storageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path) //디렉토리 만들기용 먼저 선언
        if (!storageDir.exists()) {
            storageDir.mkdir()
        }
        if (storageDir.exists()) {
            val imageFile = File(storageDir, "${System.currentTimeMillis()}.jpeg") //저장될 파일 객체 만들기 (앞은 디렉토리위치, 뒤는 파일명)
//            requireContext().cacheDir.path
//            val savedImagePath = imageFile.absolutePath
            try {
                val fOut: OutputStream = FileOutputStream(imageFile)
                image.compress(Bitmap.CompressFormat.JPEG, 80, fOut) //글라이드로 받아온 비트맵 파일을 jpeg로 압축하고 파일을 실제로 아웃풋함
                fOut.close()
                scanMedia(imageFile) //미디어db갱신
                Toast.makeText(requireActivity(), "이미지 저장완료", Toast.LENGTH_SHORT).show()
            } catch (e: java.lang.Exception) {
                Toast.makeText(requireActivity(), "저장 중 오류발생", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } else {
            Toast.makeText(requireActivity(), "폴더 만들기 실패!", Toast.LENGTH_SHORT).show()
        }
    }

    fun scanMedia(file: File?) {
        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        intent.data = Uri.fromFile(file)
        (requireActivity() as MainActivity).sendBroadcast(intent)
    }

    private fun checkPermission() {
        // context의 기능을 이용하여 permission이 있는지 확인함. 해당 권한이 없으면 PackageManager.PERMISSION_DENIED를 반환함.
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            // 처음 호출시엔 if()안의 부분은 false로 리턴 됨 -> else{..}의 권한요청으로 넘어감. 왜냐하면, 처음에는 권한이 없기때문.

            // 권한 유틸 클래스인 ActivityCompat의 기능을 이용해서 '권한' 요청전에 근거있는 대화창등의 UI를 표시해야하는지 물어본다.
            // 예를 들어, 앱정보에서 권한탭의 권한들을 수동or확인에서 허용안함했을 경우 해당항목에 대한 권한유무를 체크할 수 있다.
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.CAMERA) ) {

                AlertDialog.Builder(requireActivity())
                    .setTitle("알림")
                    .setMessage("저장소 권한이 거부되었습니다. 사용을 원하시면 설정에서 해당 권한을 직접 허용하셔야 합니다.")
                    .setNeutralButton("설정") { dialogInterface, i ->
                        //인텐트가 해야할 액션(명령)을 정의함. 여기서는 전역에 노출될 내앱의 패키지명을 입력한다는 명령임.
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.parse("package:" + requireActivity().packageName)
                        startActivity(intent)
                    }
                    .setPositiveButton(
                        "확인"
                    ) { dialogInterface, i -> requireActivity().finish() }
                    .setCancelable(false)
                    .create()
                    .show()

            } else { // 권한을 체크했을 때 해당권한이 없다면 권한을 요청한다. 필요한 권한들과 requestcode를 설정한다.
                ActivityCompat.requestPermissions(requireActivity(),
                    //외부저장소, 카메라 권한 요청
                    //onRequestPermissionsResult(int, String[], int[])의 int에 포함될 requestCode임.
                    // 내가 지정한 이 숫자가 onRequestPermissionsResult콜백메소드(int requestCode)에 들어가고
                    // 권한결과 여부가 GRANTED(0) 인지 DENIED(-1) 인지로 반환됨.
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA), 1111 )
            }

        }
    }


    fun saveFile(file: File?) {
        var localFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path) //디렉토리 만들기용 먼저 선언
        if (!localFile.exists()) { // 디렉토리가 있는지 없는지 부터 체크
            localFile.mkdirs() //없으면 생성
        }
        val filepath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path + "/"+System.currentTimeMillis() + ".jpeg"
        localFile = File(filepath) // 만들고자 하는 파일패스 + 네임 객체 할당
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(requireActivity(),"filepath: $localFile", Toast.LENGTH_SHORT).show()
        }
        Log.e("[GroupInWriteFm saveFile]", "filepath: $localFile")
        try {
            val inputs: InputStream = FileInputStream(file)
            Log.d("[GroupInWriteFm saveFile]", "on do in background, url get input stream")
            val bis = BufferedInputStream(inputs)
            Log.d("[GroupInWriteFm saveFile]", "on do in background, create buffered input stream")
            val baos = ByteArrayOutputStream()
            Log.d("[GroupInWriteFm saveFile]", "on do in background, create buffered array output stream")
            val img = ByteArray(1024)
            var current = 0
            Log.d("[GroupInWriteFm saveFile]", "on do in background, write byte to baos")
            while (bis.read().also { current = it } != -1) {
                baos.write(current)
            }
            Log.d("[GroupInWriteFm saveFile]", "on do in background, done write")
            val fos = FileOutputStream(localFile)  //파일출력스트림을 이용해 저장될 경로를 가진 빈 파일 객체에 쓸 준비를 마침
            Log.d("[GroupInWriteFm saveFile]", "on do in background, create fos")
            fos.write(baos.toByteArray())       //메모리상에 있는 글라이드로 받아온 파일객체를 바이트배열로 변환한 것을 이용해 실제 파일에 쓰기(아웃풋).
            Log.d("[GroupInWriteFm saveFile]", "on do in background, write to fos")
            fos.flush() // 받아온 이미지 파일객체를 최종byteArray로 만들어, 사진폴더에 지정해준 파일객체(localFile)를 이용해 fos로 실제파일로 아웃풋작업(byteArray -> localFile)을 함.
            fos.close()
            inputs.close()
            Log.d("[GroupInWriteFm saveFile]", "on do in background, done write to fos")
            scanMedia(localFile) //미디어 스캔을 해줘야 , 시스템에서 바로 반영됨
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }
}
