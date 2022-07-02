package com.example.androidclient.moreinfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.bible.BibleVm
import com.example.androidclient.databinding.MyHighLightBtsListBinding
import com.example.androidclient.databinding.MyHighLightBtsVhBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment



class MyHighLightBts : BottomSheetDialogFragment() {

    private var _binding: MyHighLightBtsListBinding? = null
    lateinit var bibleVm : BibleVm
    lateinit var rva: MyHighLightBtsRva
    lateinit var recyclerView: RecyclerView

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        bibleVm = ViewModelProvider(requireActivity()).get(BibleVm::class.java)
        _binding = MyHighLightBtsListBinding.inflate(inflater, container, false)

//        this.dialog?.setCanceledOnTouchOutside(false)
//        this.dialog?.window?.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        this.dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)


        //내용 리스트
        recyclerView = binding.myHighLightBtsList
        recyclerView.layoutManager = LinearLayoutManager(context).apply { orientation = LinearLayoutManager.HORIZONTAL }
        recyclerView.adapter = MyHighLightBtsRva(/*bibleVm*/)
        rva = recyclerView.adapter as MyHighLightBtsRva

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        activity?.findViewById<RecyclerView>(R.id.list)?.layoutManager = LinearLayoutManager(context)
//        activity?.findViewById<RecyclerView>(R.id.list)?.adapter = arguments?.getInt(ARG_ITEM_COUNT)?.let { ItemAdapter(it) }

    }



     inner class MyHighLightBtsRva internal constructor(/*private val bibleVm: BibleVm*/) : RecyclerView.Adapter<MyHighLightBtsVh>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHighLightBtsVh {

            return MyHighLightBtsVh(MyHighLightBtsVhBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: MyHighLightBtsVh, position: Int) {
            holder.bind()
        }

        override fun getItemCount(): Int {
            return 15
        }
    }

    inner class MyHighLightBtsVh internal constructor(binding: MyHighLightBtsVhBinding)
        : RecyclerView.ViewHolder(binding.root) {
//        internal val hellotext: TextView = binding.hellotext

        fun bind() {

        }


    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}





// TODO: Customize parameter argument names
//const val ARG_ITEM_COUNT = "item_count"
//companion object {
//
//    // TODO: Customize parameters
//    fun newInstance(itemCount: Int): MyHighLightBts =
//            MyHighLightBts().apply {
//                arguments = Bundle().apply {
//                    putInt(ARG_ITEM_COUNT, itemCount)
//                }
//            }
//
//}