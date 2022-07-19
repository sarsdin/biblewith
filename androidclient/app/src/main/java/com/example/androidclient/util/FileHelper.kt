package com.example.androidclient.util

import android.R
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.graphics.drawable.toBitmap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File


/**
 * 각종 안드로이드의 uri형식에 해당하는 File의 절대경로를 알아내어 해당파일을 만들고
 그에 해당하는 레트로핏 requestBody를 포함하는 MultipartBody.Part 를 반환한다.
 */
class FileHelper {
    val mediaType = "multipart/form-data".toMediaTypeOrNull()

    fun getPartBodyFromUri(context: Context, uri: Uri, formDataKeyName: String): MultipartBody.Part {
        val realPath = getPathFromURI(context, uri)
        val fileImage = createFile(realPath)
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

    //contentProvider 에서 받은 다중 uri 이미지를 요청용 멀티파트 리스트로 만들기
    fun getPartBodyFromUriList(context: Context, uriL: List<Uri>?, formDataKeyName: String): List<MultipartBody.Part> {
        val res = mutableListOf<MultipartBody.Part>()
        val realPathList = mutableListOf<String>()
        //각 Uri 에 담긴 path 를 이용해 실제 경로를 얻고, 그경로의 파일객체를 만들고, 요청용 바디에 담아서 멀티파트 객체로 랩핑한다. 그 각 객체는 리스트에 반복적으로 추가됨.
        uriL?.forEach {
            val realPath = getPathFromURI(context, it)
            realPathList.add(realPath)
            val fileImage = createFile(realPath)
            val requestBody = createRequestBody(fileImage)
            res.add(createPart(fileImage, requestBody, formDataKeyName))
        }
        Log.e("[FileHelper]", "realPathList: $realPathList")
        Log.e("[FileHelper]", "mutableListOf: $res")
        return res
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

    private fun getPathFromURI(context: Context, uri: Uri): String {
        var realPath = String()
        uri.path?.let { path ->

            val databaseUri: Uri
            val selection: String?
            val selectionArgs: Array<String>?
            if (path.contains("/document/image:")) { // files selected from "Documents"
                databaseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                selection = "_id=?"
                selectionArgs = arrayOf(DocumentsContract.getDocumentId(uri).split(":")[1]) //getDocumentId 가 /document/image:123 부분 uri인듯
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
                    if (it.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndexOrThrow(column)
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
}