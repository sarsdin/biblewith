package com.example.androidclient.bible

import android.graphics.Color
import com.google.gson.annotations.SerializedName
import kotlin.text.StringBuilder

data class BibleDto (var bible_no:Int, var book_category: String, /*@SerializedName("book_no")*/ var book: Int, //@SerializedName("book_no")은 json으로 넘어오는 정보에 book_no라는 속성을 book에 맵핑하겠다는 뜻
                     var book_name:String, var chapter: Int, var verse: Int, var content: String,
                     var user_no: Int, var highlight_date: String, var highlight_color: Int,

                     //비필수 정보(Ui용) - 서버에서 채워지지 않은 정보 - null이거나 빈값 또는 초기값
                     var viewType: Int, var currentItem: Boolean,
                     var highlight_selected: Boolean = false //일단 기본적으로 false. 절화면에서 선택되었을때 true로 바꿔야함
                     
                     /*var note_no: Int, var note_content: String, var note*/
                     
                     ) {

/*
    var selected_verseList: List<BibleDto>

    constructor( bible_no:Int,  book_category: String,   book: Int,
                 book_name:String,  chapter: Int,  verse: Int,  content: String,
                 viewType: Int,  currentItem: Boolean,
                 user_no: Int,  highlight_date: String,  highlight_color: Int,

                 selected_verseList: List<BibleDto>

                 ) : this(bible_no,  book_category,   book,
        book_name,  chapter,  verse,  content,
        viewType,  currentItem,
        user_no,  highlight_date,  highlight_color,

       ) {

    }


    init {
        var bible_no:Int = 0
        lateinit var book_category: String
//        @SerializedName("book_no")
        var book: Int = 0
        lateinit var book_name:String
        var chapter: Int = 0
        var verse: Int = 0
        lateinit var content: String
        var viewType: Int = 0
        var currentItem: Boolean = false
        var user_no: Int = 0
        lateinit var highlight_date: String
        var highlight_color: Int = 0
        this.selected_verseList = mutableListOf()
    }*/

/*
    fun 절목록(): String{
        var sb = StringBuilder()
        selected_verseList.forEach {
            sb.append("${it.verse} ")
        }
        return sb.toString()
    }
*/


    fun 순차색(position:Int)/*: String*/ {
        val c0 = "#FFFFFF"
        val c1 = "#FFD3D3"
        val c2 = "#FFDCFF"
        val c3 = "#65FFBA"
        val c4 = "#ACFFEF"
        val c5 = "#6DD66D"
        val c6 = "#C8FFFF"
        val c7 = "#28E7FF"
        val c8 = "#FF8A19"
        val c9 = "#FF71F8"
        var c = arrayOf<String>(c0, c1, c2, c3, c4, c5, c6, c7, c8, c9)
        this.highlight_color = Color.parseColor(c[position])
//        return c[position]
    }


}

/*

var bible_no:Int, var book_category: String, @SerializedName("book_no") var book: Int,
var book_name:String, var chapter: Int, var verse: Int, var content: String,
var viewType: Int, var currentItem: Boolean,
var user_no: Int, var highlight_date: String, var highlight_color: Int,

var selected_verseList: List<BibleDto>*/


/*

var bible_no:Int = 0
lateinit var book_category: String
@SerializedName("book_no")
var book: Int = 0
lateinit var book_name:String
var chapter: Int = 0
var verse: Int = 0
lateinit var content: String
var viewType: Int = 0
var currentItem: Boolean = false
var user_no: Int = 0
lateinit var highlight_date: String
var highlight_color: Int = 0

var selected_verseList: List<BibleDto>

constructor( bible_no:Int,  book_category: String,   book: Int,
book_name:String,  chapter: Int,  verse: Int,  content: String,
viewType: Int,  currentItem: Boolean,
user_no: Int,  highlight_date: String,  highlight_color: Int,

selected_verseList: List<BibleDto>)  {

}*/
