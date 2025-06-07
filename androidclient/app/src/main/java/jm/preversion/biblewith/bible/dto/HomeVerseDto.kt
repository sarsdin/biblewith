package jm.preversion.biblewith.bible.dto

data class HomeVerseDto(
    var result: VerseResult,
    var msg: String
)

data class VerseResult(
    var bible_no: Int,
    var book: Int,
    var chapter: Int,
    var verse: Int,
    var content: String,
    var book_no: Int,
    var book_name: String,
    var book_category: String
)
