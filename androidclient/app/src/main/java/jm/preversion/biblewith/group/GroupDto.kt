package jm.preversion.biblewith.group

data class GroupDto (
    //모임 테이블용
    var group_no:Int,
    var chat_room_no: Int,
    var user_no: Int,
    var group_name:String,
    var group_desc: String,
    var group_main_image: String,

    //모임 게시판 테이블용
    var gboard_no: Int,
    var gboard_title: String,
    var gboard_content: String,
    var create_date: String,

    /*비필수 정보(Ui용) - 서버에서 채워지지 않은 정보 - null이거나 빈값 또는 초기값*/
    var viewType: Int,
    var currentItem: Boolean

    ) {

        //깊은복사위한 2차생성자
    /*    constructor( se: BibleDto) : this(bible_no = se.bible_no,
            book_category = se.book_category?:"",  //?:""   값으로 null 이 들어오면 디폴트로 "" 넣음
            book = se.book,
            book_name = se.book_name?:"",
            chapter = se.chapter,
            verse = se.verse,
            content = se.content?:"",
            user_no = se.user_no,
            highlight_date = se.highlight_date?:"",
            highlight_color = se.highlight_color,
            viewType = se.viewType,
            currentItem = se.currentItem,
            highlight_selected = se.highlight_selected
        )*/


}

