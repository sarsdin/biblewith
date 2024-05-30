package jm.preversion.biblewith.group.member

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.RecyclerView
import jm.preversion.biblewith.MyApp
import jm.preversion.biblewith.R
import jm.preversion.biblewith.bible.BibleVm
import jm.preversion.biblewith.databinding.GroupInInviteFmBinding
import jm.preversion.biblewith.group.GroupVm
import jm.preversion.biblewith.util.Http
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class GroupInInviteFm : Fragment() {
    val tagName = "[GroupInInviteFm]"
    lateinit var groupVm: GroupVm
    lateinit var bibleVm: BibleVm
    lateinit var rva: GroupInMemberRva
    lateinit var rv: RecyclerView
    var mbinding: GroupInInviteFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mbinding = GroupInInviteFmBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = Navigation.findNavController(view)
//        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val appBarConfiguration = AppBarConfiguration.Builder(R.id.group_fm).build()
        NavigationUI.setupWithNavController(binding.toolbar, navController, appBarConfiguration)





    }


    override fun onResume() {
        super.onResume()
        binding.groupNameTv.setText(groupVm.groupInfo.get("group_name").asString) //모임 이름 넣기ㅡ

        val retrofit = Http.getRetrofitInstance(Http.HOST_IP)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)

        val jo = JsonObject()
        jo.addProperty("group_no", groupVm.groupInfo.get("group_no").asString)
//        jo.addProperty("code_type", "number")

        val call = httpGroup.모임초대번호있는지확인(jo)
        call.enqueue(object : Callback<JsonObject?> {
            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                if (response.isSuccessful) {
                    val res = response.body()!!
                    if(res.get("result").isJsonPrimitive){ //result == 'expired' or 'nocreated'임
                        //expired 만료되거나, 비밀번호 생성되지 않았음(nocreated). << 밑의 클릭 이벤트를 걸어주어 초기 생성 가능하게 만들어야함
                        //초대 비밀번호 공유 카드뷰 클릭시
                        binding.numberCard.setOnClickListener {
                            //재생성 요청이면 1, 초기 생성 요청이면 0 << 초기생성이면 백엔드에서 기존 number 레코드 delete할 필요없음
                            jo.addProperty("is_recreate", 0)
                            val call = httpGroup.모임초대번호재생성요청(jo)
                            call.enqueue(object : Callback<JsonObject?> {
                                override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                    if (response.isSuccessful) {
                                        val res = response.body()!!
                                        val resObj = res.get("result").asJsonObject
                                        binding.expireDate.text = "유효기간: ${MyApp.getTime("ui3", resObj.get("expire_date").asString)} 남음"
                                        binding.sharedNumber.text = resObj.get("invite_code").asString //새로받은 코드 갱신 넣어주기
                                        binding.numberExpireCard.visibility = View.VISIBLE

                                        Log.e(tagName, "모임초대번호재생성요청 onResponse: $res")
                                    }
                                }
                                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                    Log.e(tagName, "모임초대번호재생성요청 onFailure: " + t.message)
                                }
                            })
                        }

                    } else {
                        //만료안되고 기간 남아있음 - 그 비밀번호를 가져오고 ui 표시해줌
                        binding.numberExpireCard.visibility = View.VISIBLE
                        val resObj = res.get("result").asJsonObject
                        binding.expireDate.text = "유효기간: ${MyApp.getTime("ui3", resObj.get("expire_date").asString)} 남음"
                        binding.sharedNumber.text = resObj.get("invite_code").asString
                    }

                    Log.e(tagName, "모임초대번호있는지확인 onResponse: $res")
                }
            }
            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                Log.e(tagName, "모임초대번호있는지확인 onFailure: " + t.message)
            }
        })

        //초대 공유 비밀번호 재생성 클릭시
        binding.recreateBt.setOnClickListener {
            //재생성 요청이면 1, 초기 생성 요청이면 0 << 초기생성이면 백엔드에서 기존 number 레코드 delete할 필요없음
            jo.addProperty("is_recreate", 1)
            val call = httpGroup.모임초대번호재생성요청(jo)
            call.enqueue(object : Callback<JsonObject?> {
                override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                    if (response.isSuccessful) {
                        val res = response.body()!!
                        val resObj = res.get("result").asJsonObject
                        binding.expireDate.text = "유효기간: ${MyApp.getTime("ui3", resObj.get("expire_date").asString)} 남음"
                        binding.sharedNumber.text = resObj.get("invite_code").asString //새로받은 코드 갱신 넣어주기

                        Log.e(tagName, "모임초대번호재생성요청 onResponse: $res")
                    }
                }
                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    Log.e(tagName, "모임초대번호재생성요청 onFailure: " + t.message)
                }
            })
        }


        //초대링크 공유 카드뷰 클릭시
        binding.linkCard.setOnClickListener {
            // 서버에서 code_type이 number일경우와 link일 경우 구분 - number일경우에는 그 코드 재활용하여 가져와 ui에 보여준다.(몇시간 남았는지 또는 몇시까지인지)
            // 랜덤 숫자 6자리 만들기 - 서버에 저장하기 - 저장할때 시간설정+12시간 비교하기

            //초대링크 생성
            val call = httpGroup.모임초대링크생성(jo)
            call.enqueue(object : Callback<JsonObject?> {
                override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                    if (response.isSuccessful) {
                        val res = response.body()!!
                        val resObj = res.get("result").asJsonObject
//                        binding.expireDate.text = "유효기간: ${MyApp.getTime("ui3", resObj.get("expire_date").asString)} 남음"
//                        binding.sharedNumber.text = resObj.get("invite_code").asString //새로받은 코드 갱신 넣어주기

                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            val value = "http://biblewith.com/invite/${groupVm.groupInfo.get("group_no").asString}" +
                                    "/c/${resObj.get("invite_code").asString}"
                            putExtra(Intent.EXTRA_TEXT, value)
                            type = "text/plain"
                        }

                        val shareIntent = Intent.createChooser(sendIntent, "성경with 모임 멤버 초대링크 공유")
                        startActivity(shareIntent)

                        Log.e(tagName, "모임초대링크생성 onResponse: $res")
                    }
                }
                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    Log.e(tagName, "모임초대링크생성 onFailure: " + t.message)
                }
            })


        }


        //클립보드에 번호 저장
        binding.sharedNumber.setOnClickListener {
            clipBoard()
            Toast.makeText(requireActivity(),"초대번호 복사",Toast.LENGTH_SHORT).show()
        }


    }

    fun clipBoard() {
        val clipboardManager: ClipboardManager? = requireActivity().getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
        val clipData = ClipData.newPlainText("ID", binding.sharedNumber.text) //클립보드에 ID라는 이름표로 id 값을 복사하여 저장
        clipboardManager?.setPrimaryClip(clipData)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }











}