package com.example.androidclient.util
import android.content.Context
import android.net.Uri
import com.bumptech.glide.Glide

import android.widget.ImageView
import com.example.androidclient.R
import com.example.androidclient.util.Http.UPLOADS_URL

class ImageHelper {


    companion object {
        @JvmField   //java 의 스태틱처럼 사용가능 field는 필드, static은 메소드에 사용
//        val UPLOADS_URL = "http://15.165.174.226/uploads/"
        val UPLOADS_URL = "http://129.154.212.0:8085/uploads/"

        /**
         * 문자열주소를 이용해서 이미지를 불러옴. 
         * 기본적으로 아래의 주소를 디폴트로 prefix 로 적용함
         * val UPLOADS_URL = "http://15.165.174.226/uploads/"
         * */
        @JvmStatic fun getImageUsingGlide(context: Context, url :String?, view :ImageView) {
            //받아온 문자열이 널이 아니면 실행함 - 자바의 필드값은 null 허용이라 String이 null 일지도 모르기 때문..
            url?.let {
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

        /**
         * Uri 객체를 이용해서 이미지를 불러옴
         * */
        fun getImageUsingGlideForURI(context: Context, uri :Uri?, view :ImageView) {
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

}