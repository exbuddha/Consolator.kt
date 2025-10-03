@file:JvmName("Prediction")
@file:JvmMultifileClass

package iso.consolator

import kotlin.reflect.*

fun interface SuspendCondition<in T> : suspend (T) -> Boolean
fun interface SuspendConditionType<in T> : suspend (T) -> BooleanType

fun interface SuspendPredicate : suspend () -> Boolean
fun interface SuspendPredication : suspend () -> BooleanType

fun interface Condition<in T> : (T) -> Boolean
fun interface ConditionType<in T> : (T) -> BooleanType

fun interface BooleanFunction : () -> Boolean
fun interface BooleanTypeFunction : () -> BooleanType

internal inline infix fun Int.onPositiveValue(block: Int.() -> Int) =
    runWhen(::isPositive, block)

internal inline infix fun Long.onPositiveValue(block: Long.() -> Long) =
    runWhen(::isPositive, block)

internal inline infix fun Long.onNegativeValue(block: Long.() -> Long) =
    runWhen(::isNegative, block)

internal inline infix fun <T, R : T> T.onNullValue(block: T.() -> R) =
    runWhen(::isNull, block)

internal inline infix fun <T, R : T> T.onNonNullValue(block: T.() -> R) =
    runWhen(::isNotNull, block)

inline infix fun <T, R> T?.letResult(block: T.(T) -> R) =
    this?.let { block(it) }

internal inline infix fun <T, R> T?.result(block: T.() -> R) =
    if (this !== null) block() else null

internal inline fun <T, R> T.resultWhen(predicate: T.() -> Boolean, block: T.() -> R) =
    if (run(predicate)) block() else null

internal inline fun <T, R, S> T.resultWhen(predicate: T.() -> Boolean, block: T.() -> R, fallback: T.() -> S): Any? =
    if (run(predicate)) block() else fallback()

internal inline fun <T, R> T.resultUnless(predicate: T.() -> Boolean, block: T.() -> R) =
    if (run(predicate)) null else block()

internal inline fun <T, R, S> T.resultUnless(predicate: T.() -> Boolean, block: T.() -> R, fallback: T.() -> S): Any? =
    resultWhen(predicate, fallback, block)

inline fun <T, R : T> T.runWhen(predicate: (T) -> Boolean, block: T.() -> R) =
    runWhen(predicate, block) { this }

inline fun <T, R : T> T.runUnless(predicate: (T) -> Boolean, block: T.() -> R) =
    runUnless(predicate, block) { this }

inline fun <T, R : T, S : T> T.runWhen(predicate: (T) -> Boolean, block: T.() -> R, fallback: T.() -> S) =
    if (run(predicate)) block() else fallback()

inline fun <T, R : T, S : T> T.runUnless(predicate: (T) -> Boolean, block: T.() -> R, fallback: T.() -> S) =
    runWhen(predicate, fallback, block)

internal inline fun <reified K, reified T, R : T, S : T> T.runWhenType(noinline predicate: (T) -> Boolean, crossinline block: K.() -> R, fallback: T.() -> S) =
    if (this is K && predicate(this)) block() else fallback()

internal inline fun <reified K, reified T, R : T, S : T> T.runUnlessType(noinline predicate: (T) -> Boolean, block: T.() -> R, crossinline fallback: K.() -> S) =
    if (this !is K && predicate(this)) block() else fallback(this as K)

internal inline fun Boolean.onTrueValue(block: Work) =
    runWhen({ this }) { block(); true }

internal inline fun Boolean.onFalseValue(block: Work) =
    runUnless({ this }) { block(); false }

internal inline fun <T> T.onTrueValue(block: Work) =
    runWhen(Any?::isTrue) { block(); this }

internal inline fun <T> T.onFalseValue(block: Work) =
    runUnless(Any?::isFalse) { block(); this }

internal inline fun <T, R> T.whenTrueValue(block: T.() -> R) =
    onTrueValue { block() }

internal inline fun <T, R> T.whenFalseValue(block: T.() -> R) =
    onFalseValue { block() }

internal inline infix fun <R> Boolean.alsoOnTrue(block: () -> R) = also {
    if (this) block() }

internal inline infix fun <R> Boolean.alsoOnFalse(block: () -> R) =
    not().alsoOnTrue(block)

internal inline infix fun <R> Boolean.then(block: () -> R) =
    resultWhen({ this }) { block() }

internal inline infix fun <R> Boolean.otherwise(block: () -> R) =
    not().then(block)

private inline fun <reified K, T> typeIsBefore(noinline predicate: (T) -> Boolean, noinline operator: BooleanOperator = Boolean::and) =
    predicate.after({ it.typeIs<K, _>() })

private inline fun <reified K, T> typeIsAfter(noinline predicate: (T) -> Boolean, noinline operator: BooleanOperator = Boolean::and) =
    predicate.before({ it.typeIs<K, _>() })

private inline fun <reified K, T> typeIsNotBefore(noinline predicate: (T) -> Boolean, noinline operator: BooleanOperator = Boolean::and) =
    predicate.after({ it.typeIsNot<K, _>() })

private inline fun <reified K, T> typeIsNotAfter(noinline predicate: (T) -> Boolean, noinline operator: BooleanOperator = Boolean::and) =
    predicate.before({ it.typeIsNot<K, _>() })

inline fun <reified T, O> O.typeIs(it: O = this, post: T.() -> Boolean = { true }) =
    if (it is T) it.post() else false

inline fun <reified T, O> O.typeIsNot(it: O = this, post: O.() -> Boolean = { true }) =
    if (it !is T) it.post() else false

@Suppress("NOTHING_TO_INLINE")
private inline fun <T> ((T) -> Boolean).before(noinline other: (T) -> Boolean, noinline operator: BooleanOperator = Boolean::and): (T) -> Boolean =
    { invoke(it).operator(other(it)) }

@Suppress("NOTHING_TO_INLINE")
private inline fun <T> ((T) -> Boolean).after(noinline other: (T) -> Boolean, noinline operator: BooleanOperator = Boolean::and): (T) -> Boolean =
    run(other::before)

internal inline infix fun <R> Predicate.then(block: () -> R) =
    invoke().then(block)

internal inline infix fun <R> Predicate.otherwise(block: () -> R) =
    not().then(block)

internal fun Predicate.not() = invoke().not()

fun Predicate.isTrue() = invoke()

fun Predicate.isFalse() = invoke().not()

internal fun BooleanPointer.invokeIsTrue() = invoke().isTrue()

internal fun BooleanPointer.invokeIsFalse() = invoke().isFalse()

internal fun <P1> ((P1) -> Boolean).not(p1: P1) = invoke(p1).not()

internal fun <P1, P2> ((P1, P2) -> Boolean).not(p1: P1, p2: P2) = invoke(p1, p2).not()

internal fun <P1, P2> ((P1, P2) -> BooleanType).isTrue(p1: P1, p2: P2) = invoke(p1, p2).isTrue()

internal fun <P1, P2> ((P1, P2) -> BooleanType).isFalse(p1: P1, p2: P2) = invoke(p1, p2).isFalse()

internal fun BooleanKFunction.isTrue() = call()

internal fun BooleanKFunction.isFalse() = call().not()

private fun isPositive(it: Int) = it > 0
private fun isNegative(it: Int) = it < 0

private fun isPositive(it: Long) = it > 0
private fun isNegative(it: Long) = it < 0

internal fun <T> isNull(it: T) = it isObject null
internal fun <T> isNotNull(it: T) = it isNotObject null

fun <T> T.isNullValue() = run(::isNull)
fun <T> T.isNotNullValue() = run(::isNotNull)

internal val Any?.isKCallable get() = typeIs<AnyKCallable, _>()

internal fun Any?.asBoolean() = asType<Boolean>()
internal fun Any?.asBooleanUnsafe() = asTypeUnsafe<Boolean>()
internal fun Any?.asBooleanType() = asType<BooleanType>()

internal typealias Predicate = () -> Boolean
internal typealias BooleanPointer = () -> Boolean?
internal typealias BooleanOperator = Boolean.(Boolean) -> Boolean
internal typealias BooleanType = Comparable<Boolean>
internal typealias BooleanWork = (Boolean) -> Unit
internal typealias BooleanConversion = (Boolean) -> Any

internal typealias BooleanKFunction = KFunction<Boolean>
internal typealias BooleanKProperty = KProperty<Boolean>

internal typealias PredicateIdentityType = Boolean
internal typealias PredictionIdentityType = BooleanType