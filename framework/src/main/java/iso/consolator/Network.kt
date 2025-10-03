@file:JvmName("Network")
@file:JvmMultifileClass

package iso.consolator

import android.net.*
import android.net.ConnectivityManager.*
import android.net.NetworkCapabilities.*
import androidx.lifecycle.*
import ctx.consolator.*
import iso.consolator.annotation.*
import iso.consolator.reflect.*
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*
import kotlin.reflect.*
import kotlinx.coroutines.*
import okhttp3.*
import kotlinx.coroutines.Dispatchers.IO

internal val network
    get() = connectivityManager.activeNetwork

internal val isConnected
    get() = connectivityRequest?.canBeSatisfiedBy(networkCapabilities).isTrue()

internal var hasInternet = false
    get() = isConnected and field

internal val hasMobile
    get() = networkCapabilities?.hasTransport(TRANSPORT_CELLULAR).isTrue()

internal val hasWifi
    get() = networkCapabilities?.hasTransport(TRANSPORT_WIFI).isTrue()

internal val networkCapabilities
    get() = with(connectivityManager) { getNetworkCapabilities(activeNetwork) }

private val connectivityManager
    get() = foregroundContext.getSystemService(ConnectivityManager::class.java)!!

/** Registers the default network callback in lifecycle owner scope for receiving network state changes. */
@Coordinate @Tag(NET_CAP_REGISTER)
fun registerNetworkCallback(owner: LifecycleOwner? = null) {
    networkCallback?.apply(connectivityManager::registerDefaultNetworkCallback) }

/** Unregisters the default network callback in lifecycle owner scope. */
@Tag(NET_CAP_UNREGISTER)
fun unregisterNetworkCallback(owner: LifecycleOwner? = null) {
    networkCallback?.apply(connectivityManager::unregisterNetworkCallback)
    clearNetworkCallbackObjects() }

private var networkCallback: NetworkCallback? = null
    get() = field ?: object : NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            trySafely { reactToNetworkCapabilitiesChanged.invoke(network, networkCapabilities) } } }
        .also { field = it }

internal var reactToNetworkCapabilitiesChanged: (Network, NetworkCapabilities) -> Unit = { network, networkCapabilities ->
    commit<NetworkResolver, NetworkContext>(::saveNetworkCapabilities, SchedulerScope(), network, networkCapabilities) }

@Tag(NET_CAP_UPDATE)
private suspend fun saveNetworkCapabilities(scope: CoroutineScope, network: Network, networkCapabilities: NetworkCapabilities) =
    updateNetworkCapabilities(network, networkCapabilities)

@Coordinate @Tag(INET_REGISTER)
internal fun registerInternetCallback(scope: CoroutineScope) {
    scope.relaunch(::networkCaller, calls, IO, step = ::repeatNetworkCallFunction) }

/** Registers the internet callback in lifecycle owner scope for sending out and managing periodic network requests. */
@Coordinate @Tag(INET_REGISTER)
fun startInternetCallback(owner: LifecycleOwner? = null) {
    withSchedulerScope {
    owner?.relaunch(::networkCaller, calls, IO, step = ::repeatNetworkCallFunction) } }

internal fun pauseInternetCallback() {
    isNetCallbackResumed = false }

internal fun resumeInternetCallback() {
    isNetCallbackResumed = true }

/** Unregisters the internet callback in lifecycle owner scope. */
@Tag(INET_UNREGISTER)
fun unregisterInternetCallback(owner: LifecycleOwner? = null) {
    networkCaller?.cancel()
    netcall_delay_time = delay_reset
    clearInternetCallbackObjects() }

@JobTreeRoot @NetworkListener
@Coordinate @Tag(INET)
internal var networkCaller: Job? = null
    set(value) {
        // update addressable layer
        field = ::networkCaller.receiveUniquely(value) }

@Tag(INET_CALL)
internal var netCall: Call? = null
    private set

private suspend fun repeatNetworkCallFunction(scope: CoroutineScope) {
    scope.resolveSuspended(
        group = calls,
        block = netCallFunction,
        delayTime = NETCALL_MIN_DELAY) }

private var netCallFunction: NetworkJobContinuation =
    NetCallFunction.Callback

internal sealed class NetCallFunction<T, S, R> : suspend (T, S) -> R {
    override suspend fun invoke(scope: T, self: S): R {
        if (isNetCallbackResumed and hasNetCallRepeatTimeElapsed)
        scope.asCoroutineScope()?.run {
        log(info, INET_TAG, "Trying to send out http request for network caller...")
        launch { with(::netCall) {
        commit(this@run) { // inform scope about net call - scope can inform context - block will be committed with context in its scope
            // convert to contextual function by current context of scope - network contexts bring varieties to caller
            setInstance(this@run, INET_CALL,
                ::buildNetCall.implicitly().applyKeptOnce())
            sendForResult(this@run,
                { response ->
                    trySafelyCanceling {
                    reactToNetCallResponseReceived.commit(this@run, response) } },
                { ex ->
                    trySafelyCanceling {
                    reactToNetCallRequestFailed.commit(this@run, ex) }
                }) }
        } } }
        // return different result optionally - a stateful response is a great choice
        return Unit.type() }

    @Tag(INET_FUNCTION)
    data object Callback : NetCallFunction<Any?, Any?, Any?>()
}

private var reactToNetCallResponseReceived: JobResponseFunction =
    NetCallSuccessFunction.Callback

internal sealed class NetCallSuccessFunction<T, S, R> : (T, S, Response) -> R {
    @Suppress("UNCHECKED_CAST")
    override fun invoke(scope: T, self: S, response: Response): R {
        with(response) {
            hasInternet = isSuccessful
            if (isSuccessful)
                lastNetCallResponseTime = now()
            netcall_delay_time = delay_reset
            close() }
        scope.asResolverScope()?.log?.invoke(info, INET_TAG, "Received response for network caller.")
        // return other data types depending on the response type or scope
        return response as R }

    @Tag(INET_SUCCESS)
    data object Callback : NetCallSuccessFunction<Any?, Any?, Response>()
}

private var reactToNetCallRequestFailed: JobThrowableFunction =
    NetCallErrorFunction.Callback

internal sealed class NetCallErrorFunction<T, S, R> : (T, S, Throwable) -> R {
    @Suppress("UNCHECKED_CAST")
    override fun invoke(scope: T, self: S, ex: Throwable): R {
        hasInternet = false
        scope.asResolverScope()?.log?.invoke(warning, INET_TAG, "Failed to send http request for network caller.")
        // return different result optionally
        return ex as R }

    @Tag(INET_ERROR)
    data object Callback : NetCallErrorFunction<Any?, Any?, Throwable>()
}

private var isNetCallbackResumed = true

private val hasNetCallRepeatTimeElapsed
    get() = netCallRepeatInterval!!.hasTimeIntervalElapsed()

@Tag(MIN_DELAY)
private val NETCALL_MIN_DELAY = suspend { netcall_delay_time }

private const val delay_reset = -1L

@Tag(INET_DELAY)
internal var netcall_delay_time = delay_reset
    get() = field.runWhen({ it == delay_reset }) {
        netCallRepeatInterval!!.getDelayTime() }

private var netCallRepeatInterval: TimeInterval? = null
    get() = field ?: TimeInterval(::lastNetCallResponseTime, ::netCallRepeatTime)
        .also { field = it }

/** The minimum delay time for resending internet requests.
 *
 * default value: `5` seconds
 */
@Tag(INET_MIN_INTERVAL)
var netcall_min_time_interval = 5000L
    internal set

@Tag(INET_INTERVAL)
private var netCallRepeatTime = netcall_min_time_interval
    set(value) {
        field = maxOf(value, netcall_min_time_interval) }

private var lastNetCallResponseTime = 0L
    set(value) {
        if ((value > field) or value.isZeroTime)
            field = value }

internal fun <R> NetCall.commit(scope: Any?, block: () -> R) =
    lock(scope, block)

private fun <R> NetCall.lock(scope: Any?, block: () -> R) =
    synchronized(asCallable(), block)

private fun NetCall.asCallable() =
    when (this) {
    ::netCall -> ::netCall
    is CallableReference<*> -> asTypeUnsafe()
    else -> asProperty().getInstance().asReference() /* register lock */ }

private fun NetCall.asProperty() = this as KProperty

internal fun <R, S : R> NetCall.sendForResult(scope: Any?, respond: (Response) -> R, exit: (Throwable) -> S? = { null }) =
    tryCancelingForResult({ execute(scope).run(respond) }, exit)

private fun NetCall.execute(scope: Any?) =
    applyMarkTag(calls).getInstance(scope, INET_CALL).asType<NetCall>()
    ?.call()
    ?.execute()!!

private fun JobResponseFunction.commit(scope: Any?, response: Response) {
    markTag(calls)
    invoke(scope, this, response) }

private fun JobThrowableFunction.commit(scope: Any?, ex: Throwable) {
    markTag(calls)
    invoke(scope, this, ex) }

private var calls: FunctionSet? = null

internal fun NetCall.getInstance(scope: Any?, id: TagType): Any? =
    when (id) {
    INET_CALL -> this
    INET_FUNCTION -> netCallFunction
    INET_SUCCESS -> reactToNetCallResponseReceived
    INET_ERROR -> reactToNetCallRequestFailed
    INET_DELAY -> netcall_delay_time
    INET_INTERVAL -> netCallRepeatTime
    else -> null }

internal fun NetCall.setInstance(scope: Any?, id: TagType, value: Any?) {
    if (value !== null && value.isKept) {
        value.markSequentialTag(INET_CALL, id, calls) }
    lock(id) { when (id) {
        INET_CALL -> netCall = take(value)
        INET_FUNCTION -> netCallFunction = take(value)
        INET_SUCCESS -> reactToNetCallResponseReceived = take(value)
        INET_ERROR -> reactToNetCallRequestFailed = take(value)
        INET_DELAY -> netcall_delay_time = take(value)
        INET_INTERVAL -> netCallRepeatTime = take(value) } } }

private sealed interface NetworkResolver : Resolver

internal sealed interface NetworkContext : SchedulerContext

@Retention(SOURCE)
@Target(FUNCTION, PROPERTY)
private annotation class NetworkListener

internal fun buildNetCall(scope: Any?, key: Any) = buildHttpCall(key)

private fun NetCall.implicitly(applied: Boolean = true, key: Any = "https://httpbin.org/delay/1"): Call? = call(key)

internal fun buildHttpCall(key: Any, method: String = "GET", body: RequestBody? = null, headers: Headers? = null, retry: Boolean = false) =
    when (key) {
    is Call -> key
    else ->
        httpClient(retry)
        .newCall { newRequest {
            url(key.asUrl()).also {
            method(method, body)
            headers?.run(::headers) } } } }

private fun httpClient(retry: Boolean = false) =
    OkHttpClient.Builder()
    .retryOnConnectionFailure(retry)
    .build()

private inline fun OkHttpClient.newCall(block: Request.Builder.() -> Request) =
    newCall(Request.Builder().block())

private inline fun Request.Builder.newRequest(block: Request.Builder.() -> Request.Builder) =
    block().build()

private fun Any.asUrl() = toString()

private var connectivityRequest: NetworkRequest? = null
    get() = field ?: buildNetworkRequest {
        addCapability(NET_CAPABILITY_INTERNET) }
        .also { field = it }

private inline fun buildNetworkRequest(block: NetworkRequest.Builder.() -> Unit) =
    NetworkRequest.Builder().apply(block).build()

private fun clearNetworkCallbackObjects() {
    networkCallback = null
    connectivityRequest = null }
private fun clearInternetCallbackObjects() {
    networkCaller = null
    netCall = null
    netCallRepeatInterval = null }

private inline fun <reified T : Any> take(value: Any?): T = value.asTypeUnsafe()

private typealias NetCall = KCallable<Call?>
private typealias ResponseFunction = (Response) -> Unit
private typealias NetworkJobContinuation = suspend (Any?, Any?) -> NetworkJobContinuationIdentityType
private typealias JobResponseFunction = (Any?, Any?, Response) -> Any?
private typealias JobThrowableFunction = (Any?, Any?, Throwable) -> Any?

private typealias NetworkJobContinuationIdentityType = Any?

internal const val NET_CAP = 1896
internal const val NET_CAP_REGISTER = 1897
internal const val NET_CAP_UNREGISTER = 1898
internal const val NET_CAP_UPDATE = 1899

internal const val INET = 1990
internal const val INET_REGISTER = 1991
internal const val INET_UNREGISTER = 1992
internal const val INET_CALL = 1993
internal const val INET_FUNCTION = 1994
internal const val INET_SUCCESS = 1995
internal const val INET_ERROR = 1996
internal const val INET_DELAY = 1997
internal const val INET_INTERVAL = 1998
const val INET_MIN_INTERVAL = 1999

internal const val INET_TAG = "INTERNET"