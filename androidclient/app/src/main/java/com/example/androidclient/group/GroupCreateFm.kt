package com.example.androidclient.group

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.Navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.androidclient.MyApp
import com.example.androidclient.R
import com.example.androidclient.databinding.GroupCreateFmBinding
import com.example.androidclient.home.MainActivity
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
import java.util.*
import java.util.concurrent.ExecutionException


class GroupCreateFm : Fragment() {
    lateinit var groupVm: GroupVm
    //    lateinit var rva: GroupRva
    lateinit var rv: RecyclerView
    var mbinding: GroupCreateFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    //랜덤이미지 api
    val url = "https://random.responsiveimages.io/v1/docs"
    val url2 = "https://picsum.photos/200/300"
    var groupMainImageUri : Uri? = null  //그룹 메인 이미지픽커로 선택한 파일의 Uri

    lateinit var startForProfileImageResult : ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startForProfileImageResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                val resultCode = result.resultCode
                val data = result.data

                if (resultCode == Activity.RESULT_OK) {
                    //Image Uri will not be null for RESULT_OK
                    val fileUri = data?.data!!
                    groupMainImageUri = fileUri
                    //이미지뷰에 사진 넣기
                    (binding.groupCreateIv as ShapeableImageView).setImageURI(fileUri)
                } else if (resultCode == ImagePicker.RESULT_ERROR) {
                    Toast.makeText(requireActivity(), ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireActivity(), "선택 취소", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mbinding = GroupCreateFmBinding.inflate(inflater, container, false)
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
        checkPermission()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //툴바 셋팅
        val navController = Navigation.findNavController(view)
        val appBarConfiguration = AppBarConfiguration.Builder(com.example.androidclient.R.id.group_fm).build()
        binding.groupCreateToolbar.setupWithNavController(navController, appBarConfiguration)

        //이미지뷰 클릭시
        binding.groupCreateIv.setOnClickListener {
            Toast.makeText(requireActivity(),"사진을 선택해주세요",Toast.LENGTH_SHORT).show()

            ImagePicker.with(this)
                .galleryOnly()
                .compress(1024)         //Final image size will be less than 1 MB(Optional)
                .maxResultSize(1080, 1080)  //Final image resolution will be less than 1080 x 1080(Optional)
                .createIntent { intent ->
                    startForProfileImageResult.launch(intent)
                }
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
        binding.groupCreateToolbarAddBt.setOnClickListener {
            Toast.makeText(requireActivity(),"test bt",Toast.LENGTH_SHORT).show()
            //todo 서버로 보내는 로직 작성. groupImage - 모임 메인 이미지.   uploadO - 모임장, 모임명, 모임설명 등의 requestBody들을 가진 맵
            lateinit var groupImage :MultipartBody.Part
            val uploadO = mutableMapOf<String, RequestBody>()
            uploadO["user_no"] = MyApp.userInfo.user_no.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            uploadO["group_name"] =  binding.groupCreateGroupNameEt.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            uploadO["group_desc"] = binding.groupCreateGroupDescEt.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            val fileHelper = FileHelper()
            if(groupMainImageUri != null){
                //FileHelper 에서 uri를 이용해 MultipartBody.Part 객체를 생성해서 가져옴
                groupImage = fileHelper.getPartBodyFromUri(requireContext(), groupMainImageUri!!, "group_main_image")

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
                val resUri = Uri.parse("android.resource://${requireContext().packageName}/${R.drawable.ic_noimage}")
                groupImage = fileHelper.getPartBodyFromDrawableResource(requireContext(), R.drawable.ic_noimage, "group_main_image", "svg")
                (binding.groupCreateIv as ShapeableImageView).setImageURI(resUri)
            }
            
            //서버로 만들 모임 정보 전송
            groupVm.모임만들기(uploadO, groupImage,false)?.enqueue(object : Callback<JsonObject?>{
                override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                    if (response.isSuccessful) {
                        val res = response.body()!!
                        if(res.get("msg").asString == "ok" ){
                            Log.e("[GroupCreateFm]", "모임만들기 onResponse: $res")
                            Toast.makeText(requireActivity(),"모임을 만들었습니다.",Toast.LENGTH_SHORT).show()
                            findNavController(it).navigateUp() //만들기 성공 후 이전페이지로 돌아가기
                        }
                    }
                }
                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    Log.e("[GroupCreateFm]", "모임만들기 onFailure: " + t.message)
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
        var storageDir = File(Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES).path) //디렉토리 만들기용 먼저 선언
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
        var localFile = File(Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES).path) //디렉토리 만들기용 먼저 선언
        if (!localFile.exists()) { // 디렉토리가 있는지 없는지 부터 체크
            localFile.mkdirs() //없으면 생성
        }
        val filepath = Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES).path + "/"+System.currentTimeMillis() + ".jpeg"
        localFile = File(filepath) // 만들고자 하는 파일패스 + 네임 객체 할당
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(requireActivity(),"filepath: $localFile",Toast.LENGTH_SHORT).show()
        }
        Log.e("[GroupCreateFm saveFile]", "filepath: $localFile")
        try {
            val inputs: InputStream = FileInputStream(file)
            Log.d("[GroupCreateFm saveFile]", "on do in background, url get input stream")
            val bis = BufferedInputStream(inputs)
            Log.d("[GroupCreateFm saveFile]", "on do in background, create buffered input stream")
            val baos = ByteArrayOutputStream()
            Log.d("[GroupCreateFm saveFile]", "on do in background, create buffered array output stream")
            val img = ByteArray(1024)
            var current = 0
            Log.d("[GroupCreateFm saveFile]", "on do in background, write byte to baos")
            while (bis.read().also { current = it } != -1) {
                baos.write(current)
            }
            Log.d("[GroupCreateFm saveFile]", "on do in background, done write")
            val fos = FileOutputStream(localFile)  //파일출력스트림을 이용해 저장될 경로를 가진 빈 파일 객체에 쓸 준비를 마침
            Log.d("[GroupCreateFm saveFile]", "on do in background, create fos")
            fos.write(baos.toByteArray())       //메모리상에 있는 글라이드로 받아온 파일객체를 바이트배열로 변환한 것을 이용해 실제 파일에 쓰기(아웃풋).
            Log.d("[GroupCreateFm saveFile]", "on do in background, write to fos")
            fos.flush() // 받아온 이미지 파일객체를 최종byteArray로 만들어, 사진폴더에 지정해준 파일객체(localFile)를 이용해 fos로 실제파일로 아웃풋작업(byteArray -> localFile)을 함.
            fos.close()
            inputs.close()
            Log.d("[GroupCreateFm saveFile]", "on do in background, done write to fos")
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










//Log.e("[GroupCreateFm]", "모임만들기 file uri2: ${groupMainImageUri!!.run{
//    val projection = arrayOf(
//        MediaStore.Images.Media._ID,
//        MediaStore.Images.Media.DISPLAY_NAME,
//        MediaStore.Images.Media.DATE_TAKEN
//    )
//    val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
////                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
//    val cursor = requireActivity().contentResolver.query(this, projection , null, null, sortOrder)?.use {cursor ->
//        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
//        val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
//        val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
//
//        cursor.moveToNext()
//        val id = cursor.getLong(idColumn)
//        val dateTaken = Date(cursor.getLong(dateTakenColumn))
//        val displayName = cursor.getString(displayNameColumn)
//        val contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
////                                return@run "$id, $dateTaken, $displayName, $contentUri"
//        return@run "$contentUri"
////                                    while (cursor.moveToNext()) {
////                                        imageList += MediaItem(id, displayName, dateTaken, contentUri)
////                                    }
//    }
//} }")






//val file = File("${groupMainImageUri!!.run{
//    var realPath = String()
//    val databaseUri: Uri
//    val selection: String?
//    val selectionArgs: Array<String>?
//    if (path!!.contains("/document/image:")) { // files selected from "Documents"
//        databaseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
//        selection = "_id=?"
//        selectionArgs = arrayOf(DocumentsContract.getDocumentId(this).split(":")[1])
//    } else { // files selected from all other sources, especially on Samsung devices
//        databaseUri = this
//        selection = null
//        selectionArgs = null
//    }
//    try {
//        val column = "_data"
//        val projection = arrayOf(column)
//        val cursor = requireContext().contentResolver.query(
//            databaseUri,
//            projection,
//            selection,
//            selectionArgs,
//            null
//        )
//        cursor?.let { it2->
//            if (it2.moveToFirst()) {
//                val columnIndex = cursor.getColumnIndexOrThrow(column)
//                realPath = cursor.getString(columnIndex)
//                Log.e("[GroupCreateFm]", "모임만들기 file realPath: $realPath")
//                return@run realPath
//            }
//            cursor.close()
//        }
//    } catch (e: Exception) {
//        println(e)
//    }
//} }")





//val requestBody = file.run {
//    asRequestBody("image/*".toMediaTypeOrNull())
//}
//val groupImage = MultipartBody.Part.createFormData("group_main_image", "", requestBody)
//Log.e("[GroupCreateFm]", "모임만들기 file uri: ${groupMainImageUri!!.path!!}")       //절대경로 or /document/image:153 등의 추상화 경로
//Log.e("[GroupCreateFm]", "모임만들기 file uri2: ${MediaStore.Images.Media.EXTERNAL_CONTENT_URI}/${groupMainImageUri!!.lastPathSegment}") //content::으로 시작하는 uri
//Log.e("[GroupCreateFm]", "모임만들기 file uri2: ${groupMainImageUri!!.lastPathSegment}") //파일의 실제이름. /로구분되는 마지막경로번째의 이름이니깐 파일이면 파일이름이 나옴.
//Log.e("[GroupCreateFm]", "모임만들기 file uri2: ${groupMainImageUri!!.scheme}")      //file 이면 file 이라고 나옴
//
//
//Log.e("[GroupCreateFm]", "모임만들기 file uri2: ${groupMainImageUri!!.run{
//    val cursor = requireActivity().contentResolver.openFileDescriptor(this, "r").use { it2 ->
//        val bitmap = BitmapFactory.decodeFileDescriptor(it2?.fileDescriptor)
//    }
//    return@run cursor?: ""
//} }")



//////////////////////////////////////////////////////////////////////////참고 글라이드로 받은 이미지로부터 uri 추출하기
//var addNewPostImage: Imageview? = findviewbyid(R.id.addNewPostImage)
//var bitmap: Bitmap? = null
//var addNewPostWebLink: EditText = findviewbyid(R.id.addNewPostWebLink)
//
//private fun getUri() {
//    val imageUrl = addNewPostWebLink.text.toString().trim { it <= ' ' }
//    Glide.with(ApplicationProvider.getApplicationContext<FragmentActivity>())
//        .asBitmap()
//        .load(imageUrl) //.fitCenter()
//        .into(object : CustomTarget<Bitmap?>() {
//            fun onResourceReady(resource: Bitmap, @Nullable transition: Transition<in Bitmap>?) {
//                // you can do something with loaded bitmap here
//                //bitmap = resource;
//                addNewPostImage.setImageBitmap(resource)
//            }
//
//            override fun onLoadCleared(@Nullable placeholder: Drawable?) {}
//        })
//    postImageUri = getImageUri(this@NewPostActivity)
//    Toast.makeText(this, Objects.requireNonNull(postImageUri).toString(), Toast.LENGTH_SHORT).show()
//}

//private fun getImageUri(newPostActivity: NewPostActivity): Uri? {
//    bitmap = (addNewPostImage.getDrawable() as BitmapDrawable).bitmap
//    val bytes = ByteArrayOutputStream()
//    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
//    val path: String = MediaStore.Images.Media.insertImage(
//        newPostActivity.getContentResolver(),
//        bitmap,
//        UUID.randomUUID().toString() + ".png",
//        "drawing"
//    )
//    return Uri.parse(path)
//}
////////////////////////////////////////////////////////////////////////////////////////////////////////
