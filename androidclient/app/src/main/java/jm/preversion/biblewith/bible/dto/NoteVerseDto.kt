package jm.preversion.biblewith.bible.dto

data class NoteVerseDto(
    var note_verse_no: Int?,
    var note_no: Int?,
    var bible_no: Int?,
    var book: Int,
    var chapter: Int,
    var verse: Int,
    var content: String
)
