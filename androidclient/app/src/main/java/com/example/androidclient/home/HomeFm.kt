package com.example.androidclient.home
import android.app.Activity
import android.net.Uri
import android.util.Log

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
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.example.androidclient.BuildConfig
import com.example.androidclient.bible.BibleVm
import com.example.androidclient.databinding.HomeFmBinding
import com.example.androidclient.databinding.MyProfileFmBinding
import com.example.androidclient.group.GroupVm
import com.example.androidclient.util.GridSpacingItemDecoration
import com.example.androidclient.util.ImageHelper
import com.unsplash.pickerandroid.photopicker.UnsplashPhotoPicker
import com.unsplash.pickerandroid.photopicker.data.UnsplashPhoto
import com.unsplash.pickerandroid.photopicker.presentation.UnsplashPickerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

class HomeFm : Fragment() {
    val tagName = "[HomeFm]"
    lateinit var homeVm: HomeVm
    lateinit var groupVm: GroupVm
    lateinit var bibleVm: BibleVm
    var appBarConfiguration: AppBarConfiguration? = null
    private var navController: NavController? = null
    private var backKeyPressedTime: Long = 0

    lateinit var rv: RecyclerView
    lateinit var rva: HomeFmImgRva
    lateinit var imgVpa: HomeFmImgVpa

    var mbinding: HomeFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UnsplashPhotoPicker.init(
            MyApp.application, // application
            BuildConfig.API_KEY,
            BuildConfig.API_SKEY
            /* optional page size */
        )

        homeVm = ViewModelProvider(requireActivity()).get(HomeVm::class.java)
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
        bibleVm = ViewModelProvider(requireActivity()).get(BibleVm::class.java)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mbinding = HomeFmBinding.inflate(inflater, container, false)

        rv = binding.imgRv
        rv.setHasFixedSize(true)
        rv.itemAnimator = null
        rv.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL).apply {
            gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
//            gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
        }
        rv.addItemDecoration(GridSpacingItemDecoration(2, 20, true))
        rv.adapter = HomeFmImgRva(bibleVm, homeVm, this)
        rva = rv.adapter as HomeFmImgRva

        return binding.root
    }

    //editImageActivity 를 열었던 결과를 콜백받음.
    val editImageActivityForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result:ActivityResult ->
        val resultCode = result.resultCode
        val data = result.data

        if(resultCode == Activity.RESULT_OK){
//            val photos : ArrayList<UnsplashPhoto>? = data?.getParcelableArrayListExtra(UnsplashPickerActivity.EXTRA_PHOTOS)
//            if (photos != null) {
//                homeVm.unsplashL = photos
//                homeVm.liveUnsplashL.value = homeVm.unsplashL
//            }

            if (data != null) {
                Log.e(tagName, "editImageActivityForResult: ${data.getStringExtra("editFinished")}")
            } else {
                Log.e(tagName, "editImageActivityForResult: data is null")
            }

        }
    }

    //unsplash picker Activity 를 열었던 결과를 콜백받음.
    val unsplashForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result:ActivityResult ->
        val resultCode = result.resultCode
        val data = result.data

        if(resultCode == Activity.RESULT_OK){
            val photos : ArrayList<UnsplashPhoto>? = data?.getParcelableArrayListExtra(UnsplashPickerActivity.EXTRA_PHOTOS)
            if (photos != null) {
                homeVm.unsplashL = photos
                homeVm.liveUnsplashL.value = homeVm.unsplashL
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleOnBackPressed() //뒤로가기 종료 구현부
        appBarConfiguration = AppBarConfiguration.Builder(R.id.home_fm).build()
        navController = findNavController(view)
        setupWithNavController(binding.homeToolbar, navController!!, appBarConfiguration!!)

        //뷰페이저 셋팅
        binding.imgVp.adapter = HomeFmImgVpa(homeVm, this)
        imgVpa = binding.imgVp.adapter as HomeFmImgVpa
        binding.imgVp.clipToPadding = false
        binding.imgVp.clipChildren = false
        binding.imgVp.offscreenPageLimit = 3
        binding.imgVp.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        val compositePageTransformer = CompositePageTransformer()
        compositePageTransformer.addTransformer(MarginPageTransformer(40))
        compositePageTransformer.addTransformer(object : ViewPager2.PageTransformer {
            override fun transformPage(page: View, position: Float) {
                var r = 1 - abs(position)
                page.scaleY = 0.85f + r * 0.15f
            }
        })
        binding.imgVp.setPageTransformer(compositePageTransformer)



        //더보기 버튼 클릭시
        binding.moreTv.setOnClickListener {
            Log.e(tagName, "moreTv clicked unsplash")
            unsplashForResult.launch(UnsplashPickerActivity.getStartingIntent(requireActivity(), true))
        }

        //임시 이미지 - UnsplashPickerActivity 전용 - json으로 data 받아서 쓸거기 때문에 안쓸듯
        homeVm.liveUnsplashL.observe(viewLifecycleOwner, Observer {
            rva.notifyDataSetChanged()
        })

        //성경 일독 갱신
        homeVm.liveTodayVerse.observe(viewLifecycleOwner, Observer {
            if (it.get("bible_no") != null) {
                binding.verseTv.text = "${it.get("content").asString}"
                binding.bookTv.text = "${it.get("book_name").asString} ${it.get("chapter").asString}:${it.get("verse").asString}"
//                binding.profileIv //이미지 바꿔주기
            }
        })

        //홈 이미지 Unsplash Api 랜덤 이미지 로드
        homeVm.liveUnsplashRandomL.observe(viewLifecycleOwner, Observer {
            val profileSt = homeVm.unsplashRandomL.get(9).asJsonObject.get("urls").asJsonObject.get("regular").asString
            Log.e(tagName, "profileSt: $profileSt")

            ImageHelper.getImageUsingGlideForURI(requireActivity(), Uri.parse(profileSt), binding.profileIv)
            imgVpa.notifyDataSetChanged()
        })

        CoroutineScope(Dispatchers.Default).launch {
            homeVm.성경일독(true)
            homeVm.랜덤이미지(true)
        }

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


        //프로파일 이미지 등록
//        binding.profileIv
//        if(homeVm.unsplashRandomL){
//
//        }
//        val profileSt = homeVm.unsplashRandomL.get(0).asJsonObject.get("urls").asJsonObject.get("regular").asString
//        ImageHelper.getImageUsingGlide(requireActivity(), profileSt, binding.profileIv)

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