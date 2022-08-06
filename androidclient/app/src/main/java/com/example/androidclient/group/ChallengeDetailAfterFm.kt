package com.example.androidclient.group

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.MyApp
import com.example.androidclient.R
import com.example.androidclient.databinding.ChallengeDetailAfterFmBinding
import com.example.androidclient.util.ImageHelper
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.internal.waitMillis


class ChallengeDetailAfterFm : Fragment() {
    lateinit var groupVm: GroupVm
    lateinit var rva: ChallengeDetailAfterRva
    lateinit var rv: RecyclerView
    var mbinding: ChallengeDetailAfterFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언


    private val playbackStateListener: Player.Listener = playbackStateListener()
    private var player: ExoPlayer? = null

    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L

    lateinit var buttonGroupListener:MaterialButtonToggleGroup.OnButtonCheckedListener


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
        mbinding = ChallengeDetailAfterFmBinding.inflate(inflater, container, false)
        initializePlayer()

        rv = binding.verseList
        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = ChallengeDetailAfterRva(groupVm, this)
        rva = rv.adapter as ChallengeDetailAfterRva

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = Navigation.findNavController(view)
//        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val appBarConfiguration = AppBarConfiguration.Builder(R.id.group_fm).build()
        NavigationUI.setupWithNavController( binding.toolbar, navController, appBarConfiguration)

        initializePlayer() //플레이어 초기화

        buttonGroupListener = MaterialButtonToggleGroup.OnButtonCheckedListener { group, checkedId, isChecked ->
            val bt = group.findViewById<MaterialButton>(checkedId) // 클릭된 버튼 idRes 를 이용해 찾는다.
//            Toast.makeText(requireActivity(),"$checkedId",Toast.LENGTH_SHORT).show()
//            Log.e("오류태그", "$checkedId")
            val jo = JsonObject()
            jo.addProperty("chal_detail_no", groupVm.chalDetailInfo.get("chal_detail_no").asInt)
            jo.addProperty("user_no", MyApp.userInfo.user_no)
            //클릭한 버튼이 체크가 되어있으면 아뒤를 넣고, 아니면 db에 아이디를 초기화함 - 옵져버에서 ui갱신처리시 아이디가 버튼과 일치하지 않으면
            //표시처리하지 않기 때문 - 체크된걸 체크풀면 좋아요 표시 안한거기 때문에 거기까지 고려.
            if(bt.isChecked){
                jo.addProperty("like_bt_no", bt.id)
            } else {
                jo.addProperty("like_bt_no", 0)
            }
            jo.addProperty("is_checked", bt.isChecked)

            //체크상태가 true 일때만 전송함 - 이 리스너이벤트는 체크상태가 켜지든 꺼지든 둘다 호출되기 때문에 다른 버튼 클릭하면
            //무조건 2번 실행됨 - 거기에 옵저버에서 버튼의 isChecked 값을 바꾸면 ... 난장판이 되버림. 그러니 주의하자!

            if(bt.isChecked){
                CoroutineScope(Dispatchers.IO).launch {
                    Thread.sleep(100)
//                    this.waitMillis()
                    groupVm.챌린지상세좋아요클릭(jo,true)
                }

            //todo 문제상황 : 다른버튼 클릭했을때 둘다 체크상황이 변하니깐 무조건 2번 호출됨. 이런 상황에서 밑의 else문이 (a버튼일경우)
            //todo  버튼의 체크상황이 (true -> false)로 변할때 코루틴이 실행되고, b버튼에서 다시 false -> true 로 변할때 위의
            //todo 코루틴이 실행됨. 그럼, 결국 서로다른 별개의 코루틴의 비동기가 어느것이 빨리 완료되어 도착하느냐? 에 따라 ui적용(likeInfoLoad)이 달라지게 됨.
            //todo 문제는 여기에 있음. 둘중하나는 is_checked == true or false 를 담고 있는데 먼저 도착하는 순에 따라 켜져보이고 꺼저보이에 되는 것..
            //todo 다시 짜야 되나? - false인경우 밑의 else if문이 먼저 실행됨. (같은것이 클릭된 경우 실행안됨:else의 경우인데 else는 없으니,
            // 그리고 else if의 경우 일찍실행되고 일찍 적용되는 경우에는 어차피 위의 코루틴이 나중에 적용되어 결과값이 제대로 적용되어지게 되어 상관없지만,아닌경우
            // 일찍출발했지만 늦게 도착하는 경우 위의 코루틴보다 나중에 적용되어 제대로된 결과값이 적용안되는 결과가 도출되버림. 그렇기에 위의 코루틴이 맨나중에
            // 적용되게끔 sleep을 약간 걸어주면 밑의 코루틴 적용 여부와 상관없이 나중에 도착하게 되어 제대로된 결과 ui가 적용됨!)

            }  //체크가 true가 아닐때는 현재 likeMyInfo가 있는지 확인하고(null아님),
                // db에 저장된 체크한 like_bt_no id의 값이 현재 선택한 bt id의 값과 일치한다면(=현재선택된 버튼을 재클릭했다면)
                // db에 is_checked 값을 업데이트시켜서 선택취소를 시켜야 옵저버에서 받은 값으로 ui를 갱신할 수 있다.
            else if (!groupVm.chalDetailInfo.get("likeMyInfo").isJsonNull){
                if(bt.id == groupVm.chalDetailInfo.get("likeMyInfo").asJsonObject.get("like_bt_no").asInt) {
                    CoroutineScope(Dispatchers.IO).launch {
                        groupVm.챌린지상세좋아요클릭(jo,true)
                    }
                }
            }
        }
        binding.toggleGroup.addOnButtonCheckedListener(buttonGroupListener) //위의 리스너를 버튼그룹에 추가함 - onstop에서 removelistener해주는것 잊지말기!

//        likeInfoLoad(groupVm.chalDetailInfo) -- 뷰홀더 클릭시 챌린지인증진행하기 메소드로 오는 정보를 옵저버에서 받아 실행하기때문에 따로 초기화 할 필요없다
        //좋아요 버튼 클릭시 - 서버로부터 갱신되는 정보를 감지시 전체 좋아요 버튼 ui 갱신작업
        groupVm.liveChalDetailInfo.observe(viewLifecycleOwner, Observer {
            likeInfoLoad(it)
        })

    }

    fun likeInfoLoad(it : JsonObject){
        if(!it.get("likeMyInfo").isJsonNull){ // likeMyInfo 객체가 null 이 아니라면 실행
            val likeMyInfo = it.get("likeMyInfo").asJsonObject

            for (i in 0 until 5){
                val bt = (binding.toggleGroup.get(i) as MaterialButton)
                //각 버튼의 id 번호가 같은 번호를 찾아서 체크여부을 가림 - 체크되있다면 체크되어있는 버튼 ui 변경
                if(bt.id == likeMyInfo.get("like_bt_no").asInt && likeMyInfo.get("is_checked").asInt == 1 ){
                    bt.setTextColor(Color.parseColor("#B71C1C"))
                    bt.textSize = 18f
//                    bt.isChecked = true

                } else {
                    bt.setTextColor(Color.parseColor("#212121"))
                    bt.textSize = 14f
//                    bt.isChecked = false
                }
                //좋아요 정보에서 각 버튼 id와 동일한 id를 가진 요소들의 사이즈를 구하여 해당하는 버튼순서에따라 각각의 이모티콘과 사이즈 넣어줌
                val size = it.get("likeInfo").asJsonArray.filter { it2 ->
                    (it2 as JsonObject).get("like_bt_no").asInt == bt.id
                }.size

                if(i == 0){
                    bt.text = "\uD83D\uDE06${size}"
                } else if (i == 1){
                    bt.text = "\uD83D\uDC4D${size}"
                }else if (i == 2){
                    bt.text = "❤${size}"
                }else if (i == 3){
                    bt.text = "\uD83D\uDE2E${size}"
                }else if (i == 4){
                    bt.text = "\uD83D\uDE0D${size}"
                }

            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.toolbarTv.text = arguments?.get("verseScope").toString().substring(8)
//        hideSystemUi()
//        initializePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer() //플레이어 자원 해제
        binding.toggleGroup.removeOnButtonCheckedListener(buttonGroupListener)
//        binding.toggleGroup.clearOnButtonCheckedListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }


    private fun initializePlayer() {
        val trackSelector = DefaultTrackSelector(requireActivity()).apply {
            setParameters(buildUponParameters().setMaxVideoSizeSd())
        }
        player = ExoPlayer.Builder(requireActivity())
            .setTrackSelector(trackSelector)
            .build()
            .also { exoPlayer ->
                binding.videoView.player = exoPlayer

                //서버 uri 를 이용해 dash 영상을 불러온다
                val uri = ImageHelper.UPLOADS_URL + groupVm.chalDetailVideoInfo.get("for_streaming_file_name").asString
                val mediaItem = MediaItem.Builder()
                    .setUri(uri ) // getString(R.string.media_url_dash2)
                    .setMimeType(MimeTypes.APPLICATION_MPD)
                    .build()
//                val mediaItem = MediaItem.fromUri(getString(R.string.media_url_mp4))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.playWhenReady = playWhenReady     //준비되면 자동 재생 여부 설정
                exoPlayer.seekTo(currentItem, playbackPosition) //탐색옵션
                exoPlayer.addListener(playbackStateListener)  //플레이어 각단계 리스너
                exoPlayer.prepare()
            }
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            currentItem = exoPlayer.currentMediaItemIndex
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.removeListener(playbackStateListener)
            exoPlayer.release()
        }
        player = null
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
        WindowInsetsControllerCompat(requireActivity().window, binding.videoView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars()) //시스템 상단바 숨김
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE //스와이프하면 잠깐 나옴
        }
    }

    private fun playbackStateListener() = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateString: String = when (playbackState) {
                ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
                ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
                ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY     -"
                ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
                else -> "UNKNOWN_STATE             -"
            }
            Log.d("ExoPlayer", "changed state to $stateString")
        }
    }


}

