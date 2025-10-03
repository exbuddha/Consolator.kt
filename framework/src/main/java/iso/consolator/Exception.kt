@file:JvmName(JVM_CLASS_NAME)
@file:JvmMultifileClass

package iso.consolator

import iso.consolator.exception.*
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope

sealed interface ErrorMap<X : Throwable, out R> : Map<X, R>

sealed interface ErrorMapEntry<in X : Throwable, out R> : Map.Entry<(X?) -> Boolean, (Boolean) -> R> {
    fun <X : Throwable, R> ErrorMapEntry<X, R>.byType(type: KClass<X>): Boolean
}

internal inline fun <X : U, U : Throwable, reified R> ErrorMap<U, R>.transform(err: X): R = TODO()

internal typealias ErrorConvertor = Pair<ExitPredicate, AnySuspendFunction>
internal typealias ErrorTypeConvertor = Pair<(ThrowableKClass) -> Boolean, AnySuspendFunction>
internal typealias ErrorConversionMap = Array<ErrorConvertor>
internal typealias ErrorTypeConversionMap = Array<ErrorTypeConvertor>
internal typealias ErrorOperator = Pair<ExitPredicateType, BooleanOperator>
internal typealias ErrorTransformer = Pair<ExitPredicateType, BooleanConversion>

private inline fun <reified T : Throwable, R> T?.runWhen(predicate: (T?) -> Boolean, block: () -> R): R = TODO()

internal inline fun <reified T : Throwable, R> tryCatching(block: () -> R, predicate: ThrowablePredicate = { isType<T, _>(it) }, exit: ThrowableNothing = ::throwIt) =
    try { block() }
    catch (ex: Throwable) {
    if (predicate(ex)) exit(ex)
    else throwIt(ex) }

internal inline fun <reified T : Throwable, reified U : Throwable, R> tryMapping(transform: Throwable.(Throwable, Int) -> U = { _, _ -> U::class.new(message, cause) }, block: () -> R) =
    tryCatching<T, _>(block) { with(it) { throwIt(transform(this, 0)) } }

internal inline fun <reified T : Throwable, reified U : Throwable, R> tryFlatMapping(block: () -> R) =
    tryMapping<T, _, _>({ _, _ -> cause?.throwException<U>() ?: U::class.new() }, block)

internal inline fun <reified T : Throwable, reified U : Throwable, R> tryOuterMapping(block: () -> R) =
    tryCatching<T, _>(block, { it.typeIsNot<T, _>() }) { with(it) { throwException<U>() } }

internal inline fun <reified T : Throwable, reified U : Throwable, R> tryOuterFlatMapping(block: () -> R) =
    tryCatching<T, _>(block, { it.typeIsNot<T, _>() }) { it.cause?.throwException<U>() ?: throwIt(U::class.new()) }

internal inline fun <reified T : Throwable, R, S : R> tryMapping(block: () -> R, predicate: ThrowablePredicate = { isType<T, _>(it) }, transform: (Throwable) -> S) =
    try { block() }
    catch (ex: Throwable) {
    if (predicate(ex)) transform(ex)
    else throwIt(ex) }

internal inline fun <reified T : Throwable, R> tryBypassing(block: () -> R) =
    tryMapping<T, _, _>(block) { null }

internal inline fun <R> tryAvoiding(block: () -> R) =
    try { block() } catch (_: Propagate) {}

internal inline fun <R, S : R> tryPropagating(block: () -> R, transform: (Throwable) -> S) =
    try { block() }
    catch (ex: Propagate) { throwIt(ex) }
    catch (ex: Throwable) { transform(ex) }

inline fun <R> trySafely(block: () -> R) =
    try { block() } catch (_: Throwable) {}

inline fun <R> trySafelyForResult(block: () -> R) =
    try { block() } catch (_: Throwable) { null }

internal inline fun <R, S : R> tryFinally(block: () -> R, final: (R?) -> S): R {
    var result: R? = null
    return try { block().also { result = it } }
    catch(ex: Throwable) { throwIt(ex) }
    finally { final(result) } }

internal inline fun <R, S : R> tryFinallyForResult(block: () -> R, final: (R?) -> S): R? {
    var result: R? = null
    return try { block().also { result = it } }
    catch(_: Throwable) { null }
    finally { final(result) } }

inline fun <R> tryCanceling(msg: String? = null, block: () -> R) =
    try { block() }
    catch (ex: Throwable) { throwIt(CancellationException(msg, ex)) }

suspend inline fun <R> tryCancelingSuspended(msg: String? = null, crossinline block: suspend () -> R) =
    tryCanceling(msg) { block() }

internal inline fun <R> trySafelyCanceling(block: () -> R) =
    tryCancelingForResult(block)

internal inline fun <R> tryCancelingForResult(block: () -> R, exit: (Throwable) -> R? = { null }) =
    try { block() }
    catch (ex: CancellationException) { throwIt(ex) }
    catch (ex: Throwable) { exit(ex) }

internal inline fun <R> tryInterrupting(msg: String? = null, block: () -> R) =
    try { block() }
    catch (ex: Throwable) { rejectWithException<InterruptedPathException>(msg, ex) }

@Suppress("NOTHING_TO_INLINE")
internal inline fun <R> tryInterrupting(msg: String? = null, noinline step: suspend CoroutineScope.() -> R) =
    try { blockOf(step)() }
    catch (ex: Throwable) { rejectWithException<InterruptedStepException>(step, msg, ex) }

internal inline fun <R> trySafelyInterrupting(block: () -> R) =
    try { block() }
    catch (ex: InterruptedException) { throwIt(ex) }
    catch (_: Throwable) {}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <R> trySafelyInterrupting(msg: String? = null, noinline step: suspend CoroutineScope.() -> R) =
    try { blockOf(step)() }
    catch (ex: InterruptedException) { rejectWithException<InterruptedStepException>(step, msg, ex) }
    catch (_: Throwable) {}

internal inline fun <R> tryInterruptingForResult(msg: String? = null, noinline step: suspend CoroutineScope.() -> R, exit: (Throwable) -> R? = { null }) =
    try { blockOf(step)() }
    catch (ex: InterruptedException) { rejectWithException<InterruptedStepException>(step, msg, ex) }
    catch (ex: Throwable) { exit(ex) }

internal inline fun <reified X : Throwable, T, R : T> runOrRejectWithException(vararg args: Any?, predicate: () -> Boolean, block: () -> R): T {
    if (predicate()) return block()
    rejectWithException<X>(*args) }

inline fun <reified X : Throwable, T, R : T> runOrRejectWithException(ex: (AnyArray) -> X, vararg args: Any?, predicate: () -> Boolean, block: () -> R): T {
    if (predicate()) return block()
    rejectWithException<X>(ex, *args) }

internal inline fun <reified X : Throwable, T, R : T> runUnlessOrRejectWithException(vararg args: Any?, noinline predicate: () -> Boolean, block: () -> R) =
    runOrRejectWithException<X, _, _>(*args, predicate = predicate::not, block = block)

internal inline fun <reified X : Throwable, T, R : T> runUnlessOrRejectWithException(ex: (AnyArray) -> X, vararg args: Any?, noinline predicate: () -> Boolean, block: () -> R) =
    runOrRejectWithException<X, _, _>(ex, *args, predicate = predicate::not, block = block)

@Throws
internal fun rejectWithIllegalStateException(): Nothing = rejectWithException<IllegalStateException>()

@Throws
fun rejectWithImplementationRestriction(): Nothing = rejectWithException<BaseImplementationRestriction>()

@Throws
internal fun rejectWithSecurityException(): Nothing = rejectWithException<SecurityException>()

@Throws
inline fun <reified X : Throwable> rejectWithException(vararg args: Any?): Nothing =
    throwIt(X::class.new(*args))

@Throws
inline fun <reified X : Throwable> rejectWithException(ex: (AnyArray) -> X, vararg args: Any?): Nothing =
    throwIt(ex(args))

@Throws
internal inline fun <reified X : Throwable> AnyArray.rejectAsException(): Nothing =
    rejectWithException<X>(*this)

@Throws
internal inline fun <reified X : Throwable> Throwable.throwException(): Nothing =
    rejectWithException<X>(message, cause)

@Throws
fun throwIt(ex: Throwable): Nothing = throw ex

internal typealias ThrowablePredicate = (Throwable) -> Boolean
internal typealias ExitPredicate = (Throwable?) -> Boolean
internal typealias ExitPredicateType = (Throwable?) -> BooleanType
internal typealias ExitWork = (Throwable?) -> Unit
internal typealias ThrowableNothing = (Throwable) -> Nothing

internal typealias ThrowableKClass = KClass<out Throwable>