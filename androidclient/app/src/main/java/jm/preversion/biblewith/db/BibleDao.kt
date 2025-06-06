package jm.preversion.biblewith.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BibleDao {
    @Query("SELECT * FROM bible_verses")
    fun getAll(): Flow<List<BibleVerse>> // Flow를 사용하여 비동기 데이터 스트림 제공

    @Query("SELECT * FROM bible_verses WHERE book_name = :bookName AND chapter = :chapter")
    fun getVersesByChapter(bookName: String, chapter: Int): Flow<List<BibleVerse>>

    @Query("SELECT * FROM bible_verses WHERE book_name = :bookName AND chapter = :chapter AND verse = :verse")
    fun getVerse(bookName: String, chapter: Int, verse: Int): Flow<BibleVerse?>

    @Query("SELECT* FROM bible_verses WHERE text LIKE '%' || :keyword || '%'")
    fun searchVerses(keyword: String): Flow<List<BibleVerse>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(verses: List<BibleVerse>) // suspend 함수로 비동기 처리

    // 필요한 다른 쿼리 메서드들 추가 (예: 책 목록 가져오기, 장 수 가져오기 등)
    @Query("SELECT DISTINCT book_name FROM bible_verses ORDER BY id") // 책 순서대로
    fun getAllBookNames(): Flow<List<String>>

    @Query("SELECT MAX(chapter) FROM bible_verses WHERE book_name = :bookName")
    fun getLastChapterOfBook(bookName: String): Flow<Int?>
}