@file:JvmName("Repetition")
@file:JvmMultifileClass

package iso.consolator

import iso.consolator.Interceptor.Companion.validate
import iso.consolator.exception.*
import kotlin.coroutines.*
import kotlinx.coroutines.*
import iso.consolator.annotation.Tag

sealed interface RepetitionScope : CoroutineScope, ProcessScope {
    override val coroutineContext: CoroutineContext
        get() = SchedulerContext
}

sealed interface CallableRepetitionScope : RepetitionScope

// implicit space (in-scope)

internal suspend inline fun <I : U, U : CoroutineScope, T> I.repeatBlock(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, crossinline block: suspend U.() -> T) {
    while (addedSuspendedIsActive(predicate)()) {
        block() // will be intercepted for reactivity
        delayOrYield(delayTime()) } }

private suspend inline fun <I : U, U : CoroutineScope, T, reified X : TimeoutCancellation> I.repeatBlockOrTimeout(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, noinline downTime: DelayFunction, msg: String? = null, cause: Throwable? = null, crossinline block: suspend U.() -> T) {
    while (addedSuspendedIsActiveNotTimedOut(downTime, predicate)()) {
        block() // will be intercepted for reactivity
        delayOrTimeout<_, X>(delayTime(), downTime(), msg, cause) } }

private suspend inline fun <I : U, U : CoroutineScope, T, reified X : TimeoutCancellation> I.repeatBlockOrTimeout(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, noinline downTime: DelayFunction, crossinline ex: (AnyArray) -> X = { it.rejectAsException<X>() }, vararg args: Any?, crossinline block: suspend U.() -> T) {
    while (addedSuspendedIsActiveNotTimedOut(downTime, predicate)()) {
        block() // will be intercepted for reactivity
        delayOrTimeout<_, X>(delayTime(), downTime(), ex, *args) } }

private suspend inline fun <I : U, U : CoroutineScope, S, T : S> I.repeatBlockForResult(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, crossinline block: suspend U.() -> T, crossinline condition: (S?, (S?) -> Boolean) -> Boolean) =
    repeatBlockDeterminedForOutResult(predicate, delayTime, block, condition, intercept = TODO() /* Implicit<I>() */) // or instead, pair with an exception mapper

private suspend inline fun <I : U, U : CoroutineScope, S, T : S> I.repeatBlockForResultUnless(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, crossinline block: suspend U.() -> T, noinline condition: (S?, (S?) -> Boolean) -> Boolean) =
    repeatBlockForResult<_, _, _, S>(predicate, delayTime, block, condition::not)

private suspend inline fun <I : U, U : CoroutineScope, S, T : S, reified X : TimeoutCancellation> I.repeatBlockOrTimeoutForResult(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, noinline downTime: DelayFunction, msg: String? = null, cause: Throwable? = null, crossinline block: suspend U.() -> T, crossinline condition: (S?, (S?) -> Boolean) -> Boolean): S =
    TODO()

private suspend inline fun <I : U, U : CoroutineScope, S, T : S, reified X : TimeoutCancellation> I.repeatBlockOrTimeoutForResultUnless(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, noinline downTime: DelayFunction, msg: String? = null, cause: Throwable? = null, crossinline block: suspend U.() -> T, noinline condition: (S?, (S?) -> Boolean) -> Boolean) =
    repeatBlockOrTimeoutForResult<_, _, _, S, X>(predicate, delayTime, downTime, msg, cause, block, condition::not)

private suspend inline fun <I : U, U : CoroutineScope, S, T : S, reified X : TimeoutCancellation> I.repeatBlockOrTimeoutForResult(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, noinline downTime: DelayFunction, crossinline ex: (AnyArray) -> X = { it.rejectAsException<X>() }, vararg args: Any?, crossinline block: suspend U.() -> T, crossinline condition: (S?, (S?) -> Boolean) -> Boolean): S =
    TODO()

private suspend inline fun <I : U, U : CoroutineScope, S, T : S, reified X : TimeoutCancellation> I.repeatBlockOrTimeoutForResultUnless(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, noinline downTime: DelayFunction, crossinline ex: (AnyArray) -> X = { it.rejectAsException<X>() }, vararg args: Any?, crossinline block: suspend U.() -> T, noinline condition: (S?, (S?) -> Boolean) -> Boolean) =
    repeatBlockOrTimeoutForResult<_, _, _, S, X>(predicate, delayTime, downTime, ex, *args, block = block, condition = condition::not)

private suspend inline fun <I : U, U : CoroutineScope, O, A : O, B : O, T, S : T> I.repeatBlockDeterminedForOutResult(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, crossinline block: suspend U.() -> T, crossinline condition: (T?, (T?) -> B) -> A, noinline avoid: (suspend (T?) -> Boolean)? = null, crossinline intercept: (T?) -> B, noinline determinant: ((A, B?) -> S)? = null): S =
    TODO()

private suspend inline fun <I : U, U : CoroutineScope, O, A : O, B : O, T, S : T> I.repeatBlockDeterminedForOutResultUnless(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, crossinline block: suspend U.() -> T, noinline condition: (T?, (T?) -> B) -> A, noinline avoid: (suspend (T?) -> Boolean)? = null, crossinline intercept: (T?) -> B, noinline determinant: ((A, B?) -> S)? = null): S =
    TODO()

private suspend inline fun <I : U, U : CoroutineScope, O, A : O, B : O, T, S : T, reified X : TimeoutCancellation> I.repeatBlockDeterminedOrTimeoutForOutResult(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, noinline downTime: DelayFunction, msg: String? = null, cause: Throwable? = null, crossinline block: suspend U.() -> T, crossinline condition: (T?, (T?) -> B) -> A, noinline avoid: (suspend (T?) -> Boolean)? = null, crossinline intercept: (T?) -> B, noinline determinant: ((A, B?) -> S)? = null): S =
    TODO()

private suspend inline fun <I : U, U : CoroutineScope, O, A : O, B : O, T, S : T, reified X : TimeoutCancellation> I.repeatBlockDeterminedOrTimeoutForOutResultUnless(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, noinline downTime: DelayFunction, msg: String? = null, cause: Throwable? = null, crossinline block: suspend U.() -> T, noinline condition: (T?, (T?) -> B) -> A, noinline avoid: (suspend (T?) -> Boolean)? = null, crossinline intercept: (T?) -> B, noinline determinant: ((A, B?) -> S)? = null): S =
    TODO()

private suspend inline fun <I : U, U : CoroutineScope, O, A : O, B : O, T, S : T, reified X : TimeoutCancellation> I.repeatBlockDeterminedOrTimeoutForOutResult(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, noinline downTime: DelayFunction, crossinline ex: (AnyArray) -> X = { it.rejectAsException<X>() }, vararg args: Any?, crossinline block: suspend U.() -> T, crossinline condition: (T?, (T?) -> B) -> A, noinline avoid: (suspend (T?) -> Boolean)? = null, crossinline intercept: (T?) -> B, noinline determinant: ((A, B?) -> S)? = null): S =
    TODO()

private suspend inline fun <I : U, U : CoroutineScope, O, A : O, B : O, T, S : T, reified X : TimeoutCancellation> I.repeatBlockDeterminedOrTimeoutForOutResultUnless(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, noinline downTime: DelayFunction, crossinline ex: (AnyArray) -> X = { it.rejectAsException<X>() }, vararg args: Any?, crossinline block: suspend U.() -> T, noinline condition: (T?, (T?) -> B) -> A, noinline avoid: (suspend (T?) -> Boolean)? = null, crossinline intercept: (T?) -> B, noinline determinant: ((A, B?) -> S)? = null): S =
    TODO()

private suspend inline fun <I : U, U : CoroutineScope, O, A : O, B : O, T, S : T> I.repeatBlockDeterminedForInResult(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, crossinline block: suspend U.() -> S, crossinline condition: (S?, (T?) -> B) -> A, noinline avoid: (suspend (T?) -> Boolean)? = null, crossinline intercept: (T?) -> B, noinline determinant: ((A, B?) -> S)? = null): T =
    TODO()

private suspend inline fun <I : U, U : CoroutineScope, O, A : O, B : O, T, S : T> I.repeatBlockDeterminedForInResultUnless(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, crossinline block: suspend U.() -> S, noinline condition: (S?, (T?) -> B) -> A, noinline avoid: (suspend (T?) -> Boolean)? = null, crossinline intercept: (T?) -> B, noinline determinant: ((A, B?) -> S)? = null): T =
    TODO()

private suspend inline fun <I : U, U : CoroutineScope, O, A : O, B : O, T, S : T, reified X : TimeoutCancellation> I.repeatBlockDeterminedOrTimeoutForInResult(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, noinline downTime: DelayFunction, msg: String? = null, cause: Throwable? = null, crossinline block: suspend U.() -> S, crossinline condition: (S?, (T?) -> B) -> A, noinline avoid: (suspend (T?) -> Boolean)? = null, crossinline intercept: (T?) -> B, noinline determinant: ((A, B?) -> S)? = null): T =
    TODO()

private suspend inline fun <I : U, U : CoroutineScope, O, A : O, B : O, T, S : T, reified X : TimeoutCancellation> I.repeatBlockDeterminedOrTimeoutForInResultUnless(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, noinline downTime: DelayFunction, msg: String? = null, cause: Throwable? = null, crossinline block: suspend U.() -> S, noinline condition: (S?, (T?) -> B) -> A, noinline avoid: (suspend (T?) -> Boolean)? = null, crossinline intercept: (T?) -> B, noinline determinant: ((A, B?) -> S)? = null): T =
    TODO()

private suspend inline fun <I : U, U : CoroutineScope, O, A : O, B : O, T, S : T, reified X : TimeoutCancellation> I.repeatBlockDeterminedOrTimeoutForInResult(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, noinline downTime: DelayFunction, crossinline ex: (AnyArray) -> X = { it.rejectAsException<X>() }, vararg args: Any?, crossinline block: suspend U.() -> S, crossinline condition: (S?, (T?) -> B) -> A, noinline avoid: (suspend (T?) -> Boolean)? = null, crossinline intercept: (T?) -> B, noinline determinant: ((A, B?) -> S)? = null): T =
    TODO()

private suspend inline fun <I : U, U : CoroutineScope, O, A : O, B : O, T, S : T, reified X : TimeoutCancellation> I.repeatBlockDeterminedOrTimeoutForInResultUnless(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, noinline downTime: DelayFunction, crossinline ex: (AnyArray) -> X = { it.rejectAsException<X>() }, vararg args: Any?, crossinline block: suspend U.() -> S, noinline condition: (S?, (T?) -> B) -> A, noinline avoid: (suspend (T?) -> Boolean)? = null, crossinline intercept: (T?) -> B, noinline determinant: ((A, B?) -> S)? = null): T =
    TODO()

private suspend inline fun <I : U, U : CoroutineScope, S, T : S, R> I.repeatBlockForValidResult(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, crossinline block: suspend U.() -> T, crossinline condition: (S?, (S?) -> Unit) -> BooleanType, noinline avoid: (suspend (S?) -> Boolean)? = null, noinline intercept: (S?) -> Unit, noinline validate: ((S?) -> R)? = null, crossinline reactToExposedInterceptorNotCalled: Work = ::rejectWithSecurityException): R {
    var value: S? = null
    var isAccepted = false
    var isIntercepted = false // used when intercept is exposed
    val determinant: (S?) -> Unit = {
        intercept(it)
        value = it
        isIntercepted = true }
    val avoid: suspend (S?) -> Boolean =
        avoid ?: { it.isNullValue() }
    val condition: suspend (S?) -> Boolean = {
        avoid(it)
        .onFalseValue {
            isIntercepted = false
            condition(it, determinant)
            .also {
                isAccepted = it.isTrue()
                if (intercept.isExposed and !isIntercepted) {
                    reactToExposedInterceptorNotCalled()
                    isIntercepted = false } } } }
    val validate: (S?) -> R =
        validate ?: { it.validate() }
    while (addedSuspendedIsActive(predicate)()) {
        if (!condition(value)) break
        condition(block())
        if (isAccepted)
            return validate(value)
        delayOrYield(delayTime()) }
    if (isAccepted)
        return validate(value)
    else
        rejectWithImplementationRestriction() }

private suspend inline fun <I : U, U : CoroutineScope, S, T : S, R> I.repeatBlockForValidResultUnless(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, crossinline block: suspend U.() -> T, noinline condition: (S?, (S?) -> Unit) -> BooleanType, noinline avoid: (suspend (S?) -> Boolean)? = null, noinline intercept: (S?) -> Unit, noinline validate: ((S?) -> R)? = null, crossinline reactToExposedInterceptorNotCalled: Work = ::rejectWithSecurityException) =
    repeatBlockForValidResult(predicate, delayTime, block, condition::isFalse, avoid, intercept, validate, reactToExposedInterceptorNotCalled)

private suspend inline fun <I : U, U : CoroutineScope, S, T : S, R, reified X : TimeoutCancellation> I.repeatBlockOrTimeoutForValidResult(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, noinline downTime: DelayFunction, msg: String? = null, cause: Throwable? = null, crossinline block: suspend U.() -> T, crossinline condition: (S?, (S?) -> Unit) -> BooleanType, noinline avoid: (suspend (S?) -> Boolean)? = null, noinline intercept: (S?) -> Unit, noinline validate: ((S?) -> R)? = null, crossinline reactToExposedInterceptorNotCalled: Work = ::rejectWithSecurityException): R = TODO()

private suspend inline fun <I : U, U : CoroutineScope, S, T : S, R, reified X : TimeoutCancellation> I.repeatBlockOrTimeoutForValidResultUnless(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, noinline downTime: DelayFunction, msg: String? = null, cause: Throwable? = null, crossinline block: suspend U.() -> T, noinline condition: (S?, (S?) -> Unit) -> BooleanType, noinline avoid: (suspend (S?) -> Boolean)? = null, noinline intercept: (S?) -> Unit, noinline validate: ((S?) -> R)? = null, crossinline reactToExposedInterceptorNotCalled: Work = ::rejectWithSecurityException): R = TODO()

private suspend inline fun <I : U, U : CoroutineScope, S, T : S, R, reified X : TimeoutCancellation> I.repeatBlockOrTimeoutForValidResult(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, noinline downTime: DelayFunction, crossinline ex: (AnyArray) -> X = { it.rejectAsException<X>() }, vararg args: Any?, crossinline block: suspend U.() -> T, crossinline condition: (S?, (S?) -> Unit) -> BooleanType, noinline avoid: (suspend (S?) -> Boolean)? = null, noinline intercept: (S?) -> Unit, noinline validate: ((S?) -> R)? = null, crossinline reactToExposedInterceptorNotCalled: Work = ::rejectWithSecurityException): R = TODO()

private suspend inline fun <I : U, U : CoroutineScope, S, T : S, R, reified X : TimeoutCancellation> I.repeatBlockOrTimeoutForValidResultUnless(noinline predicate: Prediction? = null, noinline delayTime: DelayFunction = NO_DELAY, noinline downTime: DelayFunction, crossinline ex: (AnyArray) -> X = { it.rejectAsException<X>() }, vararg args: Any?, crossinline block: suspend U.() -> T, noinline condition: (S?, (S?) -> Unit) -> BooleanType, noinline avoid: (suspend (S?) -> Boolean)? = null, noinline intercept: (S?) -> Unit, noinline validate: ((S?) -> R)? = null, crossinline reactToExposedInterceptorNotCalled: Work = ::rejectWithSecurityException): R = TODO()

@Suppress("NOTHING_TO_INLINE")
private inline fun Prediction?.addedNotTimedOut(noinline downTime: DelayFunction): Prediction? =
    this?.run { { downTime.isNotTimedOut() and invoke() } }

@Suppress("NOTHING_TO_INLINE")
private inline fun <I : CoroutineScope> I.addedIsActive(noinline predicate: (suspend I.() -> Boolean)?) =
    prepend(IS_ACTIVE, predicate)

@Suppress("NOTHING_TO_INLINE")
private inline fun <I : CoroutineScope> I.addedScopeIsActive(scope: CoroutineScope, noinline predicate: (suspend I.() -> Boolean)?) =
    prepend(scope::isActive, predicate)

@Suppress("NOTHING_TO_INLINE")
private inline fun <I : CoroutineScope> I.addedJobIsActive(job: Job, noinline predicate: (suspend I.() -> Boolean)?) =
    prepend(job::isActive, predicate)

@Suppress("NOTHING_TO_INLINE")
private inline fun <I : CoroutineScope> I.addedSuspendedIsActive(noinline predicate: Prediction?) =
    prepend(IS_ACTIVE, predicate)

@Suppress("NOTHING_TO_INLINE")
private inline fun <I : CoroutineScope> I.addedSuspendedIsActiveNotTimedOut(noinline downTime: DelayFunction, noinline predicate: Prediction?) =
    addedSuspendedIsActive(predicate.addedNotTimedOut(downTime))

@Suppress("NOTHING_TO_INLINE")
private inline fun <I : CoroutineScope> I.addedSuspendedScopeIsActive(scope: CoroutineScope, noinline predicate: Prediction?) =
    prepend(scope::isActive, predicate)

@Suppress("NOTHING_TO_INLINE")
private inline fun <I : CoroutineScope> I.addedSuspendedJobIsActive(job: Job, noinline predicate: Prediction?) =
    prepend(job::isActive, predicate)

@Suppress("NOTHING_TO_INLINE")
private inline fun <I : CoroutineScope, B : BooleanType> I.invokeAddedIsActive(noinline predicate: (suspend I.() -> B)?) =
    addedIsActive { predicate.isTrue() }

@Suppress("NOTHING_TO_INLINE")
private inline fun <I : CoroutineScope, B : BooleanType> I.invokeAddedScopeIsActive(scope: CoroutineScope, noinline predicate: (suspend I.() -> B)?) =
    addedScopeIsActive(scope) { predicate.isTrue() }

@Suppress("NOTHING_TO_INLINE")
private inline fun <I : CoroutineScope, B : BooleanType> I.invokeAddedJobIsActive(job: Job, noinline predicate: (suspend I.() -> B)?) =
    addedJobIsActive(job) { predicate.isTrue() }

@Suppress("NOTHING_TO_INLINE")
private inline fun <I : CoroutineScope, B : BooleanType> I.invokeAddedSuspendedIsActive(noinline predicate: (suspend () -> B)?) =
    addedSuspendedIsActive { predicate.isTrue() }

@Suppress("NOTHING_TO_INLINE")
private inline fun <I : CoroutineScope, B : BooleanType> I.invokeAddedSuspendedScopeIsActive(scope: CoroutineScope, noinline predicate: (suspend () -> B)?) =
    addedSuspendedScopeIsActive(scope) { predicate.isTrue() }

@Suppress("NOTHING_TO_INLINE")
private inline fun <I : CoroutineScope, B : BooleanType> I.invokeAddedSuspendedJobIsActive(job: Job, noinline predicate: (suspend () -> B)?) =
    addedSuspendedJobIsActive(job) { predicate.isTrue() }

context(_: I)
private fun <I : CoroutineScope, R> (suspend () -> R).implicitlySuspended(): suspend I.() -> R =
    { this@implicitlySuspended() }

context(_: I)
private suspend fun <I : CoroutineScope> I.delayOrYield(dt: Time) =
    onActive { SchedulerScope.delayOrYield(dt) }

context(_: I)
private suspend inline fun <I : CoroutineScope, reified T : TimeoutCancellation> I.delayOrTimeout(dt: Time, downtime: Time, msg: String?, cause: Throwable?) =
    onActive { SchedulerScope.delayOrTimeout<T>(dt, downtime, msg, cause) }

context(_: I)
private suspend inline fun <I : CoroutineScope, reified T : TimeoutCancellation> I.delayOrTimeout(dt: Time, downtime: Time, crossinline ex: (AnyArray) -> T, vararg args: Any?) =
    onActive { SchedulerScope.delayOrTimeout(dt, downtime, ex, *args) }

private suspend fun <I : CoroutineScope> I.onActive(block: suspend I.() -> Unit) {
    if (isActive) this.block() }

@Tag(UNDELAYED)
internal val NO_DELAY = suspend { no_delay }

@Tag(UNTIMED)
private val NO_TIMEOUT = suspend { no_timeout }

@Tag(UNYIELDING)
private val NO_YIELD = suspend { no_yield }

@Tag(MIN_DELAY)
private val VIEW_MIN_DELAY = suspend { view_min_delay }

@Tag(ACTIVE)
private val CoroutineScope.IS_ACTIVE get() = suspend { isActive }

private inline fun <I : CoroutineScope> I.prepend(noinline isActive: Prediction, noinline predicate: (suspend I.() -> Boolean)?, crossinline operator: BooleanOperator = Boolean::and) = with(isActive) {
    if (predicate !== null)
        toCoroutinePrediction<I>(predicate, operator)
    else
        toCoroutinePrediction<I>() }

private inline fun <I : CoroutineScope> I.prepend(noinline isActive: Prediction, noinline predicate: Prediction?, crossinline operator: BooleanOperator = Boolean::and) = with(isActive) {
    if (predicate !== null)
        toCoroutinePrediction<I>(predicate, operator)
    else
        toCoroutinePrediction<I>() }

private inline fun <I : CoroutineScope> I.prepend(isActive: BooleanKProperty, noinline predicate: (suspend I.() -> Boolean)?, crossinline operator: BooleanOperator = Boolean::and) = with(isActive) {
    if (predicate !== null)
        toCoroutinePrediction<I>(predicate, operator)
    else
        toCoroutinePrediction<I>() }

private inline fun <I : CoroutineScope> I.prepend(isActive: BooleanKProperty, noinline predicate: Prediction?, crossinline operator: BooleanOperator = Boolean::and) = with(isActive) {
    if (predicate !== null)
        toCoroutinePrediction<I>(predicate, operator)
    else
        toCoroutinePrediction<I>() }

private inline fun <I : CoroutineScope> Prediction.toCoroutinePrediction(noinline predicate: suspend I.() -> Boolean, crossinline operator: BooleanOperator): suspend I.() -> Boolean =
    { invoke().operator(predicate()) }

private inline fun <I : CoroutineScope> Prediction.toCoroutinePrediction(noinline predicate: Prediction, crossinline operator: BooleanOperator): suspend I.() -> Boolean =
    { invoke().operator(predicate()) }

private inline fun <I : CoroutineScope> BooleanKProperty.toCoroutinePrediction(noinline predicate: suspend I.() -> Boolean, crossinline operator: BooleanOperator): suspend I.() -> Boolean =
    { call().operator(predicate()) }

private inline fun <I : CoroutineScope> BooleanKProperty.toCoroutinePrediction(noinline predicate: Prediction, crossinline operator: BooleanOperator): suspend I.() -> Boolean =
    { call().operator(predicate()) }

@Suppress("NOTHING_TO_INLINE")
private inline fun <I : CoroutineScope> Prediction.toCoroutinePrediction(): suspend I.() -> Boolean = { invoke() }

@Suppress("NOTHING_TO_INLINE")
private inline fun <I : CoroutineScope> BooleanKProperty.toCoroutinePrediction(): suspend I.() -> Boolean = { call() }