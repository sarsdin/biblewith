package jm.preversion.biblewith.home
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
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
import jm.preversion.biblewith.R
import jm.preversion.biblewith.MyApp
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
import jm.preversion.biblewith.BuildConfig
import jm.preversion.biblewith.MainActivity
import jm.preversion.biblewith.bible.BibleVm
import jm.preversion.biblewith.databinding.HomeFmBinding
import jm.preversion.biblewith.group.GroupVm
import jm.preversion.biblewith.login.LoginActivity
import jm.preversion.biblewith.util.GridSpacingItemDecoration
import jm.preversion.biblewith.util.ImageHelper
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

        더보기리사이클러뷰초기화()

        return binding.root
    }

    private fun 더보기리사이클러뷰초기화() {
        rv = binding.imgRv
        rv.setHasFixedSize(true)
        rv.itemAnimator = null
        rv.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL).apply {
                gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
    //            gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
            }
        rv.addItemDecoration(GridSpacingItemDecoration(2, 20, true))
        rv.adapter = HomeFmImgRva(bibleVm, homeVm, this)
        rva = rv.adapter as HomeFmImgRva
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

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        handleOnBackPressed() //뒤로가기 종료 구현부
        appBarConfiguration = AppBarConfiguration.Builder(R.id.home_fm).build()
        navController = findNavController(view)
        setupWithNavController(binding.homeToolbar, navController!!, appBarConfiguration!!)

        //뷰페이저 셋팅
        settingImgVp()


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

    private fun settingImgVp() {
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


        초대링크처리((context as MainActivity).intent)
    }



    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }

/*    private fun handleOnBackPressed() { //뒤로가기 종료 구현부
        Log.e(tagName, "뒤로가기 디버깅용")
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() { //백버튼을 조각에서 조종하기 위한 메소드.
                    val navcon = NavHostFragment.findNavController(this@HomeFm)
                    Toast.makeText(requireActivity(),"뒤로 버튼 클릭1.",Toast.LENGTH_SHORT).show()
                    Log.e(tagName, "뒤로가기 클릭1")
                    if (navcon.currentDestination?.id == R.id.home_fm) {
//                        if(getParentFragmentManager().getBackStackEntryAt(0).getId() == R.id.startFm){
                        뒤로가기종료() //두번 클릭시 종료처리
                    } else {
                        Toast.makeText(requireActivity(),"뒤로 버튼 클릭3.",Toast.LENGTH_SHORT).show()
                        Log.e(tagName, "뒤로가기 클릭3")
                        navcon.navigateUp() //뒤로가기(백스택에서 뒤로가기?)
                    }
                }
            })
    }

    private fun 뒤로가기종료() {
        Log.e(tagName, "뒤로가기 클릭2")
        Toast.makeText(requireActivity(),"뒤로 버튼 클릭2.",Toast.LENGTH_SHORT).show()
        if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
            backKeyPressedTime = System.currentTimeMillis()
            Toast.makeText(requireActivity(),"뒤로 버튼을 한번 더 누르면 종료됩니다.",Toast.LENGTH_SHORT).show()
            return
        }
        if (System.currentTimeMillis() <= backKeyPressedTime + 2000) {
            requireActivity().finish()
            //            super.onBackPressed();
        }
    }*/


    fun 초대링크처리(intent: Intent? ){
        //외부에서 모임초대 딥링크를 타고 들어왔을때 처리 - 앱이 꺼져있을때 링크를 실행하면 켜지지만 유저정보가 없는, 즉, 로그인상태가 아니기에
        //다시 LoginActivity 로 보내짐. 거기서 분기점: 자동로그인일때와 아닐때가 나뉘는데, 아니면 그냥 로그인 진행하도록 하면되고, 자동로그인이면
        //로그인되어 다시 이곳으로 보내짐. 이때 딥링크로 전달되었던 intent의 정보는 없어진 상태라 intent.data & action의 값은 null이됨.
        //유저정보(로그인)는 있지만 data가 null인 상태가 되어서 home_fm으로 가게됨 == 앱이 꺼진상태라면 로직상 최종적으로 잘가봤자 home_fm 화면까지임.
        //여기서 자동로그인기능까지 염두에 두고 초대장을 보내고 싶다면 LoginActivity로 가는 시점에 수동 intent의 data 정보를 넣어주면됨.
        //그리고, 자동로그인시 다시 그 정보를 intent에 넣어서 이곳으로 보내오고 여기서 로그인정보 + 딥링크초대정보까지 모두 가진 상태로 진행하면 초대장을
        //받을 수 있음!

//        val intent = mainActivity.intent
        val action = intent?.action
        val data = intent?.data

        // todo  아래는 딥링크로 지정한 URI의 각 부분(segment)을 아래의 메서드를 이용해 어떤 부분을 불러오는지 확인한 로그임.
        Log.e(tagName,"deepLinkInfo: $action, $data") //android.intent.action.VIEW, http://biblewith.com/invite/1

        if (data != null) {
            Log.e(tagName, "getPath: " + data.path) //  /invite/1
            Log.e(tagName, "getPathSegments: " + data.pathSegments) // [invite, 1]
            Log.e(tagName, "getLastPathSegment: " + data.lastPathSegment) // 1
            Log.e(tagName, "getQuery: " + data.query) // null
            Log.e(tagName, "getScheme: " + data.scheme) // http
            Log.e(tagName, "getSchemeSpecificPart: " + data.schemeSpecificPart) //  //biblewith.com/invite/1
            Log.e(tagName, "getFragment: " + data.fragment) // null
            Log.e(tagName, "getHost: " + data.host) // biblewith.com
            Log.e(tagName, "getQuery: " + data.query) //
        }

        //외부에서 모임초대 딥링크를 타고 들어왔을때 처리
        //로그인한 상태라 user 정보가 이미 있을 경우에는 초대 로직 그대로 진행. 아니면 LoginActivity 로 보내서 로그인 진행 처리
        if(MyApp.userInfo != null){
            //todo   http://biblewith.com/invite/1/c/660998 이라는 딥링크를 homeFm에 지정해뒀다면, getPathSegments의 반환된 List는
            //   [invite, 1, c, 660998] 이것이고, 인덱스는 0부터 시작.
            if (data != null && data.pathSegments[0] == "invite") {
//                val toMain = Intent(MyApp.getApplication(), MainActivity::class.java)
//                toMain.putExtra("group_no", data.getPathSegments().get(1)) //getPathSegments: [invite, 1, c, 660998]
//                toMain.putExtra("invite_code", data.getPathSegments().get(3))
//                //초대링크를 타고 들어왔냐는 시그널용도 - 모임 홈에서 viewmodel 처리후 이 시그널에 따른 로직을 처리 후(서버에 invite_code 검증 후 멤버추가)
//                // 위의 group_no를 이용해 해당 모임으로이동
//                toMain.putExtra("is_invited", true)
//                startActivity(toMain)
//                            Uri uri = Uri.parse("http://biblewith.com/groupfm/" + data.getPathSegments().get(1) +
//                                    "/c/" + data.getPathSegments().get(3) + "?dest=group_fm");

                val group_no = data.getPathSegments().get(1)
                val invite_code = data.getPathSegments().get(3)
////            Uri uri = Uri.parse("http://biblewith.com/groupfm/" + group_no + "/c/" + invite_code + "?is_invited=true");
////            NavDeepLinkRequest request = NavDeepLinkRequest.Builder.fromUri(uri).build();
////            navController.navigate(request);
////            navController.navigate(R.id.actiongroupfm);
//
                val bd = Bundle()
                bd.putString("group_no", group_no); //getPathSegments: [invite, 1, c, 660998]
                bd.putString("invite_code", invite_code);
                //초대링크를 타고 들어왔냐는 시그널용도 - 모임 홈에서 viewmodel 처리후 이 시그널에 따른 로직을 처리 후(서버에 invite_code 검증 후 멤버추가)
                // 위의 group_no를 이용해 해당 모임으로이동
                bd.putBoolean("is_invited", true);
                findNavController().navigate(R.id.group_fm, bd)
//            binding.mainBottomNav.invalidate();


                //조건에 맞지 않으면 일반적인 앱의 흐름으로서 home_fm 화면으로 간다.
            }/* else {
                Log.e(tagName, "home_fm으로 간다")
                findNavController().navigate(R.id.action_global_home_fm)
            }*/



        } else {
            //LoginActivity 로 보내서 로그인 진행 처리
            Log.e(tagName, "LoginActivity으로 돌아간다")
            val toLogin = Intent(MyApp.getApplication(), LoginActivity::class.java)
            startActivity(toLogin)
            requireActivity().finish()
        }
    }

}