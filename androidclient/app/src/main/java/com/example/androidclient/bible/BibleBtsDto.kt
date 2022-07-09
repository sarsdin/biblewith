package com.example.androidclient.bible

import com.google.gson.annotations.SerializedName

class BibleBtsDto {
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
    var currentColor: Boolean = false  //BibleVerseBtsRva 의 색깔 설정하는 리사이클러뷰에서 현재 선택된 뷰홀더를 나타내는데 쓰일려고 했지만, 지금은 안씀

    constructor()
    constructor(
        bible_no: Int,
        book_category: String,
        book: Int,
        book_name: String,
        chapter: Int,
        verse: Int,
        content: String,
        viewType: Int,
        currentItem: Boolean,
        user_no: Int,
        highlight_date: String,
        highlight_color: Int,
        currentColor: Boolean
    ) {
        this.bible_no = bible_no
        this.book_category = book_category
        this.book = book
        this.book_name = book_name
        this.chapter = chapter
        this.verse = verse
        this.content = content
        this.viewType = viewType
        this.currentItem = currentItem
        this.user_no = user_no
        this.highlight_date = highlight_date
        this.highlight_color = highlight_color
        this.currentColor = currentColor
    }


    init {
//        highlight_color = 순차색()
    }

//    fun 순차색(position:Int)/*: String*/ {
//        val c0 = "#FFFFFF"
//        val c1 = "#FFD3D3"
//        val c2 = "#FFDCFF"
//        val c3 = "#65FFBA"
//        val c4 = "#ACFFEF"
//        val c5 = "#6DD66D"
//        val c6 = "#C8FFFF"
//        val c7 = "#28E7FF"
//        val c8 = "#FF8A19"
//        val c9 = "#FF71F8"
//        var c = arrayOf<String>(c0, c1, c2, c3, c4, c5, c6, c7, c8, c9)
//        this.highlight_color = c[position]
////        return c[position]
//    }


}