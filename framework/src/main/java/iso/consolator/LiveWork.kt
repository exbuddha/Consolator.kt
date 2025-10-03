@file:JvmName("LiveWork")
@file:JvmMultifileClass

package iso.consolator

import androidx.lifecycle.*
import kotlin.coroutines.*
import kotlin.reflect.*
import iso.consolator.annotation.Asynchronous
import iso.consolator.annotation.TagType

internal typealias LiveWork = Triple<LiveStepPointer, CaptureFunction?, LiveWorkStateIdentityType>

internal typealias LiveCall = Triple<LiveStepKCallable, AnyKCallable, Annotations>

internal fun AnyTriple.toLiveWork(async: Boolean? = null): AnyTriple =
    runWhen({ isLiveCall }, {
    LiveWork(
        first.asLiveStepPointer(),
        second?.asVarKCallable()!!::call,
        async.isTrue() or isAsynchronous()) })

internal suspend fun AnyTriple.attach(tag: TagType? = null, owner: LifecycleOwner? = null) =
    Sequencer.attach(this, tag)

internal suspend fun AnyTriple.attachOnce(tag: TagType? = null, owner: LifecycleOwner? = null) =
    Sequencer.attachOnce(this, tag)

internal suspend fun AnyTriple.attachAfter(tag: TagType? = null, owner: LifecycleOwner? = null) =
    Sequencer.attachAfter(this, tag)

internal suspend fun AnyTriple.attachBefore(tag: TagType? = null, owner: LifecycleOwner? = null) =
    Sequencer.attachBefore(this, tag)

internal suspend fun AnyTriple.attachOnceAfter(tag: TagType? = null, owner: LifecycleOwner? = null) =
    Sequencer.attachOnceAfter(this, tag)

internal suspend fun AnyTriple.attachOnceBefore(tag: TagType? = null, owner: LifecycleOwner? = null) =
    Sequencer.attachOnceBefore(this, tag)

internal fun AnyTriple.detach() {}

internal fun AnyTriple.close() {}

// applying statement performs an operation on last marked livework and next chained item
// and returns the next livework attached in the resolved chain
private inline fun AnyTriple.applyToLiveWork(crossinline statement: LiveWorkPointer): AnyTriple = TODO()

private inline fun AnyTriple.applyToSequence(crossinline statement: LiveWorkFunction): AnyTriple = TODO()

private fun AnyTriple.attachConjunctionToLiveWork(operator: LiveWorkKFunction, target: SequencerStep) =
    applyToLiveWork { operator.call(run(AnyTriple::lastMarkedLiveWork), target) }

private fun AnyTriple.attachInterceptionToLiveWork(operator: LiveWorkKFunction, target: LiveWorkFunction) =
    applyToSequence { operator.call(run(AnyTriple::lastMarkedLiveWork), target) }

private fun AnyTriple.attachPredictionToLiveWork(operator: LiveWorkKFunction, predicate: LiveWorkPredicate) =
    applyToLiveWork { operator.call(run(AnyTriple::lastMarkedLiveWork), predicate) }

private fun AnyTriple.lastMarkedLiveWork(): AnyTriple = TODO()

internal infix fun AnyTriple.then(next: SequencerStep): AnyTriple = apply {
    attachConjunctionToLiveWork(
        AnyTriple::then, next) }

internal infix fun AnyTriple.thru(pass: LiveWorkFunction): AnyTriple = apply {
    attachInterceptionToLiveWork(
        AnyTriple::thru, pass) }

internal infix fun AnyTriple.after(prev: SequencerStep): AnyTriple = apply {
    attachConjunctionToLiveWork(
        AnyTriple::after, prev) }

internal infix fun AnyTriple.given(predicate: LiveWorkPredicate): AnyTriple = apply {
    attachPredictionToLiveWork(
        AnyTriple::given, predicate) }

internal infix fun AnyTriple.unless(predicate: LiveWorkPredicate): AnyTriple = apply {
    attachPredictionToLiveWork(
        AnyTriple::unless, predicate) }

internal infix fun AnyTriple.otherwise(next: SequencerStep): AnyTriple = apply {
    attachConjunctionToLiveWork(
        AnyTriple::otherwise, next) }

internal infix fun AnyTriple.onCancel(action: SequencerStep): AnyTriple = apply {
    attachConjunctionToLiveWork(
        AnyTriple::onCancel, action) }

internal infix fun AnyTriple.onError(action: SequencerStep): AnyTriple = apply {
    attachConjunctionToLiveWork(
        AnyTriple::onError, action) }

internal infix fun AnyTriple.onTimeout(action: SequencerStep): AnyTriple = apply {
    attachConjunctionToLiveWork(
        AnyTriple::onTimeout, action) }

internal fun <T, R> Pair<LiveData<T>, (T) -> R>.toLiveWork(async: Boolean = false) =
    LiveWork(
        { first.asType<LiveStep>() }.applyKeptOnce(),
        second.asType(),
        async)

internal fun <T, R> capture(context: CoroutineContext, step: suspend LiveDataScope<T>.() -> Unit, capture: (T) -> R) =
    liveData(context, block = step) to capture

internal fun <T, R> Pair<LiveData<T>, (T) -> R>.observe(owner: LifecycleOwner, observer: Observer<T> = run(::disposerOf)): Observer<T> {
    first.observe(owner, observer)
    return observer }

internal fun <T, R> Pair<LiveData<T>, (T) -> R>.dispose(owner: LifecycleOwner, disposer: Observer<T> = run(owner::disposerOf)) =
    observe(owner, disposer)

internal fun <T, R> Pair<LiveData<T>, (T) -> R>.observe(observer: Observer<T> = run(::disposerOf)): Observer<T> {
    first.observeForever(observer)
    return observer }

internal fun <T, R> Pair<LiveData<T>, (T) -> R>.observe(owner: LifecycleOwner, observer: (Pair<LiveData<T>, (T) -> R>) -> Observer<T> = ::disposerOf) =
    observe(owner, observer(this))

internal fun <T, R> Pair<LiveData<T>, (T) -> R>.observe(observer: (Pair<LiveData<T>, (T) -> R>) -> Observer<T> = ::disposerOf) =
    observe(observer(this))

internal fun <T, R> Pair<LiveData<T>, (T) -> R>.removeObserver(observer: Observer<T>) =
    first.removeObserver(observer)

internal fun <T, R> Pair<LiveData<T>, (T) -> R>.removeObservers(owner: LifecycleOwner) =
    first.removeObservers(owner)

internal fun <T, R> observerOf(liveStep: Pair<LiveData<T>, (T) -> R>) =
    Observer<T> { liveStep.second(it) }

private fun <T, R> disposerOf(liveStep: Pair<LiveData<T>, (T) -> R>) =
    object : Observer<T> {
        override fun onChanged(value: T) {
            val (step, capture) = liveStep
            run(step::removeObserver)
            capture(value) } }

private fun <T, R> LifecycleOwner.disposerOf(liveStep: Pair<LiveData<T>, (T) -> R>) =
    Observer<T> { value ->
        val (step, capture) = liveStep
        run(step::removeObservers)
        capture(value) }

internal fun AnyTriple.isAsynchronous() =
    determine(
        { third.asBooleanUnsafe() },
        { third.find { it is Asynchronous }
            .asType<Asynchronous>()?.enabled.isTrue() })

internal inline fun <A, B, C, S, R : S> Triple<A, B, C>.determine(work: LiveWork.() -> R, call: LiveCall.() -> R): S =
    if (isLiveWork)
        asLiveWorkUnsafe().work()
    else
        asLiveCallUnsafe().call()

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
internal inline fun <A, B, C, S, R : S> Triple<A, B, C>.capture(): S =
    if (isLiveWork)
        run((second as (Any?) -> R)::invoke)
    else
        run(second.asKCallable<R>()::call)

internal val AnyTriple.isLiveCall
    get() = first is AnyKCallable

internal val AnyTriple.isLiveWork
    get() = first !is AnyKCallable

internal fun Any?.asLiveWork() = asType<LiveWork>()
internal fun Any?.asLiveCall() = asType<LiveCall>()

internal fun Any?.asLiveWorkUnsafe() = asTypeUnsafe<LiveWork>()
internal fun Any?.asLiveCallUnsafe() = asTypeUnsafe<LiveCall>()

private typealias LiveWorkPointer = AnyTriplePointer
private typealias LiveWorkFunction = AnyTripleFunction
internal typealias LiveWorkPredicate = (AnyTriple) -> PredicateIdentityType

internal typealias LiveWorkStateIdentityType = BooleanType

internal typealias LiveStepKCallable = KCallable<LiveStep?>
internal typealias LiveWorkKFunction = KFunction<AnyTriple>