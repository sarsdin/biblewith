package jm.preversion.biblewith.moreinfo;

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MoreVm : ViewModel() {

    var userNick = ""
    var liveUserNick = MutableLiveData<String>()

}