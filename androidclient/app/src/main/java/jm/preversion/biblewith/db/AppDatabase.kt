package jm.preversion.biblewith.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BibleVerse::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bibleDao(): BibleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bible_database" // 데이터베이스 파일명
                )
                    // .createFromAsset("database/bible.db") // 사전 빌드된 DB를 assets에서 복사할 경우
                    // .addCallback(object : Callback() { // 처음 생성 시 데이터 삽입 콜백
                    //     override fun onCreate(db: SupportSQLiteDatabase) {
                    //         super.onCreate(db)
                    //         // 여기서 초기 데이터 삽입 로직 실행 (예: JSON 파일 읽어서 insert)
                    //         // IO 스레드에서 실행되도록 Coroutine 사용 권장
                    //         CoroutineScope(Dispatchers.IO).launch {
                    //             // getDatabase(context).bibleDao().insertAll(loadInitialData(context))
                    //         }
                    //     }
                    // })
                    .fallbackToDestructiveMigration(false) // 마이그레이션 경로가 없을 경우 기존 DB를 삭제하고 새로 생성 (프로덕션에서는 주의)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // 예시: JSON 파일에서 초기 데이터를 로드하는 함수 (실제 구현 필요)
        // private fun loadInitialBibleDataFromJson(context: Context): List<BibleVerse> {
        //// assets/bible_data.json 파일을 읽고 파싱하여 List<BibleVerse> 반환
        //     // Gson 또는 Moshi 라이브러리 사용 가능
        //     // val jsonString = context.assets.open("bible_data.json").bufferedReader().use { it.readText() }
        //     // val gson = Gson()
        //     // val listType = object : TypeToken<List<BibleVerse>>() {}.type
        //     // return gson.fromJson(jsonString, listType)
        //     return emptyList() // 임시 반환
        // }
    }
}