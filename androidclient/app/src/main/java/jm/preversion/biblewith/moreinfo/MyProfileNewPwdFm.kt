package jm.preversion.biblewith.moreinfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import jm.preversion.biblewith.R
import jm.preversion.biblewith.databinding.MyProfileNewPwdBinding
import jm.preversion.biblewith.group.GroupVm
import jm.preversion.biblewith.MainActivity

class MyProfileNewPwdFm : Fragment() {
    lateinit var groupVm: GroupVm
    var mbinding: MyProfileNewPwdBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mbinding = MyProfileNewPwdBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //모임 툴바 셋팅
        val navController = Navigation.findNavController(view)
//        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val appBarConfiguration = AppBarConfiguration.Builder(R.id.more_fm).build()
//        binding.groupMainCollapsingToolbar.setupWithNavController(binding.groupMainToolbar, navController, appBarConfiguration)
//        binding.groupInToolbar.setupWithNavController(navController, appBarConfiguration)
        NavigationUI.setupWithNavController( binding.toolbar, navController, appBarConfiguration )



    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
        (requireActivity() as MainActivity).binding.mainBottomNav.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}