@file:JvmName(JVM_CLASS_NAME)
@file:JvmMultifileClass

package iso.consolator

import android.app.*
import android.content.*
import android.content.pm.*
import android.content.res.Configuration
import android.net.*
import androidx.core.content.*
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.room.*
import ctx.consolator.*
import data.consolator.*
import data.consolator.dao.*
import iso.consolator.annotation.*
import iso.consolator.component.*
import iso.consolator.component.SchedulerActivity.*
import iso.consolator.exception.SchedulerIntent
import iso.consolator.reflect.*
import java.lang.*
import kotlin.reflect.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json

internal lateinit var instance: Application

var service: BaseServiceScope? = null

internal var receiver: BroadcastReceiver? = null
    get() = field.singleton().also { field = it }

internal val foregroundContext: Context
    get() = service?.asContext() ?: instance

internal val foregroundActivity
    get() = foregroundLifecycleOwner?.let {
        if (it is Activity) it
        else it.asFragment()?.activity } as? Activity

internal val foregroundFragment
    get() = foregroundLifecycleOwner?.asFragment()

internal var foregroundLifecycleOwner: LifecycleOwner? = null
    set(value) {
        field = ::foregroundLifecycleOwner.receiveUniquely(value) }

internal val processLifecycleScope
    get() = processLifecycleOwner.lifecycleScope

private val processLifecycleOwner
    get() = ProcessLifecycleOwner.get()

@Key(1)
var applicationMigrationManager: ApplicationMigrationManager? = null

@Key(2)
var activityConfigurationChangeManager: ConfigurationChangeManager? = null

@Key(3)
var activityNightModeChangeManager: NightModeChangeManager? = null

@Key(4)
var activityLocalesChangeManager: LocalesChangeManager? = null

context(provider: Context)
internal fun commitToMigrationManager(vararg context: Any?) =
    ::applicationMigrationManager.requireThenCommit(provider, *context)

context(provider: Context)
internal fun commitToMemoryManager(level: Int) =
    provider.asObjectProvider()?.provide(MemoryManager::class)?.asMemoryManager()?.commit(level)

context(provider: Context)
internal fun commitToConfigurationChangeManager(newConfig: Configuration) =
    ::activityConfigurationChangeManager.requireThenCommit(provider, newConfig)

context(provider: Context)
internal fun commitToNightModeChangeManager(mode: Int) =
    ::activityNightModeChangeManager.requireThenCommit(provider, mode)

context(provider: Context)
internal fun commitToLocalesChangeManager(locales: LocaleListCompat) =
    ::activityLocalesChangeManager.requireThenCommit(provider, locales)

private inline fun <reified T : Resolver> KMutableProperty<T?>.requireThenCommit(provider: Any, vararg context: Any?) =
    require(provider)?.commit(*context)

fun clearResolverObjects() {
    activityConfigurationChangeManager = null
    activityNightModeChangeManager = null
    activityLocalesChangeManager = null
    applicationMigrationManager = null }

@Throws
context(instance: Application)
internal fun touchContext(context: Context = instance) {
    if (context is MainUncaughtExceptionHandler) {
        mainUncaughtExceptionHandler = context
            .apply(Thread::setDefaultUncaughtExceptionHandler)
        State[-1] = State.Resolved }
    if (instance.typeIs<SchedulerApplication, _>())
        with(processLifecycleOwner) {
        currentThread.from(
            APP_INIT,
            SchedulerScope) {
        commit {
            asTypeUnsafe<DefaultScope>()
            .withLazy(::mainUncaughtExceptionHandler) {
            setTo(
                ::mainUncaughtExceptionHandler,
                ::uncaughtException.tag) } } } } }

@Tag(UNCAUGHT_SHARED)
internal lateinit var mainUncaughtExceptionHandler: MainUncaughtExceptionHandler

interface MainUncaughtExceptionHandler : ExceptionHandler, ExceptionHandlerReceptor {
    override fun <R> ReceiverItem<R>.set(value: R): ExceptionHandlerReceptor {
        ::mainUncaughtExceptionHandler.receiveUniquely(
            value.asTypeUnsafe())
        /* also process tags */
        .also { mainUncaughtExceptionHandler = it }
        return asTypeUnsafe() }

    fun setToFunction(handler: UnitKFunction): ExceptionHandlerReceptor {
        // lookup tag in class or function
        return this }
}

private typealias ExceptionHandlerReceptor = ReceiverItem<KCallable<ExceptionHandler>>

fun interface SchedulerResumption : SchedulerConjunction.Variable.Instance<WeakContext>

sealed interface SchedulerConjunction<in T, out R> : suspend (CoroutineScope?, T) -> R {
    sealed interface Variable<T, out R : T> : SchedulerConjunction<T, R> {
        sealed interface Instance<T> : Variable<T, T>
    }

    sealed interface Vector<T> : Variable.Instance<Array<out T>>
}

// context change functions - repository

internal inline fun <R> withForegroundContext(block: Context.() -> R) =
    with(foregroundContext, block)

internal fun Context.change(stage: ContextStep) =
    commit { stage(this) }

internal fun Context.changeLocally(owner: LifecycleOwner, stage: ContextStep) =
    commit { stage(this) }

internal fun Context.changeBroadly(ref: WeakContext = asWeakReference(), stage: ContextStep) =
    commit { stage(this) }

internal fun Context.changeGlobally(owner: LifecycleOwner, ref: WeakContext = asWeakReference(), stage: ContextStep) =
    commit { stage(this) }

internal suspend fun updateNetworkState() {
    NetworkDao {
    updateNetworkState(
        isConnected,
        hasInternet,
        hasMobile,
        hasWifi) } }

internal suspend fun updateNetworkCapabilities(network: Network? = iso.consolator.network, networkCapabilities: NetworkCapabilities? = iso.consolator.networkCapabilities) {
    networkCapabilities?.run {
    NetworkDao {
    updateNetworkCapabilities(
        Json.encodeToString(capabilities),
        linkDownstreamBandwidthKbps,
        linkUpstreamBandwidthKbps,
        signalStrength,
        network.hashCode()) } } }

internal inline fun <reified D : RoomDatabase> buildDatabase(context: Context) =
    buildDatabase(context, D::class)

internal inline fun <reified D : RoomDatabase> commitBuildDatabase(context: Context, instance: KMutableProperty<out D?>) =
    instance.requireAsync(constructor = { buildDatabase<D>(context).also(instance::setInstance) })

internal interface DatabaseContext : SchedulerContext

internal val isRuntimeDbNull get() = runDb.isNullValue()
internal val isRuntimeDbNotNull get() = runDb.isNotNullValue()
internal val isSessionNull get() = session.isNullValue()
internal val isSessionNotNull get() = session.isNotNullValue()
internal val isLogDbNull get() = logDb.isNullValue()
internal val isLogDbNotNull get() = logDb.isNotNullValue()
internal val isNetDbNull get() = netDb.isNullValue()
internal val isNetDbNotNull get() = netDb.isNotNullValue()
internal val isRuntimeDbOrSessionNull get() = isRuntimeDbNull or isSessionNull
internal val isLogDbOrNetDbNull get() = isLogDbNull or isNetDbNull

internal fun clearRuntimeDbObjects() {
    runDb = null }
internal fun clearLogDbObjects() {
    logDb = null }
internal fun clearNetDbObjects() {
    netDb = null }
internal fun clearAllDbObjects() {
    clearRuntimeDbObjects()
    clearLogDbObjects()
    clearNetDbObjects()
    clearObjects() }
internal fun clearSessionObjects() {
    session = null }

internal fun Context.isPermissionGranted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) `is` PackageManager.PERMISSION_GRANTED

internal fun Context.intendFor(cls: Class<*>) = Intent(this, cls)
internal fun Context.intendFor(cls: AnyKClass) = intendFor(cls.java)

internal val Context.startTime
    get() = uniqueStartTime()

internal fun Context.uniqueStartTime(fallback: LongFunction = { -1L }) =
    if (this is UniqueContext.Instance) uid
    else fallback()

internal fun Context.uniqueStartTimeOrNow() =
    uniqueStartTime(::now)

internal typealias ContextStep = suspend Context.(Any?) -> Any?

internal open class Propagate : SchedulerIntent()

internal inline fun <reified T : Any> T?.defaultSingleton(crossinline constructor: () -> T = { T::class.new() }, lock: Any = T::class.lock) =
    commitAsyncForResult(lock, { isNullValue() }, constructor, { this }) as T

internal inline fun <reified T : Any> T?.singleton(vararg args: Any?, noinline constructor: (VarArray) -> T = { it.asNew() }, lock: Any = T::class.lock) =
    defaultSingleton(args.with(constructor), lock)

internal inline fun <reified T : Any> T?.reconstruct(vararg args: Any?, constructor: KCallable<T?> = T::class::new) =
    require { args.runCall<T?>(constructor) }

internal inline fun <reified T : Any> T?.reconstruct(vararg args: Any?) =
    require(args::asNew)

internal inline fun <T> T?.require(constructor: () -> T) =
    this ?: constructor()

internal inline fun <reified T> KMutableProperty<out T?>.instantiate(provider: Any = T::class) =
    applyRenew {
    when (provider) {
        Activity::class,
        Fragment::class ->
            foregroundLifecycleOwner?.provide(T::class)
        Service::class,
        BaseServiceScope::class ->
            service.asContextProvide(T::class)
        is BaseServiceScope ->
            provider.asContextProvide(T::class)
        else ->
            reconstruct(provider) } as? T }

internal inline fun <reified T> KMutableProperty<out T?>.reconstruct(provider: Any = T::class) =
    applyRenew {
    (if (provider is AnyKClass)
        provider.emptyConstructor.call()
    else
        provider.provide(T::class)) as T }

private inline fun <T> KMutableProperty<out T?>.applyRenew(constructor: () -> T? = ::getInstance) =
    apply { renew(constructor) }

internal inline fun <T> KMutableProperty<out T?>.renew(constructor: () -> T? = ::getInstance) {
    if (isNull())
        setInstance(constructor()) }

internal inline fun <reified T> KMutableProperty<out T?>.require(provider: Any = T::class) =
    reconstruct(provider).getInstance()

internal inline fun <T> KMutableProperty<out T?>.require(predicate: (T) -> Boolean = ::trueWhenNull, constructor: () -> T? = ::getInstance) =
    getInstance().runWhen(
        { it === null || predicate(it) },
        { constructor()!!.also(this@require::setInstance) })!!

internal inline fun <T> KMutableProperty<out T?>.requireAsync(predicate: (T) -> Boolean = ::trueWhenNull, constructor: () -> T? = ::getInstance) =
    require(predicate) {
        synchronized(this) {
            require(predicate, constructor) } }

internal fun Any?.asActivity() = asType<Activity>()
internal fun Any?.asFragment() = asType<Fragment>()
internal fun Any?.asLifecycleOwner() = asType<LifecycleOwner>()
internal fun Any?.asContext() = asType<Context>()
internal fun Any?.asWeakContext() = asType<WeakContext>()
internal fun Any?.asUniqueContext() = asType<UniqueContext.Instance>()

const val JVM_CLASS_NAME = ctx.consolator.JVM_CLASS_NAME