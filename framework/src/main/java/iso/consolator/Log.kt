@file:JvmName("Log")
@file:JvmMultifileClass

package iso.consolator

import android.util.Log

sealed interface LoggerScope {
    val log: LogInvoker
        get() = iso.consolator.log
}

internal lateinit var log: LogInvoker

lateinit var info: LogFunction
lateinit var debug: LogFunction
lateinit var warning: LogFunction

operator fun LogFunction.plus(other: LogFunction): LogFunction = { tag, msg ->
    this(tag, msg)
    other(tag, msg) }

operator fun LogFunction.times(other: LogFunction): LogFunction = { tag, msg ->
    if (isOn) invoke(tag, msg)
    if (other.isOn) other(tag, msg) }

private val bypass: LogFunction = { _, _ -> }
private val LogFunction.isOn
    get() = isNotObject(bypass)

fun enableLogger() { log = { log, tag, msg -> log(tag, msg) } }
fun restrictLogger() { log = { log, tag, msg -> if (log.isOn) log(tag, msg) } }
fun disableLogger() { log = { _, _, _ -> } }

fun enableInfoLog() { info = { tag, msg -> Log.i(tag.toString(), msg.toString()) } }
fun enableDebugLog() { debug = { tag, msg -> Log.d(tag.toString(), msg.toString()) } }
fun enableWarningLog() { warning = { tag, msg -> Log.w(tag.toString(), msg.toString()) } }
fun enableAllLogs() {
    enableInfoLog()
    enableDebugLog()
    enableWarningLog() }
fun bypassInfoLog() { info = bypass }
fun bypassDebugLog() { debug = bypass }
fun bypassWarningLog() { warning = bypass }
fun bypassAllLogs() {
    bypassInfoLog()
    bypassDebugLog()
    bypassWarningLog() }

private typealias LogFunction = (CharSequence, CharSequence) -> Any?
internal typealias LogInvoker = (LogFunction, CharSequence, CharSequence) -> Any?