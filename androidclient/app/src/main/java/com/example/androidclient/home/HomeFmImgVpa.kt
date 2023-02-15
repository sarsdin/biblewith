package com.example.androidclient.home

import android.R
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.burhanrashid52.photoediting.EditImageActivity
import com.example.androidclient.databinding.HomeFmImgVhBinding
import com.example.androidclient.util.ImageHelper
import com.google.gson.JsonObject
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.shape.ShapeBuilder
import ja.burhanrashid52.photoeditor.shape.ShapeType


class HomeFmImgVpa(val homeVm: HomeVm, val homeFm: HomeFm) : RecyclerView.Adapter<HomeFmImgVpa.HomeFmImgVpaVh>() {
    val tagName = "[HomeFmImgVpa]"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeFmImgVpaVh {
        return HomeFmImgVpaVh(HomeFmImgVhBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: HomeFmImgVpaVh, position: Int) {
        holder.bind(homeVm.unsplashRandomL[position].asJsonObject)
    }

    override fun getItemCount(): Int {
        if(homeVm.unsplashRandomL.size() != 0){
            return homeVm.unsplashRandomL.size()
        }
        return 0
    }

    inner class HomeFmImgVpaVh(var binding: HomeFmImgVhBinding) :RecyclerView.ViewHolder(binding.root) {

        fun bind(mItem: JsonObject) {
//            Log.i(tagName, "mItem.urls: ${mItem.get("urls")}")
            ImageHelper.getImageUsingGlideForURI(homeFm.requireActivity(),
                Uri.parse(mItem.get("urls").asJsonObject.get("small").asString), binding.imageIv)


            //현재 이미지 홀더뷰를 클릭했을때 - 카드뷰임 현재
            binding.root.setOnClickListener {
//                ImageHelper.getImageUsingGlideForURI(homeFm.requireActivity(),
//                    Uri.parse(mItem.get("urls").asJsonObject.get("small").asString), homeFm.binding.photoEditorView.source)

                val intent = Intent(homeFm.requireActivity(), EditImageActivity::class.java)
                intent.action = Intent.ACTION_EDIT
                intent.data = Uri.parse(mItem.get("urls").asJsonObject.get("regular").asString)
                intent.putExtra("content", homeVm.todayVerse.asJsonObject.get("content").asString)
                val send_st = "${homeVm.todayVerse.asJsonObject.get("book_name").asString} " +
                        "${homeVm.todayVerse.asJsonObject.get("chapter").asString}:${homeVm.todayVerse.asJsonObject.get("verse").asString}"
                intent.putExtra("book", send_st)
//                    Uri.parse(mItem.get("urls").asJsonObject.get("small").asString)

//                val mIntent  = Intent(getActivity().getApplicationContext(), WriteActivity:: class.java)
//                /*homeFm.*/startActivityForResult(intent, 14)
                
                homeFm.editImageActivityForResult.launch(intent) //이미지 에티터를 연다

            }

        }


    }

}









//
////Use custom font using latest support library
////                val mTextRobotoTf = ResourcesCompat.getFont(homeFm.requireActivity(), homeFm.requireActivity().resources.getFont(R.))
////loading font from asset
//val mTextRobotoTf = Typeface.createFromAsset(homeFm.requireActivity().assets, "maruburibold.ttf")
////                val mEmojiTypeFace = Typeface.createFromAsset(homeFm.requireActivity().assets, "maruburibold.ttf")
//
//val mPhotoEditor = PhotoEditor.Builder(homeFm.requireActivity(), homeFm.binding.photoEditorView)
//    .setPinchTextScalable(true)
//    .setClipSourceImage(true)
//    .setDefaultTextTypeface(mTextRobotoTf)
////                    .setDefaultEmojiTypeface(mEmojiTypeFace)
//    .build()
//
//mPhotoEditor.setBrushDrawingMode(true);
////                mPhotoEditor.addShape(shape)
////                mPhotoEditor.brushSize = 20
////                mPhotoEditor.setOpacity(opacity)
////                mPhotoEditor.brushColor = Color.GREEN
//mPhotoEditor.brushEraser()
//
//val mShapeBuilder = ShapeBuilder()
//    .withShapeOpacity(100)
//    .withShapeType(ShapeType.OVAL)
//    .withShapeSize(50f)
//    .withShapeColor(Color.GREEN)
//
//mPhotoEditor.setShape(mShapeBuilder)