@file:JvmName("Reflection")
@file:JvmMultifileClass

package iso.consolator

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.*

internal inline fun <reified T> AnyArray.runCall(callable: KCallable<T>) =
    callable.call(*this)

internal inline fun <reified T : Any> AnyArray.asNew() =
    T::class.new(*this)

internal inline fun <reified T : Any> Any?.asTypeOf(instance: KCallable<T>) =
    run(instance.call()::class::safeCast)

internal inline fun <reified T : Any> Any?.asTypeOf(obj: T) =
    run(obj::class::safeCast)

internal inline fun <reified T : Any> Any?.asType() =
    run(T::class::safeCast)

internal inline fun <reified T : Any> Any?.asTypeUnsafe() =
    run(T::class::cast)

internal inline fun <reified T : Any> T.findClassMemberFunction(noinline predicate: AnyKFunctionPredicate) =
    T::class.findMemberFunction(predicate)

inline fun <reified T : Any> T.qualifiedClassName() =
    T::class.qualifiedName

internal val <T : Any> KClass<out T>.companion
    get() = objectInstance as T

internal val <T : Any> KClass<out T>.lock
    get() = objectInstance ?: safeLock

internal val <T : Any> KClass<out T>.safeLock
    get() = this

internal inline fun <reified T : Any> KClass<out T>.reconstruct(vararg args: Any?) =
    if (isCompanion) objectInstance!!
    else new(*args)

fun <T : Any> KClass<out T>.newDefault() =
    emptyConstructor.call()

fun <T : Any> KClass<out T>.newObject(constructor: (VarArray) -> T = { new(*it) }, vararg args: Any?) =
    if (args.isEmpty()) newDefault()
    else constructor(args)

fun <T : Any> KClass<out T>.new(vararg args: Any?) =
    if (args.isEmpty()) newDefault()
    else callFirstConstructor(*args)

private fun <T : Any> KClass<out T>.callFirstConstructor(vararg args: Any?) =
    firstConstructor.call(*args)

internal fun <T : Any> KClass<out T>.findMemberFunction(predicate: AnyKFunctionPredicate) =
    memberFunctions.find(predicate)

internal val <T : Any> KClass<out T>.emptyConstructor
    get() = constructors.first { it.parameters.isEmpty() }

internal val <T : Any> KClass<out T>.firstConstructor
    get() = constructors.first()

internal fun <T> KMutableProperty<out T?>.setInstance(value: T?) =
    setter.call(value)

fun <T> KProperty<T?>.getInstance() =
    getter.call()

internal inline fun <T : Any, reified R : T> KProperty<T?>.getAsTypeUnsafe() =
    getInstance().asTypeUnsafe<R>()

internal fun <V> KProperty<V>.toPointer(): () -> V = getter::call.toPointer()

internal fun <R> KFunction<R>.toPointer(): () -> R = { call() }

private fun <R> KFunction<R>.returnType() = returnType.jvmErasure

private fun <R> KFunction<R>.nullType() = Unit.nullType(returnType())

internal typealias AnyKFunctionPredicate = (AnyKFunction) -> Boolean

internal fun <T> Any?.asKCallable() = asTypeUnsafe<KCallable<T>>()
internal fun <T> Any?.asKProperty() = asTypeUnsafe<KProperty<T>>()
internal fun <T> Any?.asKMutableProperty() = asTypeUnsafe<KMutableProperty<T>>()

internal fun Any?.asAnyKCallable() = asType<AnyKCallable>()
internal fun Any?.asVarKCallable() = asTypeUnsafe<VarKCallable>()
internal fun Any?.asAnyKProperty() = asType<AnyKCallable>()
internal fun Any?.asVarKProperty() = asTypeUnsafe<VarKCallable>()
internal fun Any?.asAnyKMutableProperty() = asType<AnyKMutableProperty>()
internal fun Any?.asVarKMutableProperty() = asTypeUnsafe<VarKMutableProperty>()

internal fun AnyKCallable.asKCallable() = asType<AnyKCallable>()
internal fun AnyKCallable.asKProperty() = asType<AnyKProperty>()
internal fun AnyKCallable.asKMutableProperty() = asType<AnyKMutableProperty>()

typealias AnyKClass = KClass<*>
typealias AnyKFunction = KFunction<*>
internal typealias AnyKCallable = KCallable<*>
internal typealias VarKCallable = KCallable<Any?>
internal typealias AnyKProperty = KProperty<*>
internal typealias VarKProperty = KProperty<Any?>
typealias AnyKMutableProperty = KMutableProperty<*>
typealias VarKMutableProperty = KMutableProperty<out Any?>
internal typealias AnyKParameterMap = Map<KParameter, *>
internal typealias KParameterMap = Map<KParameter, Any?>