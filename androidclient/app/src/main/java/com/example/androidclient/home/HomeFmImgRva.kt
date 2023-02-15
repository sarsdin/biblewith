package com.example.androidclient.home
import android.util.Log

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.burhanrashid52.photoediting.EditImageActivity
import com.example.androidclient.bible.BibleVm
import com.example.androidclient.databinding.GroupChatInnerImageRvVhBinding
import com.example.androidclient.group.GroupChatInnerFm
import com.example.androidclient.group.GroupVm
import com.example.androidclient.util.ImageHelper

import com.google.gson.JsonObject
import com.unsplash.pickerandroid.photopicker.data.UnsplashPhoto

class HomeFmImgRva(val bibleVm: BibleVm, val homeVm: HomeVm, val homeFm: HomeFm) : RecyclerView.Adapter<HomeFmImgRva.HomeFmImgRvVh>() {
    val tagName = "[HomeFmImgRva]"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeFmImgRvVh {
        return HomeFmImgRvVh(GroupChatInnerImageRvVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: HomeFmImgRvVh, position: Int) {
        holder.bind(homeVm.unsplashL[position] /*as JsonObject*/)
    }

    override fun onViewAttachedToWindow(holder: HomeFmImgRvVh) {
        holder.setIsRecyclable(false)
        super.onViewAttachedToWindow(holder)
    }

    override fun getItemCount(): Int {
        if(homeVm.unsplashL.size == 0 /*|| homeVm.unsplashL.isJsonNull*/){
            return 0
        }
        return homeVm.unsplashL.size
    }

    inner class HomeFmImgRvVh(var binding: GroupChatInnerImageRvVhBinding) : RecyclerView.ViewHolder(binding.root) {
        //        var rva: GroupChatInnerFmVhRva? = null
        var rv: RecyclerView? = null

        init {
        }

        @SuppressLint("ClickableViewAccessibility")
        fun bind(mItem: UnsplashPhoto) {
//            this.mItem = mItem;

//            if(mItem.get("stored_file_name") != null && !mItem.get("stored_file_name").isJsonNull){
//                ImageHelper.getImageUsingGlide(homeFm.requireActivity(), mItem.get("stored_file_name").asString, binding.imageIv)
//            }

//            Log.i(tagName, "mItem.urls: ${mItem.urls}")
            ImageHelper.getImageUsingGlideForURI(homeFm.requireActivity(), Uri.parse(mItem.urls.small), binding.imageIv)


//            binding.imageIv.setOnTouchListener { v, event ->
//                //이건 ViewHolder의 root 레이아웃인 리니어 레이아웃인데 이러면 그 상위의 리사이클러뷰를 포함한 부모와 조상들 모두 터치이벤트를
//                //인터셉트하지 못하게되어 리사이클러뷰의 스크롤이 작동을 안한다. 터치시작을 뷰홀더로 시작하면 그 상위의 스크롤뷰 또한 작동안한다.
//                //그래서, 정확하게 슬라이드뷰의 root뷰를 지정해서 requestDisallowInterceptTouchEvent(true)를 사용하면 뷰홀더를 포함하는
//                //수평 스크롤이 슬라이드뷰(부모뷰)와 곂치지 않고 자식인 리사이클러뷰의 터치이벤트(수평 스크롤)에만 반응하여 작동하게 된다.
////                binding.root.requestDisallowInterceptTouchEvent(true)
//
////                homeFm.binding.includedLayout.root.requestDisallowInterceptTouchEvent(true)
//
//                //이것을 true로 두면 이벤트 전파를 막는듯!? 실험결과 false로해도 일단 리사이클러뷰와 슬라이드뷰의 수평스크롤은 곂치지 않았음.
//                //위의 requestDisallowInterceptTouchEvent(true)로 인해 전파가 이미 막혀서 영향이 없는 듯하다. 원래는 true로 두면 전파막는 역할임.
//                return@setOnTouchListener true
//            }

            //현재 이미지 홀더뷰를 클릭했을때 - 카드뷰임 현재
            binding.root.setOnClickListener {
//                ImageHelper.getImageUsingGlideForURI(homeFm.requireActivity(),
//                    Uri.parse(mItem.get("urls").asJsonObject.get("small").asString), homeFm.binding.photoEditorView.source)

                val intent = Intent(homeFm.requireActivity(), EditImageActivity::class.java)
                intent.action = Intent.ACTION_EDIT
//                intent.data = Uri.parse(mItem.get("urls").asJsonObject.get("regular").asString)
                intent.data =  Uri.parse(mItem.urls.regular)
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