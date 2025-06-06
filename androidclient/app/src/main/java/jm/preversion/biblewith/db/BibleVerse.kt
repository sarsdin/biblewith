package jm.preversion.biblewith.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bible_verses")
data class BibleVerse(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "book_name") val bookName: String, // 예: 창세기
    @ColumnInfo(name = "book_abbreviation") val bookAbbreviation: String, // 예: 창
    val chapter: Int,
    val verse: Int,
    val text: String
)