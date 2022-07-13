package com.example.androidclient.group

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.R
import com.example.androidclient.databinding.GroupFmBinding
import com.example.androidclient.home.MainActivity


class GroupFm : Fragment() {
    lateinit var groupVm: GroupVm
//    lateinit var rva: GroupRva
    lateinit var rv: RecyclerView
    var mbinding: GroupFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mbinding = GroupFmBinding.inflate(inflater, container, false)
        groupVm = ViewModelProvider(this).get(GroupVm::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //메인액티비티의 툴바는 감춤
        (requireActivity() as MainActivity).binding.mainToolbar.visibility = View.GONE

        //툴바
//        binding.groupMainToolbar.set
        val navController = Navigation.findNavController(view)
//        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val appBarConfiguration = AppBarConfiguration.Builder(R.id.group_fm).build()
//        binding.groupMainCollapsingToolbar.setupWithNavController(binding.groupMainToolbar, navController, appBarConfiguration)
        binding.groupMainToolbar.setupWithNavController(navController, appBarConfiguration)

        //스크롤 이벤트로 밑으로 갈시 바텀네비게이션 감추기
        binding.groupMainNestedScrollView.setOnScrollChangeListener(View.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
//            Log.e("[GroupFm]", "scroll ev : $scrollY , $oldScrollY" );
            if(scrollY > oldScrollY){ //이전 위치보다 높다는 말은 밑으로 스크롤 한다는 말
                (requireActivity() as MainActivity).binding.mainBottomNav.visibility = View.GONE
            } else {
                (requireActivity() as MainActivity).binding.mainBottomNav.visibility = View.VISIBLE
            }
        })


    }

    override fun onResume() {
        super.onResume()
//        (requireActivity() as MainActivity).supportActionBar?.hide() //지원바를 설정시에만 사용(setSupportActionBar). 그냥 toolbar를 사용했을때는 작동안함.
        //스크롤 이벤트로 감춰진 바텀네비게이션 다시 보이게(이벤트의 의도된작동인지모르는데 네비가 사라짐. 이때 이페이지로 복귀했을때 첫화면에서는 보여야하므로)
        (requireActivity() as MainActivity).binding.mainBottomNav.visibility = View.VISIBLE

    }

    override fun onPause() {
        super.onPause()
        //onViewCreated에서 감추었던 메인액티비티의 툴바를 다시 보이게 함(다른 화면에서는 보여야하므로)
        (requireActivity() as MainActivity).binding.mainToolbar.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }
}