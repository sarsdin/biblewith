package jm.preversion.biblewith.group

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import jm.preversion.biblewith.MyApp
import jm.preversion.biblewith.R
import jm.preversion.biblewith.databinding.GroupListFmBinding
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class GroupListFm : Fragment() {
    lateinit var groupVm: GroupVm
    lateinit var rva: GroupListRva
    lateinit var rv: RecyclerView
    var mbinding: GroupListFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
        //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
        // -- 비동기라서 데이터가 미리 로딩이 안됨.. groupVm에서 미리 로딩해놔야함.
        groupVm.모임목록가져오기(MyApp.userInfo.user_no, true)
//                Log.e("[GroupListFm]", "모임목록가져오기 onFailure: " + t.message)

        mbinding = GroupListFmBinding.inflate(inflater, container, false)

        rv = binding.groupList
        rv.layoutManager = GridLayoutManager(context, 2);
        rv.adapter = GroupListRva(groupVm, this)
        rva = rv.adapter as GroupListRva

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //모임만들기 클릭시
        binding.groupListMakeGroupBt.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_group_fm_to_groupCreateFm)
        }

        //모임 정렬클릭 시 메뉴 팝업
        binding.groupListSortGroupBt.setOnClickListener {
            //팝업 메뉴 생성 후 각 메뉴에 대한 xml 파일 인플레이트
            val groupSortPopupMenu = PopupMenu(MyApp.application, it)
            requireActivity().menuInflater.inflate(R.menu.group_sort_menu, groupSortPopupMenu.menu)
            //각 메뉴항목 클릭했을때의 동작 설정
            groupSortPopupMenu.setOnMenuItemClickListener { menuItem ->
                when(menuItem.itemId){
                    //모임이름순 클릭시
                    R.id.group_sort_name_popup -> {
                        //정렬 팝업 클릭시 vm의 정렬상태값을 업데이트하여 상태값 유지
                        groupVm.sortState = "name"
                        binding.groupListSortGroupBt.text = getString(R.string.group_name_sort)
                        groupVm.모임목록가져오기(MyApp.userInfo.user_no, true)
//                        Toast.makeText(this.context, "수정클릭",Toast.LENGTH_SHORT).show()
                        return@setOnMenuItemClickListener true
                    }
                    //멤버 순 클릭시
                    R.id.group_sort_member_count_popup -> {
                        groupVm.sortState = "member"
                        binding.groupListSortGroupBt.text = getString(R.string.group_member_sort)
                        groupVm.모임목록가져오기(MyApp.userInfo.user_no, true)
                        return@setOnMenuItemClickListener true
                    }
                    //게시글 순 클릭시
                    R.id.group_sort_board_count_popup -> {
                        groupVm.sortState = "board"
                        binding.groupListSortGroupBt.text = getString(R.string.group_board_sort)
                        groupVm.모임목록가져오기(MyApp.userInfo.user_no, true)
                        return@setOnMenuItemClickListener true
                    }

                    else -> {
                        return@setOnMenuItemClickListener false
                    }
                }
            }
            //위에서 설정된 팝업 메뉴를 화면(해당뷰)에 띄움
            groupSortPopupMenu.show()
        }

        //정렬 팝업 클릭시 갱신되는 모임순 감시 후 리사이클러뷰 갱신
        groupVm.liveGroupL.observe(viewLifecycleOwner, Observer {
            rva.notifyDataSetChanged()
        })

    }

    override fun onResume() {
        super.onResume()
        binding.groupListSortGroupBt.text = groupVm.sortState.run {
            var sort = if (this == "name") {
                getString(R.string.group_name_sort)
            } else if(this == "member"){
                getString(R.string.group_member_sort)
            } else {
                getString(R.string.group_board_sort)
            }
            return@run sort
        }
        rva.notifyDataSetChanged()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }
}