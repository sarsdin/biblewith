package jm.preversion.biblewith.login

data class TempLoginDto(var user_no:Int, var user_email:String, var user_pwd:String,
                        var user_autologin:Boolean, var user_nick:String, var user_name:String) {

//    lateinit var user_pwd:String
//    var user_autologin:Boolean = false
//    var user_no:Int = 0
//    var user_nick:String = ""
//    var user_name:String = ""
//
//    constructor(user_email:String, user_pwd:String, user_autologin:Boolean) : this(user_email) {//user_email은 기본 생성자(최상단의)로부터 위임받음.
//        this.user_pwd = user_pwd
//        this.user_autologin = user_autologin
//    }
//
//    ) : this(user_email, user_pwd, user_autologin) //기본 생성자(최상단의)로부터 위임받음.


    //부생성자는 주생성자처럼 변수이자 인자로 작용하지 못하고 인자로만 기능하기때문에 {}안에 this를 이용해서 클래스변수에 맵핑시켜줘야 한다.
//    constructor(user_no:Int, user_email:String, user_pwd:String, user_autologin:Boolean, user_nick:String, user_name:String) : this(user_email){
//        this.user_no = user_no
//        this.user_pwd = user_pwd
//        this.user_autologin = user_autologin
//        this.user_nick = user_nick
//        this.user_name = user_name
//    }


}


//(var user_email:String, var user_pwd:String, var user_autologin:Boolean = false )

//(var user_no:Int?, var user_email:String, var user_pwd:String, var user_autologin:Boolean, var user_nick:String?, var user_name:String?)