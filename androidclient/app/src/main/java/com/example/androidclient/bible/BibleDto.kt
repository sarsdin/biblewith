package com.example.androidclient.bible

import com.google.gson.annotations.SerializedName

data class BibleDto(var bible_no:Int, var book_category: String, @SerializedName("book_no") var book: Int,
                     var book_name:String, var chapter: Int, var verse: Int, var content: String,
                     var viewType: Int, var currentItem: Boolean,
                     var user_no: Int, var highlight_date: String, var highlight_color: String
                     ) {


}