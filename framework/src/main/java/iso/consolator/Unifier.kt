@file:JvmName("Unification")
@file:JvmMultifileClass

package iso.consolator

import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*
import kotlin.reflect.*

// code unification will cover code injections, preferences, selections, and other areas of interest

// sets granularity for parts of code blocks inside controller units of logic that allow it
// cooperativeness is one good use case
// granularity and throughput together with device's processing speed set the fluidity of the program logic at runtime
internal enum class Granularity { Maximal, Blocked, Linear, Coarse, Hard, Soft, Minimal }

internal sealed interface Unifier

internal inline fun <T, S, reified R : Any> Unifier.unifyObject(obj: T, convert: (T) -> S, transform: (S) -> R) =
    transform(convert(obj))

internal inline fun <T, S, reified R : Any> Unifier.unifyTypeObject(obj: T, convert: (T) -> S?, transform: (S) -> R?): R? =
    unifyObject<_, _, R>(obj, convert) {
        transform(it ?: return R::class.asConvertedNull())
        ?: return R::class.asTransformedNull() }

@Retention(SOURCE)
@Target(PROPERTY)
internal annotation class Converted

@Retention(SOURCE)
@Target(PROPERTY)
internal annotation class Transformed

internal inline fun <reified T : Any> KClass<out T>.asConvertedNull() = Unit.nullType<T>()

internal inline fun <reified T : Any> KClass<out T>.asTransformedNull() = Unit.nullType<T>()

internal inline fun <reified T : Any> Unit.nullType(cls: KClass<out T> = T::class) = null.asType<T>()

@Suppress("UNCHECKED_CAST")
internal fun <R> Unit.type() = this as R

internal typealias UnitKCallable = KCallable<Unit>
internal typealias UnitKFunction = KFunction<Unit>