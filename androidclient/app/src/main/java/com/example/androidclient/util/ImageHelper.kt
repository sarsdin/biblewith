package com.example.androidclient.util
import android.content.Context
import android.net.Uri
import com.bumptech.glide.Glide

import android.widget.ImageView
import com.example.androidclient.R

class ImageHelper {

    val UPLOADS_URL = "http://15.165.174.226/uploads/"

    fun getImageUsingGlide(context: Context, url :String, view :ImageView) {
        val uri = Uri.parse(UPLOADS_URL + url)
        Glide.with(context) //해당 환경의 Context나 객체 입력
        .load(uri) //URL, URI 등등 이미지를 받아올 경로
//        .diskCacheStrategy(DiskCacheStrategy.NONE) //disk cache 전략을 off
//        .skipMemoryCache(true) //memory cache 전략을 off. disk와 같이 꺼야 같은 url 에서 이미지를 반복적으로 로딩가능.캐시전략 on: url이 같으면 같은 이미지취급
//        .override(184,153) // override(width, height) : 받아온 이미지의 크기를 조절할 수 있는 함수
        .centerCrop() // 외부에서 받아온 이미지가 있다면, 가운데에서 이미지를 잘라 보여주는 함수
        .placeholder(R.drawable.ic_baseline_cloud_24) // 이미지가 로딩하는 동안 보여질 이미지를 정함
        .error(R.drawable.ic_xmark) //이미지를 불러오는데 실패 했을때 보여질 이미지를 정함
        .into(view) //받아온 이미지를 받을 공간(ex. ImageView)
    }

}