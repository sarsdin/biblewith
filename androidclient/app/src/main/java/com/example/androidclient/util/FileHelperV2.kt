package com.example.androidclient.util

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

class FileHelperV2 {

    val BYTE_STREAM: String = "application/octet-stream"

    fun readFileFromUri(context: Context, uri: Uri): ByteArray? {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                return inputStream.readBytes()
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
        }
        return null
    }



    fun saveFileToDownloads(context: Context, fileName: String, fileContent: ByteArray) {
        //저장될 또는 만들어질 콘텐츠(문서,이미지,비디오 파일등)의 정보를 담은 메타데이터.
        // 이것을 이용해 contentResolver에서 콘텐츠에 대한 가상 Uri를 생성할 수 있음.
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
//            put(MediaStore.Downloads.MIME_TYPE, BYTE_STREAM)
            put(MediaStore.Downloads.MIME_TYPE, getMimeTypeFromFileName(fileName))

        }

        val uriOfFolder = buildUriCollection(contentValues)

        val resolver = context.contentResolver
        val uri = resolver.insert(uriOfFolder, contentValues) //콘텐츠리졸버에 위에서 생성한 저장할 폴더Uri와 콘텐츠값을 전달하여 최종Uri를 생성.

        if (uri != null) {
            try {
                //위에서 만든 Uri에 해당하는 위치에 outputStream을 만들고 받아온 ByteArray를 씀.
                //기존처럼 File 객체를 만들고 절대경로를 찾아 입력하고 할 필요 없음. resolver에서 처리함.
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(fileContent)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                contentValues.clear()
                if (isSdkHigherThan28()) {
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0) //현재 리졸버의 콘텐츠값을 pending false로 바꿔서 작업흐름정리함.
                }
                resolver.update(uri, contentValues, null, null)
            }
        }
    }

    /**
     * 파일이름의 확장자를 기준으로 MIME 타입을 MimeTypeMap 으로부터 가져옴.
     */
    fun getMimeTypeFromFileName(fileName: String): String {
        val extension = fileName.substringAfterLast(".")
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        return mimeType ?: "application/octet-stream"
    }

    /**
     * (저장할) 외부 저장소의 sdk version에 따른 Uri 주소를 가져옴.
     */
    @SuppressLint("InlinedApi")
    private fun buildUriCollection(contentValues: ContentValues): Uri {
        val uri: Uri

        // todo  MediaStore.Images, MediaStore.Video, MediaStore.Audio 등의 폴더도 지정가능.
        if (isSdkHigherThan28()) {
//            uri = MediaStore.Images.Media.getContentUri(
//                MediaStore.VOLUME_EXTERNAL_PRIMARY
//            )
            uri = MediaStore.Downloads.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 1)
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)

        } else {
//            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        }
        return uri
    }

    fun isSdkHigherThan28(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }


    /**
     *  contentResolver로부터 Uri를 이용해 해당하는 파일의 이름을 가져오는 메서드.
     */
    fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var fileName: String? = null
        val contentResolver = context.contentResolver
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)

        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                fileName = cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
        return fileName
    }



}