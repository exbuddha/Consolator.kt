package iso.consolator

import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext
import iso.consolator.AttachOperator.Element.Attachable
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

// coordinates with capture function queue for scheduling purposes
internal sealed interface Synchronizer<L> : Coordinator<L, L> {
    override fun <R> synchronize(lock: L?, block: () -> R) =
        synchronized(lock!!, block)
}

private fun synchronizer(block: SynchronizerStep) {}

private inline fun <L, reified R : Any> Synchronizer<L>.synchronize(lock: L?, crossinline predicate: () -> Boolean, crossinline block: () -> R) =
    synchronize(lock) {
        if (predicate()) block()
        else Unit.nullType<R>() }

private inline fun <L : Any> Synchronizer<L>.synchronize(lock: L, crossinline predicate: (L) -> Boolean, crossinline block: Work) =
    synchronize(lock) {
        if (predicate(lock)) block() }

@Suppress("NOTHING_TO_INLINE")
private inline fun <L : Any>  Synchronizer<L>.synchronizeByFunction(lock: L, predicate: BooleanKFunction, noinline block: Work) =
    synchronize(lock, predicate::isTrue, block)

internal sealed interface LiveStepSynchronizer<T> : Synchronizer<suspend () -> T>

internal fun <L> Any.asSynchronizer() = asTypeUnsafe<Synchronizer<L>>()

enum class Lock : LockState {
    Open { override fun invoke() = TODO() /* callbacks group reconfiguration */ },
    Closed { override fun invoke() = TODO() /* callbacks group reconfiguration */ };

    @Retention(SOURCE)
    @Target(CLASS, CONSTRUCTOR, FUNCTION, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER)
    annotation class Shared(
        /* sets sensitivity for locks within function - specifically, how they are used in a code block */
        val inside: Boolean = false,
        val outside: Boolean = false,
        val topmost: Boolean = false,
        val onepass: Boolean = false)
}

internal inline fun commitAsync(lock: Any, predicate: Predicate, block: Work) {
    if (predicate())
        synchronized(lock) {
            if (predicate()) block() } }

internal inline fun <L : Any> commitAsync(lock: L, predicate: (L) -> Boolean, block: Work) {
    if (predicate(lock))
        synchronized(lock) {
            if (predicate(lock)) block() } }

internal inline fun commitAsyncUnless(lock: Any, noinline predicate: ObjectPredicate, block: Work) =
    commitAsync(lock, predicate::not, block)

internal inline fun commitAsyncByFunction(lock: Any, predicate: BooleanKFunction, block: Work) {
    if (predicate.call())
        commitAsync(lock, predicate::isTrue, block) }

internal inline fun commitAsyncByFunctionUnless(lock: Any, predicate: BooleanKFunction, block: Work) =
    commitAsyncByFunction(lock, predicate::isFalse, block)

internal inline fun <R, S : R> commitAsyncForResult(lock: Any, predicate: ObjectPredicate, crossinline block: () -> R, crossinline fallback: () -> S? = { null }): R? {
    commitAsync(lock, predicate) { return block() }
    return fallback() }

internal inline fun <R, S : R> commitAsyncForResultUnless(lock: Any, noinline predicate: ObjectPredicate, crossinline block: () -> R, crossinline fallback: () -> S? = { null }) =
    commitAsyncForResult(lock, predicate::not, block)

internal inline fun <R> commitAsyncOrFallback(lock: Any, predicate: ObjectPredicate, crossinline block: () -> R, crossinline fallback: Work = {}): R? {
    commitAsync(lock, predicate) { return block() }
    fallback()
    return null }

internal inline fun <R> commitAsyncOrFallbackUnless(lock: Any, noinline predicate: ObjectPredicate, crossinline block: () -> R, crossinline fallback: Work = {}) =
    commitAsyncOrFallback(lock, predicate::not, block, fallback)

internal inline fun <R> commitLocked(lock: Any, predicate: ObjectPredicate, crossinline block: () -> R, crossinline fallback: BooleanWork = {}) =
    commitLockedForResult(lock, predicate, block) { fallback(it); null }

internal inline fun <R> commitLockedUnless(lock: Any, noinline predicate: ObjectPredicate, crossinline block: () -> R, crossinline fallback: BooleanWork = {}) =
    commitLocked(lock, predicate::not, block)

internal inline fun <R, S : R> commitLockedForResult(lock: Any, predicate: ObjectPredicate, crossinline block: () -> R, crossinline fallback: (Boolean) -> S? = { null }) =
    commitAsyncForResult(lock, predicate,
        { return@commitAsyncForResult block() },
        { fallback(false) })

internal inline fun <R, S : R> commitLockedForResultUnless(lock: Any, noinline predicate: ObjectPredicate, crossinline block: () -> R, crossinline fallback: (Boolean) -> S? = { null }) =
    commitLockedForResult(lock, predicate::not, block)

internal inline fun Any.commitLocked(predicate: ObjectPredicate, block: Work) =
    commitAsync(this, predicate, block)

internal inline fun Any.commitLockedUnless(noinline predicate: ObjectPredicate, block: Work) =
    commitLocked(predicate::not, block)

@Suppress("NOTHING_TO_INLINE")
internal inline fun Any.commitLockedByFunction(predicate: BooleanKFunction, noinline block: Work) =
    commitAsyncByFunction(this, predicate, block)

@Suppress("NOTHING_TO_INLINE")
internal inline fun Any.commitLockedByFunctionUnless(predicate: BooleanKFunction, noinline block: Work) =
    commitLockedByFunction(predicate::isFalse, block)

internal sealed interface AttachOperator<in S> {
    fun attach(step: S, vararg args: Any?): Any?

    sealed interface Element {
        interface Attachable<I> : Element {
            fun onAttach(index: I) = this

            interface By<in S, I> : Attachable<I> {
                fun onAttachBy(container: S) = this
            }
        }

        interface Observable : Element {
            fun onObserve(job: Job, index: Int, context: CoroutineContext?) = this
        }
    }
}

internal sealed interface AdjustOperator<in S, I> : AttachOperator<S> {
    fun attach(index: I, step: S, vararg args: Any?): Any?
    fun adjust(index: I): I

    sealed interface Element : AttachOperator.Element {
        interface Adjustable<I> : Element, Attachable<I> {
            interface By<in S, I> : Adjustable<I>, Attachable.By<S, I>
        }
    }
}

private typealias SynchronizerStep = Synchronizer<*>.() -> Unit
private typealias AnySynchronizerStep = Synchronizer<*>.() -> Any?
private typealias StateFunction = Synchronizer<*>.(State) -> Unit
private typealias AnyStateFunction = Synchronizer<*>.(State) -> Any?