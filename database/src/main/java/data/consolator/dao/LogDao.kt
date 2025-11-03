package data.consolator.dao

import androidx.room.*
import data.consolator.*

@Dao
abstract class LogDao {
    companion object {
        @JvmStatic suspend operator fun <R> invoke(block: suspend LogDao.() -> R): R =
            logDb!!.logDao().block()
} }