package com.example.androidclient.moreinfo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.MyApp
import com.example.androidclient.bible.BibleDto
import com.example.androidclient.bible.BibleVm
import com.example.androidclient.databinding.MyHighLightFmListBinding
import com.example.androidclient.util.Http
import com.example.androidclient.util.Http.HttpBible
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyHighLightFm : Fragment() {
    lateinit var bibleVm: BibleVm
    var mbinding: MyHighLightFmListBinding? = null
    lateinit var rva: MyHighLightRva
    lateinit var recyclerView: RecyclerView
//    var mbinding: MyHighLightFmListBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        bibleVm = ViewModelProvider(requireActivity()).get(BibleVm::class.java)
        mbinding = MyHighLightFmListBinding.inflate(inflater, container, false)
//          DataBindingUtil.inflate<ViewDataBinding>(inflater, R.layout.my_high_light_fm_list, container, false)
//        val bmbinding = DataBindingUtil.bind<ViewDataBinding>(mbinding!!.root)
        bibleVm.하이라이트목록가져오기()

        recyclerView = binding.myHighLightFmList
        recyclerView.layoutManager = LinearLayoutManager(binding.root.context)
        //        recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
        recyclerView.adapter = MyHighLightRva(bibleVm, this)
        rva = recyclerView.adapter as MyHighLightRva
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bibleVm.liveHighL.observe(viewLifecycleOwner, Observer<List<BibleDto>> {
            rva.notifyDataSetChanged()
//            binding.root.invalidate()
            //양방향 데이터는 viewModel의 라이브데이터와 @={} 이 기호로 연결되어 사용되며 체크박스를 예로 체크되었다면 중괄호안의 viewModel데이터의 변경을 옵저
            //버가 인식을 하여 즉각적으로 ui를 변경할 수 있게 하는 방법.
        })
    }


    fun 하이라이트목록가져오기() {
        val retrofit = Http.getRetrofitInstance(bibleVm.host)
        val httpBible = retrofit.create(HttpBible::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        httpBible.getHlList(MyApp.userInfo.user_no)?.enqueue(object :Callback<List<BibleDto>>{
            override fun onResponse(call: Call<List<BibleDto>>, response: Response<List<BibleDto>>) {
                if (response.isSuccessful ){
                    var res : List<BibleDto>? = response.body()
                    Log.e("[MyHighLightFm]", "하이라이트목록가져오기 onResponse: $res")
                    bibleVm.highL = res
                    bibleVm.liveHighL.value = bibleVm.highL
                }
            }
            override fun onFailure(call: Call<List<BibleDto>>, t: Throwable) {
                    Log.e("[MyHighLightFm]", "하이라이트목록가져오기 onFailure: ${t.message}")
            }
        })
    }


    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }
}