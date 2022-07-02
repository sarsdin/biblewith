package com.example.androidclient.moreinfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.bible.BibleVm
import com.example.androidclient.databinding.MyNoteFmListBinding

class MyNoteFm : Fragment() {
    lateinit var bibleVm: BibleVm
    var mbinding: MyNoteFmListBinding? = null
    lateinit var rva: MyNoteRva
    lateinit var recyclerView: RecyclerView
//    var mbinding: MyNoteFmListBinding? = null
        val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            bibleVm = ViewModelProvider(requireActivity()).get(BibleVm::class.java)
            mbinding = MyNoteFmListBinding.inflate(inflater, container, false)
            recyclerView = binding.myNoteFmList
            recyclerView.layoutManager = LinearLayoutManager(binding.root.context)
            //        recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            recyclerView.adapter = MyNoteRva(bibleVm, this)
            rva = recyclerView.adapter as MyNoteRva
            return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
        }

        override fun onDestroyView() {
            super.onDestroyView()
            mbinding = null
        }
}