@file:JvmName("Transaction")
@file:JvmMultifileClass

package iso.consolator

import androidx.lifecycle.LifecycleOwner
import kotlin.reflect.KCallable
import kotlinx.coroutines.CoroutineScope

interface Resolver : ResolverScope {
    override fun commit(step: AnyCoroutineStep) =
        commit(blockOf(step))

    fun commit(vararg context: Any?): Unit?
}

sealed interface ResolverScope : RepetitionScope, CoroutineStepTransactor, LoggerScope

sealed interface CallableResolverScope : CallableRepetitionScope, CallableTransactor<CallableTransactionIdentityType> {
    fun <I : LifecycleOwner> I.call(step: AnyKCallable, vararg args: Any?): CallableTransactionIdentityType
    fun <I : LifecycleOwner> I.callBy(step: AnyKCallable, args: KParameterMap): CallableTransactionIdentityType

    fun AnyKCallable.commitStep(scope: CoroutineScope = SchedulerScope(), vararg args: Any?): CallableTransactionIdentityType
    fun AnyKCallable.commitStepBy(scope: CoroutineScope = SchedulerScope(), args: KParameterMap): CallableTransactionIdentityType
    fun AnyKCallable.commitSuspend(vararg args: Any?): CallableTransactionIdentityType
    fun AnyKCallable.commitSuspendBy(args: KParameterMap): CallableTransactionIdentityType
    fun AnyKCallable.commit(vararg args: Any?): CallableTransactionIdentityType
    fun AnyKCallable.commitBy(args: KParameterMap): CallableTransactionIdentityType
    fun AnyKCallable.commitSafely(vararg args: Any?): CallableTransactionIdentityType
    fun AnyKCallable.commitSafelyBy(args: KParameterMap): CallableTransactionIdentityType
}

sealed interface CallableTransactor<R> : Transactor<KCallable<R>, ResolverTransactionIdentityType>

sealed interface Transactor<T, out R> {
    fun commit(step: T): R
}

internal typealias CoroutineStepTransactor = Transactor<AnyCoroutineStep, ResolverTransactionIdentityType>

// job extensions provide detailed continuations for each step
// items group contains and clears them when scope changes to background

internal suspend inline fun <I : U, U : CoroutineScope, T> I.resolveSuspended(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, group: FunctionSet? = null, noinline block: suspend (U?, Any?) -> T) {
    markTagsForJobContinuationRepeat(block, group, currentJob(), predicate, delayTime)
    repeatBlock(predicate, delayTime) {
        blockSuspended(block) } }

internal suspend inline fun <I : U, U : CoroutineScope, T, S> I.resolveExtended(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, group: FunctionSet? = null, initial: S? = null, noinline block: suspend (U?, Any?, S?) -> T) {
    markTagsForJobExtensionRepeat(block, group, currentJob(), predicate, delayTime)
    repeatBlock(predicate, delayTime) {
        blockExtended(initial, block) } }

// convert to contextual function in I
internal suspend inline fun <I : U, U : CoroutineScope, T> I.resolveSuspendedImplicitly(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, noinline block: suspend (U?, Any?) -> T) =
    repeatBlock(predicate, delayTime) {
        /* implicit<I>() */ blockSuspended(block) }

// convert to contextual function in I
internal suspend inline fun <I : U, U : CoroutineScope, T, S> I.resolveExtendedImplicitly(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, initial: S? = null, noinline block: suspend (U?, Any?, S?) -> T) =
    repeatBlock(predicate, delayTime) {
        /* implicit<I>() */ blockExtended(initial, block) }

private suspend inline fun <I : U, U : CoroutineScope, T> I.blockSuspended(noinline block: suspend (U?, Any?) -> T) =
    block(this, block)

private suspend inline fun <I : U, U : CoroutineScope, T, S> I.blockExtended(initial: S? = null, noinline block: suspend (U?, Any?, S?) -> T) =
    block(this, block, initial)

typealias ResolverTransactionIdentityType = Any? /* symbolic to AnyCoroutineStep in ResolverScope */
private typealias CallableTransactionIdentityType = Any?

fun Any?.asResolverScope() = asType<ResolverScope>()

internal typealias AnyTransactor = Transactor<*,*>