package net.consolator

import android.content.*
import ctx.consolator.*
import iso.consolator.*
import iso.consolator.annotation.Key
import iso.consolator.annotation.Tag
import iso.consolator.component.SchedulerApplication

/**
 * Adds support to scheduler application for starting the service and editing the shared preferences.
 */
sealed class BaseApplication : SchedulerApplication(), MainUncaughtExceptionHandler {
    init {
        disableLogger()
        apply(::touchContext) }

    /** @suppress
     *
     * Starts the base service.
     *
     * Service will be started only if it is enabled by scheduler application.
     *
     * @sample onCreate
     */
    override fun onCreate() {
        super.onCreate()
        withSchedulerScope {
        commitStart(BaseService::class) }
    }

    @Tag(UNCAUGHT_SHARED)
    override fun uncaughtException(thread: Thread, ex: Throwable) {
        editUncaughtSharedPrefs {
        putLong(EX_TIME, now())
        putLong(EX_CTX_TIME, uid)
        putBoolean(EX_THREAD, thread.isMainThread)
        putException(ex) } }

    @Key(-1)
    final override fun getLastUncaughtExceptionType(depth: Int) =
        withUncaughtSharedPrefs { trySafelyForResult {
        SchedulerScope.findClass(getCountedStringOrNull(EX_TYPE, depth) ?: return null) } }

    @Key(-2)
    final override fun getLastUncaughtExceptionMessage(depth: Int) =
        withUncaughtSharedPrefs {
        getCountedStringOrNull(EX_MSG, depth) }

    @Key(-3)
    final override fun getLastUncaughtExceptionTime() =
        withUncaughtSharedPrefs {
        getUncountedOrNull(EX_TIME, ::getLong, 0L) }

    @Key(-4)
    final override fun getLastUncaughtExceptionContextUid() =
        withUncaughtSharedPrefs {
        getUncountedOrNull(EX_CTX_TIME, ::getLong, 0L) }

    @Key(-5)
    final override fun getLastUncaughtExceptionWasMainThread() =
        withUncaughtSharedPrefs {
        getUncountedOrNull(EX_THREAD, ::getBoolean, false) }

    /** Shared preferences for the uncaught exception. */
    private val uncaughtSharedPrefs
        get() = getSharedPreferences(UNCAUGHT_EX, MODE_PRIVATE)

    /**
     * Writes the exception and its chain of causes to shared preferences.
     *
     * If depth is `0`, every cause is written.
     *
     * @param ex the exception.
     * @param depth the number of inner causes to write.
     */
    private fun SharedPreferences.Editor.putException(ex: Throwable, depth: Int = 0) =
        putIteration(ex,
            map = EX_KVMAP,
            next = Throwable::cause,
            index = 0,
            depth = depth)

    internal companion object {
        /** Transit key for starting the main migration process. */
        const val ACTION_MIGRATE_APP: Short = 1

        /** KV map key in shared preferences for exception type. */
        private const val EX_TYPE = "$EXCEPTION"
        /** KV map key in shared preferences for exception message. */
        private const val EX_MSG = "$EXCEPTION_MESSAGE"
        /** KV map key in shared preferences for exception time. */
        private const val EX_TIME = "$NOW"
        /** KV map key in shared preferences for exception context time. (start time) */
        private const val EX_CTX_TIME = "$START"
        /** KV map key in shared preferences for exception thread state. (`true` if it had occurred on main thread) */
        private const val EX_THREAD = "$MAIN"
        /** Shared preferences name for uncaught exception. */
        private const val UNCAUGHT_EX = "$UNCAUGHT"

        /** Exception KV map. */
        private val EX_KVMAP: SharedPrefsExceptionKVMap = arrayOf(
            EX_TYPE to Throwable::qualifiedClassName,
            EX_MSG to Throwable::message,
        )

        /** Edits shared preferences for the uncaught exception. */
        private inline fun BaseApplication.editUncaughtSharedPrefs(block: SharedPreferences.Editor.() -> Unit) =
            with(uncaughtSharedPrefs.edit(), block)

        private inline fun <R> BaseApplication.withUncaughtSharedPrefs(block: SharedPreferences.() -> R) =
            with(uncaughtSharedPrefs, block)
    }
}

private typealias SharedPrefsExceptionKVMap = Array<Pair<CharSequence, (Throwable) -> String?>>