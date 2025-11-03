package iso.consolator.component

import android.app.Application
import android.content.SharedPreferences
import ctx.consolator.UniqueContext
import iso.consolator.asStringCounted
import iso.consolator.commit
import iso.consolator.rejectWithImplementationRestriction
import iso.consolator.resultWhen
import ctx.consolator.now
import iso.consolator.AnyCoroutineStep
import iso.consolator.AnyKClass
import iso.consolator.ObjectProvider
import iso.consolator.Time
import iso.consolator.annotation.Key
import kotlin.reflect.KFunction

abstract class SchedulerApplication : Application(), ObjectProvider, UniqueContext.Instance {
    /** Unique context start time. */
    @Key(0)
    override var uid = now()

    /** Sets whether or not the base service is enabled. */
    @Key(1)
    open var isSchedulerServiceEnabled = true

    /**
     * Retrieves the last uncaught exception type from shared preferences.
     *
     * @return the exception class type or `null` if it doesn't exist.
     */
    abstract fun getLastUncaughtExceptionType(depth: Int = 0): AnyKClass?

    /**
     * Retrieves the uncaught exception message from shared preferences.
     *
     * @param depth the exception index. (zero-based)
     *
     * @return the exception message or `null` if it doesn't exist.
     */
    abstract fun getLastUncaughtExceptionMessage(depth: Int = 0): String?

    /**
     * Retrieves the last uncaught exception time from shared preferences.
     *
     * @return the exception time or `null` if it doesn't exist.
     */
    abstract fun getLastUncaughtExceptionTime(): Time?

    /**
     * Retrieves the last uncaught exception context start time from shared preferences.
     *
     * @return the exception context start time or `null` if it doesn't exist.
     */
    abstract fun getLastUncaughtExceptionContextUid(): Time?

    /**
     * Retrieves the last uncaught exception thread state from shared preferences.
     *
     * If the exception was thrown on the main thread, the returned value is `true`.
     *
     * @return the exception thread state or `null` if it doesn't exist.
     */
    abstract fun getLastUncaughtExceptionWasMainThread(): Boolean?

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        commit<MemoryManager>(level) }

    /** Commits step for execution to the scheduler. */
    fun commit(step: AnyCoroutineStep) = iso.consolator.commit(step = step)

    protected tailrec fun <T, K : Any, V> SharedPreferences.Editor.putIteration(item: T, function: SharedPreferences.Editor.(String, V) -> Any? = ::putKeyValuePairAsString, vararg map: Pair<K, (T) -> V>, next: (T) -> T?, index: Int = 0, depth: Int = 0) {
        putItem(item, function, *map, index = index)
        val item = next(item) ?: return
        val depth = depth - 1
        if (depth == 0) return
        putIteration(item, function, *map, next = next, index = index + 1, depth = depth) }

    context(iterator: Iterator<T>)
    protected tailrec fun <T, K : Any, V> SharedPreferences.Editor.putIteration(function: SharedPreferences.Editor.(String, V) -> Any? = ::putKeyValuePairAsString, vararg map: Pair<K, (T) -> V>, index: Int = 0, depth: Int = 0) {
        if (depth == 0) return
        val item = with(iterator) { if (hasNext()) next() else return }
        putItem(item, function, *map, index = index)
        putIteration(function, *map, index = index + 1, depth = depth - 1) }

    private fun <T, K : Any, V> SharedPreferences.Editor.putItem(item: T, function: SharedPreferences.Editor.(String, V) -> Any?, vararg map: Pair<K, (T) -> V>, index: Int) {
        fun put(str: K, value: V) =
            function(str.counted(index), value)
        map.forEach {
            val (key, value) = it
            put(key, value(item)) } }

    private fun <V> putKeyValuePairAsString(editor: SharedPreferences.Editor, key: String, value: V) =
        editor.putString(key, value.toString())

    protected fun SharedPreferences.getCountedStringOrNull(str: String, index: Int = 0) =
        getString(str.counted(index), null)

    protected fun <R> SharedPreferences.getUncountedOrNull(str: String, function: KFunction<R>, default: R) =
        resultWhen({ contains(str) }) { function.call(this, str, default) }

    private fun Any.counted(n: Int) = asStringCounted(n)

    override fun provide(type: AnyKClass) = when (type) {
        MemoryManager::class ->
            object : MemoryManager {
                override fun commit(vararg context: Any?) {}
            }
        else ->
            rejectWithImplementationRestriction() }
}