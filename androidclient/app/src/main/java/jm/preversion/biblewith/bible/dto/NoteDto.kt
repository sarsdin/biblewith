package jm.preversion.biblewith.bible.dto

data class NoteDto(
    var note_no: Int,
    var user_no: Int,
    var note_content: String,
    var note_date: String,
    var note_verseL: List<NoteVerseDto>
)
