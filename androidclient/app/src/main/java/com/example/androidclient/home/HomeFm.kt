package com.example.androidclient.home

import androidx.navigation.Navigation.findNavController
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.NavController
import android.widget.Toast
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.example.androidclient.R
import com.example.androidclient.MyApp
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.example.androidclient.databinding.HomeFmBinding
import com.example.androidclient.databinding.MyProfileFmBinding
import com.example.androidclient.util.ImageHelper

class HomeFm : Fragment() {
    private var homeVm: HomeVm? = null
    var appBarConfiguration: AppBarConfiguration? = null
    private var navController: NavController? = null
    private var backKeyPressedTime: Long = 0

    var mbinding: HomeFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        homeVm = ViewModelProvider(requireActivity()).get(HomeVm::class.java)
        mbinding = HomeFmBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleOnBackPressed() //뒤로가기 종료 구현부
        appBarConfiguration = AppBarConfiguration.Builder(R.id.home_fm).build()
        navController = findNavController(view)
        setupWithNavController(binding.homeToolbar, navController!!, appBarConfiguration!!)


    }

    override fun onResume() {
        super.onResume()
        //        MainActivity mainA = (MainActivity)requireActivity();
//        ((MainActivity)requireActivity()).binding.mainToolbar.getMenu().findItem(R.id.main_toolbar_menu_logout).setVisible(false);
//        ((MainActivity)requireActivity()).binding.mainToolbar.getMenu().findItem(R.id.app_bar_search).setEnabled(false);
//        ((SearchView) mainA.binding.mainToolbar.getMenu().findItem(R.id.app_bar_search).getActionView()).setVisibility(View.GONE);
//        Log.e("test", "test: "+((MainActivity)requireActivity()).binding.mainToolbar.getMenu().findItem(R.id.app_bar_search).setVisible(false));

        //상단바 프로필 이미지 클릭시
        binding.homeToolbarIv.setOnClickListener {
            findNavController().navigate(R.id.action_global_myProfileFm)
        }
        //상단바 프로필 이미지 로딩
        ImageHelper.getImageUsingGlide(requireActivity(), MyApp.userInfo.user_image, binding.homeToolbarIv)

    }



    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }

    private fun handleOnBackPressed() { //뒤로가기 종료 구현부
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() { //백버튼을 조각에서 조종하기 위한 메소드.
//                        Toast.makeText(requireActivity(),"test1111",Toast.LENGTH_SHORT).show();
//                            requireActivity().finish(); //AlertDialog 창으로 종료할지 물어야함
                    if (parentFragmentManager.backStackEntryCount == 0) {
//                        if(getParentFragmentManager().getBackStackEntryAt(0).getId() == R.id.startFm){
                        뒤로가기종료() //두번 클릭시 종료처리
                    } else {
                        val navcon = NavHostFragment.findNavController(this@HomeFm)
                        navcon.navigateUp() //뒤로가기(백스택에서 뒤로가기?)
                    }
                }
            })
    }

    private fun 뒤로가기종료() {
        if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
            backKeyPressedTime = System.currentTimeMillis()
            Toast.makeText(requireActivity(),"뒤로 버튼을 한번 더 누르면 종료됩니다.",Toast.LENGTH_SHORT).show()
            return
        }
        if (System.currentTimeMillis() <= backKeyPressedTime + 2000) {
            requireActivity().finish()
            //            super.onBackPressed();
        }
    }
}