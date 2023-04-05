package com.example.androidclient.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.Fragment
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.fragment.findNavController
import com.example.androidclient.MyApp
import com.example.androidclient.R
import com.example.androidclient.login.LoginActivity

class StartFm : Fragment()/*, MainActivity.TestDeep */{

    val tagName = "[StartFm]"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
//        초대링크처리(context as MainActivity)
        초대링크처리((context as MainActivity).intent)
    }



    override fun onAttach(context: Context) {
        super.onAttach(context)
//        초대링크처리((context as MainActivity).intent)
//        val data: Uri? = intent?.data
//
//        if(data?.lastPathSegment == "discover") {
//            val pendingIntent = NavDeepLinkBuilder(this)
//                .setGraph(R.navigation.nav_main)
//                .setDestination(R.id.discover_dest)
//                .createPendingIntent()
//
//            pendingIntent.send()
//        (context as MainActivity).listenerDeepLink = this
    }

//    override fun sendIntentDeep(intent: Intent?, mainActivity: MainActivity) {
//        초대링크처리(intent)
//    }

    fun 초대링크처리(intent: Intent? ){
        //외부에서 모임초대 딥링크를 타고 들어왔을때 처리 - 앱이 꺼져있을때 링크를 실행하면 켜지지만 유저정보가 없는, 즉, 로그인상태가 아니기에
        //다시 LoginActivity 로 보내짐. 거기서 분기점: 자동로그인일때와 아닐때가 나뉘는데, 아니면 그냥 로그인 진행하도록하면되고, 자동로그인이면
        //로그인되어 다시 이곳으로 보내짐. 이때 딥링크로 전달되었던 intent의 정보는 없어진 상태라 intent.data & action의 값은 null이됨.
        //유저정보(로그인)는 있지만 data가 null인 상태가 되어서 home_fm으로 가게됨 == 앱이 꺼진상태라면 로직상 최종적으로 잘가봤자 home_fm 화면까지임.
        //여기서 자동로그인기능까지 염두에 두고 초대장을 보내고 싶다면 LoginActivity로 가는 시점에 수동 intent의 data 정보를 넣어주면됨.
        //그리고, 자동로그인시 다시 그 정보를 intent에 넣어서 이곳으로 보내오고 여기서 로그인정보 + 딥링크초대정보까지 모두 가진 상태로 진행하면 초대장을
        //받을 수 있음!

//        val intent = mainActivity.intent
        val action = intent?.action
        val data = intent?.data
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
            if (data != null && data.getPathSegments().get(0) == "invite") {
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
            } else {
                Log.e(tagName, "home_fm으로 간다")
                findNavController().navigate(R.id.action_global_home_fm)
            }

        //LoginActivity 로 보내서 로그인 진행 처리
        } else {
            Log.e(tagName, "LoginActivity으로 돌아간다")
            val toLogin = Intent(MyApp.getApplication(), LoginActivity::class.java)
            startActivity(toLogin)
            requireActivity().finish()
        }
    }

    override fun onStop() {
        super.onStop()
//        findNavController().popBackStack()
//        findNavController().clearBackStack(R.id.startFm)
    }


}