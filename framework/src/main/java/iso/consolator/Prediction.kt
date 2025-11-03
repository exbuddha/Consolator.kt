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

internal inline infix fun Int.onPositiveValue(block: Int.() -> Int): Int =
    runWhen(::isPositive, block)

internal inline infix fun Long.onPositiveValue(block: Long.() -> Long): Long =
    runWhen(::isPositive, block)

internal inline infix fun Long.onNegativeValue(block: Long.() -> Long): Long =
    runWhen(::isNegative, block)

internal inline infix fun <T, R : T> T.onNullValue(block: T.() -> R): T =
    runWhen(::isNull, block)

internal inline infix fun <T, R : T> T.onNonNullValue(block: T.() -> R): T =
    runWhen(::isNotNull, block)

inline infix fun <T, R> T?.letResult(block: T.(T) -> R): R? =
    this?.let { block(it) }

internal inline infix fun <T, R> T?.result(block: T.() -> R): R? =
    if (this !== null) block() else null

internal inline fun <T, R> T.resultWhen(predicate: T.() -> Boolean, block: T.() -> R): R? =
    if (run(predicate)) block() else null

internal inline fun <T, R, S> T.resultWhen(predicate: T.() -> Boolean, block: T.() -> R, fallback: T.() -> S): Any? =
    if (run(predicate)) block() else fallback()

internal inline fun <T, R> T.resultUnless(predicate: T.() -> Boolean, block: T.() -> R): R? =
    if (run(predicate)) null else block()

internal inline fun <T, R, S> T.resultUnless(predicate: T.() -> Boolean, block: T.() -> R, fallback: T.() -> S): Any? =
    resultWhen(predicate, fallback, block)

inline fun <T, R : T> T.runWhen(predicate: (T) -> Boolean, block: T.() -> R): T =
    runWhen(predicate, block) { this }

inline fun <T, R : T> T.runUnless(predicate: (T) -> Boolean, block: T.() -> R): T =
    runUnless(predicate, block) { this }

inline fun <T, R : T, S : T> T.runWhen(predicate: (T) -> Boolean, block: T.() -> R, fallback: T.() -> S): T =
    if (run(predicate)) block() else fallback()

inline fun <T, R : T, S : T> T.runUnless(predicate: (T) -> Boolean, block: T.() -> R, fallback: T.() -> S): T =
    runWhen(predicate, fallback, block)

internal inline fun <reified K, reified T, R : T, S : T> T.runWhenType(noinline predicate: (T) -> Boolean, crossinline block: K.() -> R, fallback: T.() -> S): T =
    if (this is K && predicate(this)) block() else fallback()

internal inline fun <reified K, reified T, R : T, S : T> T.runUnlessType(noinline predicate: (T) -> Boolean, block: T.() -> R, crossinline fallback: K.() -> S): T =
    if (this !is K && predicate(this)) block() else fallback(this as K)

internal inline fun Boolean.onTrueValue(block: UnitFunction): Boolean =
    runWhen({ this }) { block(); true }

internal inline fun Boolean.onFalseValue(block: UnitFunction): Boolean =
    runUnless({ this }) { block(); false }

internal inline fun <T> T.onTrueValue(block: UnitFunction): T =
    runWhen(Any?::isTrue) { block(); this }

internal inline fun <T> T.onFalseValue(block: UnitFunction): T =
    runUnless(Any?::isFalse) { block(); this }

internal inline fun <T, R> T.whenTrueValue(block: T.() -> R): T =
    onTrueValue { block() }

internal inline fun <T, R> T.whenFalseValue(block: T.() -> R): T =
    onFalseValue { block() }

internal inline infix fun <R> Boolean.alsoOnTrue(block: () -> R): Boolean = also {
    if (this) block() }

internal inline infix fun <R> Boolean.alsoOnFalse(block: () -> R): Boolean =
    not().alsoOnTrue(block)

internal inline infix fun <R> Boolean.then(block: () -> R): R? =
    resultWhen({ this }) { block() }

internal inline infix fun <R> Boolean.otherwise(block: () -> R): R? =
    not().then(block)

private inline fun <reified K, T> typeIsBefore(noinline predicate: (T) -> Boolean, noinline operator: BooleanOperator = Boolean::and): (T) -> Boolean =
    predicate.after({ it.typeIs<K, _>() })

private inline fun <reified K, T> typeIsAfter(noinline predicate: (T) -> Boolean, noinline operator: BooleanOperator = Boolean::and): (T) -> Boolean =
    predicate.before({ it.typeIs<K, _>() })

private inline fun <reified K, T> typeIsNotBefore(noinline predicate: (T) -> Boolean, noinline operator: BooleanOperator = Boolean::and): (T) -> Boolean =
    predicate.after({ it.typeIsNot<K, _>() })

private inline fun <reified K, T> typeIsNotAfter(noinline predicate: (T) -> Boolean, noinline operator: BooleanOperator = Boolean::and): (T) -> Boolean =
    predicate.before({ it.typeIsNot<K, _>() })

inline fun <reified T, O> O.typeIs(it: O = this, post: T.() -> Boolean = { true }): Boolean =
    if (it is T) it.post() else false

inline fun <reified T, O> O.typeIsNot(it: O = this, post: O.() -> Boolean = { true }): Boolean =
    if (it !is T) it.post() else false

@Suppress("NOTHING_TO_INLINE")
private inline fun <T> ((T) -> Boolean).before(noinline other: (T) -> Boolean, noinline operator: BooleanOperator = Boolean::and): (T) -> Boolean =
    { invoke(it).operator(other(it)) }

@Suppress("NOTHING_TO_INLINE")
private inline fun <T> ((T) -> Boolean).after(noinline other: (T) -> Boolean, noinline operator: BooleanOperator = Boolean::and): (T) -> Boolean =
    run(other::before)

internal inline infix fun <R> Predicate.then(block: () -> R): R? =
    invoke().then(block)

internal inline infix fun <R> Predicate.otherwise(block: () -> R): R? =
    not().then(block)

internal fun Predicate.not(): Boolean = invoke().not()

fun Predicate.isTrue(): Boolean = invoke()

fun Predicate.isFalse(): Boolean = invoke().not()

internal fun BooleanPointer.invokeIsTrue(): Boolean = invoke().isTrue()

internal fun BooleanPointer.invokeIsFalse(): Boolean = invoke().isFalse()

internal fun <P1> ((P1) -> Boolean).not(p1: P1): Boolean = invoke(p1).not()

internal fun <P1, P2> ((P1, P2) -> Boolean).not(p1: P1, p2: P2): Boolean = invoke(p1, p2).not()

internal fun <P1, P2> ((P1, P2) -> BooleanType).isTrue(p1: P1, p2: P2): Boolean = invoke(p1, p2).isTrue()

internal fun <P1, P2> ((P1, P2) -> BooleanType).isFalse(p1: P1, p2: P2): Boolean = invoke(p1, p2).isFalse()

internal fun BooleanKFunction.isTrue(): Boolean = call()

internal fun BooleanKFunction.isFalse(): Boolean = call().not()

internal fun isPositive(it: Int): Boolean = it > 0
internal fun isNegative(it: Int): Boolean = it < 0

internal fun isPositive(it: Long): Boolean = it > 0
internal fun isNegative(it: Long): Boolean = it < 0

internal fun <T> isNull(it: T): Boolean = it isObject null
internal fun <T> isNotNull(it: T): Boolean = it isNotObject null

fun <T> T.isNullValue(): Boolean = run(::isNull)
fun <T> T.isNotNullValue(): Boolean = run(::isNotNull)

internal val Any?.isKCallable: Boolean get() = typeIs<AnyKCallable, _>()

internal fun Any?.asBoolean(): Boolean? = asType<Boolean>()
internal fun Any?.asBooleanUnsafe(): Boolean = asTypeUnsafe<Boolean>()
internal fun Any?.asBooleanType(): BooleanType? = asType<BooleanType>()

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