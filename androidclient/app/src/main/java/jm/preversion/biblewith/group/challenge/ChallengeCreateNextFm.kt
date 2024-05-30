package jm.preversion.biblewith.group.challenge

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.RecyclerView
import jm.preversion.biblewith.MyApp
import jm.preversion.biblewith.R
import jm.preversion.biblewith.databinding.ChallengeCreateNextFmBinding
import jm.preversion.biblewith.group.GroupVm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.ceil


class ChallengeCreateNextFm : Fragment() {
    lateinit var groupVm: GroupVm
//    lateinit var rva: ChallengeCreateFmRva
    lateinit var rv: RecyclerView
    var mbinding: ChallengeCreateNextFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
        mbinding = ChallengeCreateNextFmBinding.inflate(inflater, container, false)


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = Navigation.findNavController(view)
//        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val appBarConfiguration = AppBarConfiguration.Builder(R.id.group_fm).build()
        NavigationUI.setupWithNavController(
            binding.createNextToolbar,
            navController,
            appBarConfiguration
        )



        //분량으로 계산 버튼 클릭시
        binding.verseBt.setOnClickListener {
//            binding.verseBt.isCheckable = true
//            binding.dayBt.isCheckable = false
            groupVm.whatIsSelected = "verse"
            binding.verseGroup.visibility = View.VISIBLE
            binding.dayGroup.visibility = View.GONE
            binding.nextBt.isEnabled = true
            binding.createNextToolbarAddBt.visibility = View.VISIBLE
            binding.verseBt.setStrokeColorResource(R.color.color_green2)
            binding.dayBt.setStrokeColorResource(R.color.color_gray2)
            binding.verseBt.setTextColor(R.color.color_green2) //적용안됨 xml 따로만들어 selector 태그 써야할듯
            binding.dayBt.setTextColor(ColorStateList.valueOf(R.color.color_gray2))
        }
        //일수로 계산 버튼 클릭시
        binding.dayBt.setOnClickListener {
//            binding.dayBt.isCheckable = true
//            binding.verseBt.isCheckable = fals
            groupVm.whatIsSelected = "day"
            binding.verseGroup.visibility = View.GONE
            binding.dayGroup.visibility = View.VISIBLE
            binding.nextBt.isEnabled = true
            binding.createNextToolbarAddBt.visibility = View.VISIBLE
            binding.verseBt.setStrokeColorResource(R.color.color_gray2)
            binding.dayBt.setStrokeColorResource(R.color.color_green2)
            binding.verseBt.setTextColor(ColorStateList.valueOf(R.color.color_gray2))
            binding.dayBt.setTextColor(ColorStateList.valueOf(R.color.color_green2))
        }



        //분량계산
        binding.seekBarVerse.max = 300
        binding.seekBarVerse.min = 20
        binding.seekBarVerse.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                //todo 목록 전체의 구절 수 구하기(서버통신) -
                // 전체구절수 / 절수  = 필요일수           를 하여 나오는 일수를 오늘날짜를 기준으로 몇월몇일 완료되는지 계산하기
                // 전체구절수 / 일수  = 하루진행할 절 수     오늘 기준으로 씩바의 수치를 +하여 몇월몇일 완료되는지 계산

                groupVm.progressCountVerse = progress
                val t = LocalDateTime.now()
                val t1 = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                //총절수를 seekBar 조정시 변하는 수치로 나누고(몇일걸리는지에대한일수) 현재 날짜에서 plus 하여 그날이 몇일인지 구함
                val t2 = (t.plusDays(ceil(groupVm.totalVerseCount.toDouble() / groupVm.progressCountVerse.toDouble()).toLong())).format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                groupVm.computedDay = ceil(groupVm.totalVerseCount.toDouble() / groupVm.progressCountVerse.toDouble()).toInt()

                binding.selectNumberTv.text = "${groupVm.progressCountVerse}절씩 읽으면"
                binding.selectDurationTv.text = "${t1} ~ ${t2}일 완료예정"
                binding.selectTotalTv.text = "선택한 성경은 총 ${groupVm.totalVerseCount}절 입니다."
                binding.selectExpectTv.text =
                    "하루에 약 ${groupVm.progressCountVerse}절씩 ${ceil(groupVm.totalVerseCount.toDouble() / groupVm.progressCountVerse.toDouble()).toInt()}일" //필요일수계산

            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        //일수계산
        binding.seekBarDay.max = 120
        binding.seekBarDay.min = 3
        binding.seekBarDay.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                groupVm.progressCountDay = progress
                val t = LocalDateTime.now()
                val t1 = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                //seekBar 조정시 변하는 수치를 현재 날짜에서 plus 하여 그날이 몇일인지 구함
                val t2 = (t.plusDays(groupVm.progressCountDay.toLong())).format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
//                groupVm.dDay = t2

                binding.createDayNumberTv.text = "${groupVm.progressCountDay}일간 읽으면"
                binding.createDayDurationTv.text = "${t1} ~ ${t2}일 완료예정"
                binding.createDayTotalVerseTv.text = "선택한 성경은 총 ${groupVm.totalVerseCount}절 입니다."
                binding.createDayExpectTv.text =
                    "하루에 약 ${ceil(groupVm.totalVerseCount.toDouble() / groupVm.progressCountDay.toDouble()).toInt()}절씩 ${groupVm.progressCountDay}일"//필요절수계산

            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })


        //완료하기 버튼 클릭시 - 모든 지정된 정보들(계산방식:계산된결과, 선택된 성경목록, 사용자번호, 모임번호, 필요일수) 서버로 보내 챌린지 만들기
        binding.nextBt.setOnClickListener {
            val jo = groupVm.createJo  //createfm에서 chal_title 받아놓음
            jo.addProperty("whatIsSelected", groupVm.whatIsSelected)
            jo.addProperty("user_no", MyApp.userInfo.user_no)
            jo.addProperty("group_no", groupVm.groupInfo.get("group_no").asString)
            if(groupVm.whatIsSelected == "day"){
                jo.addProperty("computedDay", groupVm.progressCountDay)        //seekBar로 선택된 완료예정일수
            } else {
                jo.addProperty("computedDay", groupVm.computedDay)        //계산된 완료예정일수
            }
            jo.add("selectedCreateList", groupVm.selectedCreateList)
            CoroutineScope(Dispatchers.Main).launch {
                binding.progressBar.visibility = View.VISIBLE
                groupVm.챌린지만들기완료하기(jo, true)
                groupVm.챌린지목록가져오기(true)
                binding.progressBar.visibility = View.GONE
                findNavController().navigate(R.id.action_challengeCreateNextFm_to_group_in_challenge_fm)
            }
        }



    }

    override fun onResume() {
        super.onResume()
        //선택한 목록 표시
        val st = StringBuilder()
        groupVm.selectedCreateList.forEachIndexed { i, jo ->
            st.append("${i+1}.")
            st.append(jo.asJsonObject.get("book_name").asString)
            if(groupVm.selectedCreateList.size() != i+1){ //요소의 인덱스가 마지막이 아니라면 ' ' 추가
                st.append(" ")
            }
        }
        binding.selectListTv.text = "선택한 목록: $st "

        //seekBar 수치를 날짜포맷으로 변환
        val t = LocalDateTime.now()
        val t1 = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
        //seekBar 조정시 변하는 수치를 현재 날짜에서 plus 하여 그날이 몇일인지 구함
        val t2 = (t.plusDays(groupVm.progressCountDay.toLong())).format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
//        Log.e("오류태그", "${LocalDateTime.now()}")
//        Log.e("오류태그", "${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))}")
//        Log.e("오류태그", "${t1}")
//        Log.e("오류태그", "${t2}")

        //분량계산 ui정보
        binding.selectNumberTv.text = "${groupVm.progressCountVerse}절씩 읽으면"
        binding.selectDurationTv.text = "${t1} ~ ${t2}일 완료예정"
        binding.selectTotalTv.text = "선택한 성경은 총 ${groupVm.totalVerseCount}절 입니다." //총 절 표시
        binding.selectExpectTv.text =
            if(groupVm.progressCountVerse != 0){
                //seekBar로 설정한 수치를 적용계산하여 완료예정일을 구한다
                "하루에 약 ${groupVm.progressCountVerse}절씩 ${ceil(groupVm.totalVerseCount.toDouble() / groupVm.progressCountVerse.toDouble()).toInt()}일" //필요일수계산
            } else {
                "하루에 약 ${groupVm.progressCountVerse}절씩 ${groupVm.totalVerseCount / 1}일" //필요일수계산
            }


        //일수계산 ui정보
        binding.createDayNumberTv.text = "${groupVm.progressCountDay}일간 읽으면"
        binding.createDayDurationTv.text = "${t1} ~ ${t2}일 완료예정"
        binding.createDayTotalVerseTv.text = "선택한 성경은 총 ${groupVm.totalVerseCount}절 입니다." //총 절 표시
        binding.createDayExpectTv.text =
            if(groupVm.progressCountDay != 0){
                "하루에 약 ${ceil(groupVm.totalVerseCount.toDouble() / groupVm.progressCountDay.toDouble()).toInt()}절씩 ${groupVm.progressCountDay}일"//필요절수계산
            } else {
                "하루에 약 ${groupVm.totalVerseCount / 1}절씩 ${groupVm.progressCountDay}일"//필요절수계산
            }
    }




    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }
}