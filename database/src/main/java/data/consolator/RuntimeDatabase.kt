@file:JvmName(JVM_CLASS_NAME)
@file:JvmMultifileClass

package data.consolator

import android.content.*
import androidx.room.*
import data.consolator.dao.*
import data.consolator.entity.*
import java.text.*
import java.util.*
import kotlin.reflect.*
import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.CLASS
import ctx.consolator.JVM_CLASS_NAME

internal const val DB_VERSION = 1

var runDb: RuntimeDatabase? = null
    set(value) {
        field = ::runDb.receiveInDataModule(value) }

var session: RuntimeSessionEntity? = null
    set(value) {
        field = ::session.receiveInDataModule(value) }

@Database(version = DB_VERSION, exportSchema = false, entities = [
    RuntimeSessionEntity::class,
])
@File("run.db")
abstract class RuntimeDatabase : RoomDatabase() {
    internal abstract fun runtimeDao(): RuntimeDao

    companion object {
        internal const val CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP"
        internal const val ID = "_id"
        internal const val DB_TAG = "DATABASE"
} }

suspend fun buildNewSession(startTime: Long) {
    RuntimeDao {
    session = getSession(
        newSession(startTime)) } }

fun <D : RoomDatabase> buildDatabase(context: Context, cls: KClass<D>, name: String? = cls.lastAnnotatedFilename()) =
    with(cls) { buildDatabase(context, java, name) }

private fun <D : RoomDatabase> buildDatabase(context: Context, cls: Class<D>, name: String?) =
    Room.databaseBuilder(context, cls, name).build()

/** Runtime database timestamp format. */
var dateTimeFormat: DateFormat? = null
    get() = field ?: SimpleDateFormat(DATE_FORMAT, Locale.US)
        .also { field = it }

internal fun String.toLocalTime() =
    run(dateTimeFormat!!::parse)?.time

internal fun Long.toLocalTimestamp() =
    dateTimeFormat!!.format(run(::Date))

private var dbTimeDiff: Long? = null
    get() = field ?: session?.run {
        dbTime.toLocalTime()!! - initTime }
        ?.also { field = it }

/** can only be applied to db timestamps. */
private fun String.toCurrentLocalTime() = toLocalTime()!!.minus(dbTimeDiff!!)

/** can only be applied to localized db times. */
private fun Long.toCurrentDatabaseTime() = plus(dbTimeDiff!!)

internal fun <R> KCallable<R>.receiveInDataModule(value: R) = value

fun clearObjects() {
    dateTimeFormat = null
    dbTimeDiff = null }

@Retention(SOURCE)
@Target(CLASS)
@Repeatable
internal annotation class File(
    val name: String)

internal fun <T : Any> KClass<out T>.annotatedFiles() =
    annotations.filterIsInstance<File>()

internal fun <T : Any> KClass<out T>.lastAnnotatedFile() =
    annotations.last { it is File } as File

internal fun <T : Any> KClass<out T>.lastAnnotatedFilename() =
    lastAnnotatedFile().name

private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"