package com.example.androidclient.group

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.androidclient.databinding.GroupChatFmBinding

class GroupChatFm : Fragment() {
    lateinit var groupVm: GroupVm
    lateinit var rva: GroupChatRva
    lateinit var rv: RecyclerView
    var mbinding: GroupChatFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        mbinding = GroupChatFmBinding.inflate(inflater, container, false)

        rv = binding.chatList
//        rv.layoutManager = GridLayoutManager(context, 2);
        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = GroupChatRva(groupVm, this)
        rva = rv.adapter as GroupChatRva

        return binding.root
    }



    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }
}