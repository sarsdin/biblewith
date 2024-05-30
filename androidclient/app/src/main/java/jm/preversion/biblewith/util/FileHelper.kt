package jm.preversion.biblewith.util

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import jm.preversion.biblewith.MainActivity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.RuntimeException
import java.util.concurrent.ExecutionException
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine


/**
 * 각종 안드로이드의 uri형식에 해당하는 File의 절대경로를 알아내어 해당파일을 만들고
 그에 해당하는 레트로핏 requestBody를 포함하는 MultipartBody.Part 를 반환한다.
 */
class FileHelper {
    val mediaType = "multipart/form-data".toMediaTypeOrNull()
    val UriUtil = UtilsFile()  //각종 안드로이드 Uri 를 분석하여 file_paths 가져오기

    fun getPartBodyFromUri(context: Context, uri: Uri, formDataKeyName: String): MultipartBody.Part {
//        val realPath = getPathFromURI(context, uri) //일단 기존에 쓰던것인데, DownloadsProvider 로 받은 파일은 인식안됨 따로 작업해야함.
//        val realPath = UriHelper.getPath(context, uri) //-- 안됨...ㅠㅠ
        val realPath = UriUtil.getFullPathFromContentUri(context, uri) //됨
        val fileImage = createFile(realPath)
        val requestBody = createRequestBody(fileImage)
        Log.e("[FileHelper]", "uri: $uri")
        Log.e("[FileHelper]", "realPath: $realPath")
        return createPart(fileImage, requestBody, formDataKeyName)
    }

    fun getPartBodyFromUriForVideo(context: Context, uri: Uri, formDataKeyName: String): MultipartBody.Part {
        val realPath = getAbsolutePathFromUri(context, uri)
        val fileImage = createFile(realPath!!)
        val requestBody = createRequestBody(fileImage)
        Log.e("[FileHelper]", "uri: $uri")
        Log.e("[FileHelper]", "realPath: $realPath")
        return createPart(fileImage, requestBody, formDataKeyName)
    }

    //context : activity or fragment , drawableResource : 안드로이드 이미지 리소스, formDataName : 서버로 보낼 key 이름, svgOrBmp: 변환될 원본 이미지의 포맷
    fun getPartBodyFromDrawableResource(context: Context, drawableResource: Int, formDataKeyName: String, svgOrBmp: String): MultipartBody.Part {
        val resUri = Uri.parse("android.resource://${context.packageName}/${drawableResource}")
        val byteArrayOutS = ByteArrayOutputStream()
        //svg 형식일 경우 백터를 bitmap 으로 바꾸는 내장 메소드를 실행하여 bitmap을 얻어옴. 아닌 경우 decodeResource 로 해석하여 가져옴
        val bitmap :Bitmap = if(svgOrBmp == "svg"){
            getDrawable(context, drawableResource)!!.toBitmap(200, 200)
        }else{
            BitmapFactory.decodeResource(context.resources, drawableResource)
        }
        //얻은 비트맵 파일를 압축하고 byteArrayOutS 으로 보냄
        bitmap.compress(Bitmap.CompressFormat.PNG, 30, byteArrayOutS)
        //byteArrayOutS 에서 압축된 파일을 얻고 그 파일을 requestBody 객체에 담는다.
        val requestBody = byteArrayOutS.toByteArray().run {
            toRequestBody("image/png".toMediaTypeOrNull(),0, size )
        }
        Log.e("[FileHelper]", "resUri: $resUri")
        //다시 그 requestBody 객체를 서버전송을 위해 MultipartBody.Part 객체에 담는다.
        return MultipartBody.Part.createFormData(formDataKeyName, resUri.lastPathSegment, requestBody)
    }

    //contentProvider 에서 받은 다중 uri 이미지를 요청용 멀티파트 리스트로 만들기. 로컬 uri 파일 전용!
    fun getPartBodyFromUriList(context: Context, uriL: List<Uri>?, formDataKeyName: String): List<MultipartBody.Part> {
        val res = mutableListOf<MultipartBody.Part>()
        val realPathList = mutableListOf<String>()
        //각 Uri 에 담긴 path 를 이용해 실제 경로를 얻고, 그경로의 파일객체를 만들고, 요청용 바디에 담아서 멀티파트 객체로 랩핑한다. 그 각 객체는 리스트에 반복적으로 추가됨.
        uriL?.forEach {
//            val realPath = getPathFromURI(context, it)
            val realPath = UriUtil.getFullPathFromContentUri(context, it)
            realPathList.add(realPath)
            val fileImage = createFile(realPath)
            val requestBody = createRequestBody(fileImage)
            res.add(createPart(fileImage, requestBody, formDataKeyName))
        }
        Log.e("[FileHelper]", "realPathList: $realPathList")
        Log.e("[FileHelper]", "mutableListOf: $res")
        return res
    }

    //모임의 글수정 화면에서 사용한다. 글수정 완료 버튼을 클릭시 - 업데이트할 자료들(이미지,글관련 데이터들)를
     suspend fun 업데이트용멀티파트리스트만들기(context: Context, uriL: List<Uri>?, formDataKeyName: String): List<MultipartBody.Part> {
//        CoroutineScope(Dispatchers.Main).launch {
        val res = mutableListOf<MultipartBody.Part>() //실질적으로 리턴할 멀티파트 리스트
        val realPathList = mutableListOf<String>() //디버깅용으로써 제대로 절대경로를 얻었는지 확인하기 위한 용도임. 다른 용도없음.
        //각 Uri 에 담긴 path 를 이용해 실제 경로를 얻고, 그경로의 파일객체를 만들고, 요청용 바디에 담아서 멀티파트 객체로 랩핑한다. 그 각 객체는 리스트에 반복적으로 추가됨.
            Log.e("[FileHelper]", "list: $uriL")
            uriL?.forEachIndexed { index, it ->
                //분기점: 인터넷을 통해 받아온 이미지(http://시작 Uri) or 로컬에 존재하는 파일(content://, document.image: Uri) << 이 두가지 Uri 형식이 존재
                //각 분기에 따라 수행할 메소드 명령들이 다름
                if(it.toString().contains("http://")){ //it.path 는 http://주소 를 제외한 uri의 경로만 리턴하니 주의! 삽질했음..

                    //코루틴을 비동기 콜백안의 resumeWith 실행전까지 잠시 멈춘다.
                    // - 콜백메소드안의 코드가 모두 실행되고 resume 하는것! 반복문의 비동기 결과들을 순차적으로 만들기 위함
                    val resp = suspendCoroutine { cont: Continuation<Bitmap> ->

                        //글라이드를 통해 파일 다운로드
                        Log.e("[FileHelper]", "path: ${it.path}")
                        val requestManager = Glide.with(context)
                        val loaded =  requestManager.asBitmap().load(it)
                            .diskCacheStrategy(DiskCacheStrategy.NONE) //disk cache 전략을 off
                            .skipMemoryCache(true)
                            //받은 비트맵을 requestBody 객체로 만듦
                            .into(object: CustomTarget<Bitmap>(){
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                val requestBody = bitmapToRequestBody(resource)
                                //위의 바디를 멀티파트로 만들고, 리턴할 멀티파트 리스트에 추가
                                res.add(
                                    requestBodyToAddMultipart(
                                        formDataKeyName,
                                "$index ${System.currentTimeMillis().toString()}",
                                        requestBody )
                                )
                                Log.e("[FileHelper]", "$index")

                                //resp 에 결과인 resource 가 담기고 코루틴 재개.
                                cont.resumeWith(Result.success(resource))
                            }
                            override fun onLoadCleared(placeholder: Drawable?) {
                            }
                        })
                    }

                // http:// 을 포함하지 않는 Uri는 content:// 이나 document.image: 등으로 시작되는 형식(안드로이드 로컬파일)이므로
                // 아래의 메소드를 이용해 로컬의 파일 절대경로를 얻고 그 파일을 멀티파트로 만들고 멀티파트 리스트에 담는다.
                } else {
                    val realPath = getPathFromURI(context, it)
                    realPathList.add(realPath) //디버깅용으로써 제대로 절대경로를 얻었는지 확인하기 위한 용도임. 다른 용도없음.
                    val fileImage = createFile(realPath)
                    val requestBody = createRequestBody(fileImage)
                    res.add(createPart(fileImage, requestBody, formDataKeyName))
                                    Log.e("오류태그", "else $index")
                }
            }

        Log.e("[FileHelper]", "realPathList: $realPathList")
        Log.e("[FileHelper]", "mutableListOf: $res")
//        }
        return res
    }

    //requestBody 객체를 받아 MultipartBody.Part 로 추가하는 메소드
    fun requestBodyToAddMultipart(formDataKeyName: String, fileName: String, requestBody: RequestBody):  MultipartBody.Part {
        val multipartBody = MultipartBody.Part.createFormData(formDataKeyName, fileName, requestBody)
        return multipartBody
    }

    //glide 등에서 받아온 bitmap 파일을 requestBody 객체로 만드는 메소드
    fun bitmapToRequestBody(glideBitmap: Bitmap): RequestBody {
        val bitmap = glideBitmap /*BitmapFactory.decodeStream(requireActivity().contentResolver.openInputStream(groupMainImageUri!!))*/
        val byteArrayOutS = ByteArrayOutputStream()
        //글라이드로 받은 bitmap을 압축해서 byteArray스트림에 넣음. 그리고, 그 스트림을 출력하여 새로운 byteArray를 만들고 그것을 다시 requestBody로 만들어 리턴
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutS)
        val requestBody = byteArrayOutS.toByteArray().run {
            toRequestBody("image/jpeg".toMediaTypeOrNull(),0, size )
        }
//        val groupImage = MultipartBody.Part.createFormData("group_main_image", file.name, requestBody)
        return requestBody
    }
    
    private fun createFile(realPath: String): File {
        return File(realPath)
    }

    private fun createRequestBody(file: File): RequestBody {
        return file.asRequestBody(mediaType)
    }

    private fun createPart(file: File, requestBody: RequestBody, formDataKeyName: String): MultipartBody.Part {
        return MultipartBody.Part.createFormData(formDataKeyName, file.name, requestBody)
    }

    //  /document/image: 에 해당하는 형식의 Uri 를 해석하고 그 해당 파일의 절대경로를 알아내어 문자열로 반환한다.
    private fun getPathFromURI(context: Context, uri: Uri): String {
        var realPath = String()
//        uri.path?.let { path ->
        uri.path?.let { path ->

            Log.e("[FileHelper]", "getPathFromURI path: $path")
            val databaseUri: Uri
            val selection: String?
            val selectionArgs: Array<String>?

            if (path.contains("/document/image:")||path.contains("/document/msf:") ) { // files selected from "Documents"
                Log.e("[FileHelper]", "getPathFromURI path: $path")
                databaseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                selection = "_id=?"
                selectionArgs = arrayOf(DocumentsContract.getDocumentId(uri).split(":")[1]) //getDocumentId 가 /document/image:123 부분 uri인듯

            } else if (path.contains("/document/") ) { // files selected from "Documents"
                Log.e("[FileHelper]", "getPathFromURI path: $path DocumentsContract.getDocumentId(uri):${DocumentsContract.getDocumentId(uri)}")
                databaseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                selection = "_id=?"
                selectionArgs = arrayOf(DocumentsContract.getDocumentId(uri)) //getDocumentId 가 /document/47 부분 uri인듯 id가 47이라 :으로 split할 필요가 없다

            } else { // files selected from all other sources, especially on Samsung devices
                databaseUri = uri
                selection = null
                selectionArgs = null
            }

            try {
                val column = "_data"
                val projection = arrayOf(column)
                val cursor = context.contentResolver.query(
                    databaseUri,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )
                cursor?.let {
                    val columnIndex = cursor.getColumnIndexOrThrow(column)
                    if (it.moveToFirst()) {
                        realPath = cursor.getString(columnIndex)
                    }
                    cursor.close()
                }
            } catch (e: Exception) {
                println(e)
            }
        }
        return realPath
    }


    //글라이드를 이용해 단순 Bitmap 반환하는 메소드
    suspend fun bitmapReturnUsingGlide(context: Context, url: String) : Bitmap?{
        var bitmap : Bitmap? = null
        try {
            //글라이드를 통해 파일 다운로드
            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                val requestManager = Glide.with(context)
                val loaded = requestManager.asBitmap().load(url) //받은 url 을 이용해 이미지를 Bitmap으로 불러옴
                    .diskCacheStrategy(DiskCacheStrategy.NONE) //disk cache 전략을 off
                    .skipMemoryCache(true)
                //받은 비트맵을 외부스토리지에 저장.
                loaded.into(object: CustomTarget<Bitmap>(){
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        bitmap = resource //받아와서 전달부터 해주고
                        cont.resumeWith(Result.success(Unit))  //중지된 루틴을 재개해주고 밑에서 bitmap 을 호출한 함수쪽으로 리턴해줌
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {
                    }
                })
    //            val file: File = requestManager.downloadOnly().load(url).submit().get()
            }
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return bitmap
    }

    ///////////////////////////////////////////////////////   글라이드로 받은 파일 로컬에 다운로드
    fun downLoadFile(context: Context, url: String) {
        try {
            //글라이드를 통해 파일 다운로드
            val requestManager = Glide.with(context)
            val loaded = requestManager.asBitmap().load(url) //받은 url 을 이용해 이미지를 Bitmap으로 불러옴
                .diskCacheStrategy(DiskCacheStrategy.NONE) //disk cache 전략을 off
                .skipMemoryCache(true)
            //받은 비트맵을 외부스토리지에 저장.
            loaded.into(object: CustomTarget<Bitmap>(){
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    saveImage(resource, context ) //받은 Bitmap 을 로컬에 저장함
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
    private fun saveImage(image: Bitmap, context: Context) {
        //디렉토리 만들기용 먼저 선언. 외부 스토리지: 이미지폴더 << 에 경로를 지정함. 폴더는 안드로이드 기기마다 디폴트가 다를 수 있음
        var storageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path)
        if (!storageDir.exists()) {
            storageDir.mkdir()  //해당 경로에 디렉토리가 없으면 만듦
        }
        if (storageDir.exists()) {
            val imageFile = File(storageDir, "${System.currentTimeMillis()}.jpeg") //저장될 파일 객체 만들기 (앞은 디렉토리위치, 뒤는 파일명)
//            requireContext().cacheDir.path
//            val savedImagePath = imageFile.absolutePath
            try {
                val fOut: OutputStream = FileOutputStream(imageFile)
                image.compress(Bitmap.CompressFormat.JPEG, 80, fOut) //글라이드로 받아온 비트맵 파일을 jpeg로 압축하고 파일을 실제로 아웃풋함
                fOut.close()
                scanMedia(imageFile, context) //미디어db갱신
                Toast.makeText(context, "이미지 저장완료", Toast.LENGTH_SHORT).show()
            } catch (e: java.lang.Exception) {
                Toast.makeText(context, "저장 중 오류발생", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } else {
            Toast.makeText(context, "폴더 만들기 실패!", Toast.LENGTH_SHORT).show()
        }
    }

    fun scanMedia(file: File?, context: Context) {
        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        intent.data = Uri.fromFile(file)
        (context as MainActivity).sendBroadcast(intent)
    }

/////////////////////////////////////////////////////////////////////////  글라이드로 로컬에 다운받기 끝








    /**
     * A helper function to get the captured file location.
     */
//    else if (path.contains("media/external/video")){
////                outputFileResults.savedUri: content://media/external/video/media/230
    //content:// 형식의 contentProvider가 제공한 Uri의 절대경로를 찾아내는 메소드이다. contentResolver의 query 메소드로 해당 cursor객체를 생성하여
    //그 커서에 포함된 _data 컬럼의 정보(절대경로)를 가져온다.
    private fun getAbsolutePathFromUri(context: Context, contentUri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = context
                .contentResolver
                //두번째 매개변수는 반환할 쿼리의 열목록을 말하는 것. DATA는 _data 행을 반환하는데 image 든 video 든 같은 열이 존재하므로 어느것을 써도 노상관.
                //_data 열에는 실제 내가 알고자하는 파일의 실제 경로가 담겨져있음.
                .query(contentUri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)
            if (cursor == null) {
                return null
            }
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst() //일단 커서의 위치를 맨 처음으로 옴겨서 초기화한다.
            cursor.getString(columnIndex) // 그후 커서의 인덱스에 해당하는 정보를 문자열로 가져온다.
        } catch (e: RuntimeException) {
            Log.e("VideoViewerFragment", String.format("Failed in getting absolute path for Uri %s with Exception %s",
                contentUri.toString(), e.toString() ))
            null
        } finally {
            cursor?.close()
        }
    }

    /**
     * A helper function to retrieve the captured file size.
     */
    private fun getFileSizeFromUri(context: Context, contentUri: Uri): Long? {
        val cursor = context
            .contentResolver
            .query(contentUri, null, null, null, null)
            ?: return null

        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        cursor.moveToFirst()

        //블록의 결과와 더불어 호출한 해당 리소스를 정상적으로 종료해제한다.
        cursor.use {
            return it.getLong(sizeIndex)
        }
    }

}