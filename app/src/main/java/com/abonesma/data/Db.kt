package com.abonesma.data
import android.content.Context
import androidx.room.*
@Entity data class BookE(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val path: String, val hash: String = "")
@Entity data class ChapterE(@PrimaryKey(autoGenerate = true) val id: Long = 0, val bookId: Long, val title: String, val pos: Int)
@Entity data class LessonE(@PrimaryKey(autoGenerate = true) val id: Long = 0, val chapterId: Long, val title: String, val text: String, val pos: Int)
@Entity data class AttemptE(@PrimaryKey(autoGenerate = true) val id: Long = 0, val chapterId: Long, val score: Int, val total: Int, val at: Long)
@Entity data class WrongE(@PrimaryKey(autoGenerate = true) val id: Long = 0, val chapterId: Long, val q: String, val options: String,
                          val chosen: Int, val correct: Int, val source: String, val explain: String)
@Entity data class CardStateE(@PrimaryKey val key: String, val mastery: Int, val review: Boolean)
@Dao abstract class Dao {
    @Insert abstract suspend fun book(b: BookE): Long
    @Insert abstract suspend fun chapter(c: ChapterE): Long
    @Insert abstract suspend fun lesson(l: LessonE): Long
    @Insert abstract suspend fun attempt(a: AttemptE): Long
    @Insert abstract suspend fun addWrong(w: WrongE): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putCard(c: CardStateE)
    @Query("SELECT * FROM BookE ORDER BY id DESC") abstract suspend fun books(): List<BookE>
    @Query("SELECT * FROM BookE WHERE hash=:h LIMIT 1") abstract suspend fun bookByHash(h: String): BookE?
    @Query("SELECT * FROM ChapterE WHERE bookId=:b ORDER BY pos") abstract suspend fun chapters(b: Long): List<ChapterE>
    @Query("SELECT * FROM LessonE WHERE chapterId=:c ORDER BY pos") abstract suspend fun lessons(c: Long): List<LessonE>
    @Query("SELECT * FROM LessonE WHERE id=:id") abstract suspend fun lessonById(id: Long): LessonE?
    @Query("SELECT * FROM AttemptE WHERE chapterId=:c ORDER BY at DESC") abstract suspend fun attempts(c: Long): List<AttemptE>
    @Query("SELECT * FROM WrongE WHERE chapterId=:c ORDER BY id DESC") abstract suspend fun wrongs(c: Long): List<WrongE>
    @Query("SELECT * FROM CardStateE WHERE key=:k") abstract suspend fun card(k: String): CardStateE?
    @Query("UPDATE ChapterE SET title=:t WHERE id=:id") abstract suspend fun renameChapter(id: Long, t: String)
    @Query("UPDATE LessonE SET title=:t WHERE id=:id") abstract suspend fun renameLesson(id: Long, t: String)
    @Query("UPDATE ChapterE SET pos=:p WHERE id=:id") abstract suspend fun setChapterPos(id: Long, p: Int)
    @Query("UPDATE LessonE SET pos=:p WHERE id=:id") abstract suspend fun setLessonPos(id: Long, p: Int)
    @Query("DELETE FROM ChapterE WHERE id=:id") abstract suspend fun deleteChapter(id: Long)
    @Query("DELETE FROM LessonE WHERE id=:id") abstract suspend fun deleteLesson(id: Long)
    @Query("DELETE FROM LessonE WHERE chapterId=:c") abstract suspend fun deleteLessonsOf(c: Long)
    @Query("DELETE FROM WrongE WHERE chapterId=:c") abstract suspend fun deleteWrongsOf(c: Long)
    @Query("DELETE FROM WrongE WHERE id=:id") abstract suspend fun deleteWrong(id: Long)
    @Query("DELETE FROM WrongE WHERE chapterId=:c AND q=:q") abstract suspend fun deleteWrongByQ(c: Long, q: String)
    @Query("DELETE FROM AttemptE WHERE chapterId=:c") abstract suspend fun deleteAttemptsOf(c: Long)
    @Query("DELETE FROM BookE WHERE id=:id") abstract suspend fun deleteBook(id: Long)
    @Transaction open suspend fun deleteChapterCascade(id: Long) { deleteLessonsOf(id); deleteWrongsOf(id); deleteAttemptsOf(id); deleteChapter(id) }
    @Transaction open suspend fun deleteBookCascade(id: Long) { for (c in chapters(id)) deleteChapterCascade(c.id); deleteBook(id) }
    @Transaction open suspend fun saveBook(book: BookE, chapters: List<com.abonesma.core.Chapter>): Long {
        val bid = book(book)
        chapters.forEachIndexed { ci, c ->
            val cid = chapter(ChapterE(bookId = bid, title = c.title, pos = ci))
            c.lessons.forEachIndexed { li, l -> lesson(LessonE(chapterId = cid, title = l.title, text = l.text, pos = li)) }
        }
        return bid
    }
}
@Database(entities = [BookE::class, ChapterE::class, LessonE::class, AttemptE::class, WrongE::class, CardStateE::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): Dao
    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(c: Context): AppDb = inst ?: synchronized(this) { inst ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, "nesma.db").build().also { inst = it } }
    }
}
