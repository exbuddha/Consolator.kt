package net.consolator

import iso.consolator.*
import iso.consolator.annotation.*
import kotlin.reflect.*
import kotlinx.coroutines.*

/**
 * Application for configuring and activating the resolver scope.
 */
@Coordinate
open class DefaultApplication : BaseApplication() {
    /**
     * Calls [begin].
     *
     *  @sample onCreate
     */
    override fun onCreate() {
        super.onCreate()
        withCallableScope {
        ::begin.commitStep() }
    }

    /**
     * Loads initial values from resource.
     *
     * @param scope the resolver scope.
     */
    @Scope @Tag(APP_INIT)
    private suspend fun begin(scope: CoroutineScope) {
        withSchedulerScope { with(currentThread) {
        log(info,
        registerValue(
            R.integer.netcall_min_time_interval.toLong(),
            ::netcall_min_time_interval),
        "Minimum interval for netcall was found in resource.")
        log(info,
        registerValue(
            R.integer.view_min_delay.toLong(),
            ::view_min_delay),
        "Minimum delay for view was found in resource.") } } }

    companion object {
        /**
         * Caches [value] and identifies it with [target].
         *
         * @param V the value type.
         * @return the value instance.
         */
        @JvmStatic fun <V> registerValue(value: V, target: KProperty<V>) =
            Value(value).setTarget(target)

        /**
         * Caches [value] and identifies it by [tag].
         *
         * @param V the value type.
         * @return the value instance.
         */
        @JvmStatic fun <V> registerValueByTag(value: V, tag: TagType) =
            Value(value).setTag(tag)
    }
}