package com.example.androidclient.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.R
import com.example.androidclient.bible.BibleVm
import com.example.androidclient.databinding.ChallengeCreateFmBinding
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class ChallengeCreateFm : Fragment() {
    lateinit var groupVm: GroupVm
    lateinit var bibleVm: BibleVm
    lateinit var rva: ChallengeCreateFmRva
    lateinit var rv: RecyclerView
    var mbinding: ChallengeCreateFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
        bibleVm = ViewModelProvider(requireActivity()).get(BibleVm::class.java)
        mbinding = ChallengeCreateFmBinding.inflate(inflater, container, false)

        //책정보를 새로운 jsonArray로 복사함 - 챌린지 만들기 에서 사용할 json 리스트 생성!
        bibleVm.bookLForSearch.forEach {
            val jo = JsonObject()
            jo.addProperty("book", it.book) //책번호
            jo.addProperty("book_name", it.book_name) //책이름
            jo.addProperty("is_selected", false) //선택유무
            groupVm.createList.add(jo)
//            groupVm.selectedCreateList.add(jo.deepCopy())
        }

        rv = binding.createList
        rv.layoutManager = GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false);
        rv.adapter = ChallengeCreateFmRva(groupVm, bibleVm, this)
        rva = rv.adapter as ChallengeCreateFmRva

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = Navigation.findNavController(view)
//        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val appBarConfiguration = AppBarConfiguration.Builder(R.id.group_fm).build()
        NavigationUI.setupWithNavController(binding.createToolbar, navController, appBarConfiguration)


        //다음 버튼 클릭시 - 제목을 가져오고 다음페이지로 감
        binding.nextBt.setOnClickListener {
            groupVm.createJo.addProperty("chal_title", binding.titleInput.text.toString())
            findNavController().navigate(R.id.action_challengeCreateFm_to_challengeCreateNextFm)
        }

        //버튼 활성화 조건감지
        groupVm.liveSelectedCreateList.observe(viewLifecycleOwner, Observer {
            if(it.size() > 0 &&  binding.titleInput.text.toString() != ""){
                binding.createToolbarAddBt.visibility = View.VISIBLE
                binding.nextBt.isEnabled = true //하나라도 선택되었다고 제목이 비어있지 않으면 버튼 활성화
            } else {
                binding.createToolbarAddBt.visibility = View.GONE
                binding.nextBt.isEnabled = false
            }
        })



    }













}