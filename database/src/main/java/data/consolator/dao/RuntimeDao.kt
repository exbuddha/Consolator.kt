package data.consolator.dao

import android.os.Build
import androidx.room.*
import ctx.consolator.now
import data.consolator.*
import data.consolator.entity.*

@Dao
abstract class RuntimeDao {
    @Query("INSERT INTO ${RuntimeSessionEntity.TABLE}(${RuntimeSessionEntity.CTX_TIME},${RuntimeSessionEntity.INIT_TIME},${RuntimeSessionEntity.INIT_TIMESTAMP},${RuntimeSessionEntity.DB_VERSION},${RuntimeSessionEntity.BUILD_TAGS},${RuntimeSessionEntity.BUILD_TIME}) VALUES (:ctxTime,:initTime,:initTimestamp,:dbVersion,:buildTags,:buildTime)")
    abstract suspend fun newSession(ctxTime: Long, initTime: Long = now(), initTimestamp: String = initTime.toLocalTimestamp(), dbVersion: Int = DB_VERSION, buildTags: String = Build.TAGS, buildTime: Long = Build.TIME): Long

    @Query("SELECT * FROM ${RuntimeSessionEntity.TABLE} WHERE id == :id")
    abstract suspend fun getSession(id: Long): RuntimeSessionEntity

    @Query("SELECT * FROM ${RuntimeSessionEntity.TABLE} ORDER BY id DESC LIMIT 1")
    abstract suspend fun getFirstSession(): RuntimeSessionEntity

    @Query("DELETE FROM ${RuntimeSessionEntity.TABLE} WHERE id NOT IN (SELECT id FROM ${RuntimeSessionEntity.TABLE} ORDER BY id DESC LIMIT :n)")
    abstract suspend fun truncateSessions(n: Int = 1)

    @Query("DELETE FROM ${RuntimeSessionEntity.TABLE}")
    abstract suspend fun dropSessions()

    companion object {
        @JvmStatic suspend operator fun <R> invoke(block: suspend RuntimeDao.() -> R) =
            runDb!!.runtimeDao().block()
} }