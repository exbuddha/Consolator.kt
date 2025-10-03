@file:JvmName(JVM_CLASS_NAME)
@file:JvmMultifileClass

package iso.consolator

import android.os.Message
import iso.consolator.AdjustOperator.Element.*
import kotlin.reflect.*
import kotlinx.coroutines.*
import iso.consolator.annotation.TagType

internal fun Runnable.detach(): Runnable? = null

private fun Runnable.close() {}

internal fun FunctionSet.saveRunnable(self: AnyKCallable, tag: TagType) =
    save(self, tag, Item.Type.Runnable)

internal open class RunnableItem<R>(target: KCallable<R>) : CoroutineItem<R>(target), Adjustable.By<Any, ClockIndex>, Runnable {
    init {
        type = Type.Runnable }

    override fun onAttach(index: ClockIndex): RunnableItem<R> {
        super.onAttach(index)
        onSaveIndex(index)
        return this }

    override fun onAttachBy(container: Any): RunnableItem<R> {
        super.onAttachBy(container)
        return this }

    override fun run() {
        target?.call().asWork()?.invoke() }
}

internal fun Any?.asRunnableItem() = asType<RunnableItem<*>>()

// applying statement performs an operation on last marked message and next chained item
// and returns the next runnable attached in the resolved chain
private inline fun Runnable.applyToRunnable(crossinline statement: RunnablePointer): Runnable = TODO()

internal fun Runnable.attachConjunctionToRunnable(target: Runnable, operation: (Message, Runnable) -> Runnable) =
    applyToRunnable { operation(run(::lastMarkedMessage), target) }

internal fun Runnable.attachPredictionToRunnable(predicate: MessagePredicate, operation: (Message, MessagePredicate) -> Runnable) =
    applyToRunnable { operation(run(::lastMarkedMessage), predicate) }

private fun Runnable.lastMarkedMessage(): Message = TODO()

internal fun Runnable.asStep() =
    run(Clock::getStep) ?: toStep()

private fun Runnable.asMessage() = withClock {
    run(::getAnyCoroutine)?.run(::getMessage) }

private fun Runnable.toCoroutine(): CoroutineStep = { run() }
private fun Runnable.toStep() = suspend { run() }

private typealias RunnablePointer = () -> Runnable
private typealias RunnableFunction = (Runnable) -> Any?
private typealias RunnablePredicate = (Runnable) -> PredicateIdentityType

// step <-> runnable
internal fun handle(step: AnyCoroutineStep) = post(runnableOf(step))
internal fun handleAhead(step: AnyCoroutineStep) = postAhead(runnableOf(step))
internal fun handleSafely(step: CoroutineStep) = post(safeRunnableOf(step))
internal fun handleAheadSafely(step: CoroutineStep) = postAhead(safeRunnableOf(step))
internal fun handleInterrupting(step: CoroutineStep) = post(interruptingRunnableOf(step))
internal fun handleAheadInterrupting(step: CoroutineStep) = postAhead(interruptingRunnableOf(step))
internal fun handleSafelyInterrupting(step: CoroutineStep) = post(safeInterruptingRunnableOf(step))
internal fun handleAheadSafelyInterrupting(step: CoroutineStep) = postAhead(safeInterruptingRunnableOf(step))

internal fun Runnable.asCoroutine() =
    run(Clock::getCoroutine) ?: toCoroutine()

// step <-> runnable
internal fun reinvoke(step: Step) = post(runnableOf(step))
internal fun reinvokeAhead(step: Step) = postAhead(runnableOf(step))
internal fun reinvokeSafely(step: Step) = post(safeRunnableOf(step))
internal fun reinvokeAheadSafely(step: Step) = postAhead(safeRunnableOf(step))
internal fun reinvokeInterrupting(step: Step) = post(interruptingRunnableOf(step))
internal fun reinvokeAheadInterrupting(step: Step) = postAhead(interruptingRunnableOf(step))
internal fun reinvokeSafelyInterrupting(step: Step) = post(safeInterruptingRunnableOf(step))
internal fun reinvokeAheadSafelyInterrupting(step: Step) = postAhead(safeInterruptingRunnableOf(step))

internal fun <T> blockOf(step: suspend CoroutineScope.() -> T): () -> T = { runBlocking(block = step) }
internal fun <T> runnableOf(step: suspend CoroutineScope.() -> T) = Runnable { runBlocking(block = step) }
internal fun <T> safeRunnableOf(step: suspend CoroutineScope.() -> T) = Runnable { trySafely(blockOf(step)) }
internal fun <T> interruptingRunnableOf(step: suspend CoroutineScope.() -> T) = Runnable { tryInterrupting(step = step) }
internal fun <T> safeInterruptingRunnableOf(step: suspend CoroutineScope.() -> T) = Runnable { trySafelyInterrupting(step = step) }

internal fun <T> blockOf(step: suspend () -> T): () -> T = step::block
internal fun <T> runnableOf(step: suspend () -> T) = Runnable { step.block() }
internal fun <T> safeRunnableOf(step: suspend () -> T) = Runnable { trySafely(blockOf(step)) }
internal fun <T> interruptingRunnableOf(step: suspend () -> T) = Runnable { tryInterrupting(block = blockOf(step)) }
internal fun <T> safeInterruptingRunnableOf(step: suspend () -> T) = Runnable { trySafelyInterrupting(blockOf(step)) }

internal fun <R> (suspend () -> R).block() = runBlocking { invoke() }
internal fun <T, R> (suspend T.() -> R).block(scope: T) = runBlocking { invoke(scope) }
internal fun <T, U, R> (suspend T.(U) -> R).block(scope: T, value: U) = runBlocking { invoke(scope, value) }
internal fun <T, U, R> (suspend T.(U) -> R).block(scope: () -> T, value: U) = runBlocking { invoke(scope(), value) }
internal fun <T, U, R> (suspend T.(U) -> R).block(scope: KCallable<T>, value: U) = runBlocking { invoke(scope.call(), value) }

internal fun Any?.asRunnable() = asType<Runnable>()

internal typealias RunnableKCallable = KCallable<Runnable>
private typealias RunnableKFunction = KFunction<Runnable>