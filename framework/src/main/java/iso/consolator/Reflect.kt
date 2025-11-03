@file:JvmName("Reflect")
@file:JvmMultifileClass

package iso.consolator

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.*

fun findClass(className: String): KClass<*> = Class.forName(className).kotlin

internal inline fun <reified T> AnyArray.runCall(callable: KCallable<T>): T =
    callable.call(*this)

internal inline fun <reified T : Any> AnyArray.asNew(): T =
    T::class.new(*this)

internal inline fun <reified T : Any> Any?.asTypeOf(instance: KCallable<T>): T? =
    run(instance.call()::class::safeCast)

internal inline fun <reified T : Any> Any?.asTypeOf(obj: T): T? =
    run(obj::class::safeCast)

internal inline fun <reified T : Any> Any?.asType(): T? =
    run(T::class::safeCast)

internal inline fun <reified T : Any> Any?.asTypeUnsafe(): T =
    run(T::class::cast)

internal inline fun <reified T : Any> T.findClassMemberFunction(noinline predicate: AnyKFunctionPredicate): AnyKFunction? =
    T::class.findMemberFunction(predicate)

inline fun <reified T : Any> T.qualifiedClassName(): String? =
    T::class.qualifiedName

internal val <T : Any> KClass<out T>.companion: T
    get() = objectInstance as T

internal val <T : Any> KClass<out T>.lock: Any
    get() = objectInstance ?: safeLock

internal val <T : Any> KClass<out T>.safeLock: KClass<out T>
    get() = this

internal inline fun <reified T : Any> KClass<out T>.reconstruct(vararg args: Any?): T =
    if (isCompanion) objectInstance!!
    else new(*args)

fun <T : Any> KClass<out T>.newDefault(): T =
    emptyConstructor.call()

fun <T : Any> KClass<out T>.newObject(constructor: (VarArray) -> T = { new(*it) }, vararg args: Any?): T =
    if (args.isEmpty()) newDefault()
    else constructor(args)

fun <T : Any> KClass<out T>.new(vararg args: Any?): T =
    if (args.isEmpty()) newDefault()
    else callFirstConstructor(*args)

private fun <T : Any> KClass<out T>.callFirstConstructor(vararg args: Any?): T =
    firstConstructor.call(*args)

internal fun <T : Any> KClass<out T>.findMemberFunction(predicate: AnyKFunctionPredicate): AnyKFunction? =
    memberFunctions.find(predicate)

internal val <T : Any> KClass<out T>.emptyConstructor: KFunction<T>
    get() = constructors.first { it.parameters.isEmpty() }

internal val <T : Any> KClass<out T>.firstConstructor: KFunction<T>
    get() = constructors.first()

internal fun <T> KMutableProperty<out T?>.setInstance(value: T?): Unit =
    setter.call(value)

fun <T> KProperty<T?>.getInstance(): T? =
    getter.call()

internal inline fun <T : Any, reified R : T> KProperty<T?>.getAsTypeUnsafe(): R =
    getInstance().asTypeUnsafe<R>()

internal fun <V> KProperty<V>.toPointer(): () -> V = getter::call.toPointer()

internal fun <R> KFunction<R>.toPointer(): () -> R = { call() }

private fun <R> KFunction<R>.returnType(): KClass<*> = returnType.jvmErasure

private fun <R> KFunction<R>.nullType(): Any? = Unit.nullType(returnType())

internal typealias AnyKFunctionPredicate = (AnyKFunction) -> Boolean

internal fun <T> Any?.asKCallable(): KCallable<T> = asTypeUnsafe<KCallable<T>>()
internal fun <T> Any?.asKProperty(): KProperty<T> = asTypeUnsafe<KProperty<T>>()
internal fun <T> Any?.asKMutableProperty(): KMutableProperty<T> = asTypeUnsafe<KMutableProperty<T>>()

internal fun Any?.asAnyKCallable(): AnyKCallable? = asType<AnyKCallable>()
internal fun Any?.asVarKCallable(): VarKCallable = asTypeUnsafe<VarKCallable>()
internal fun Any?.asAnyKProperty(): AnyKCallable? = asType<AnyKCallable>()
internal fun Any?.asVarKProperty(): VarKCallable = asTypeUnsafe<VarKCallable>()
internal fun Any?.asAnyKMutableProperty(): AnyKMutableProperty? = asType<AnyKMutableProperty>()
internal fun Any?.asVarKMutableProperty(): VarKMutableProperty = asTypeUnsafe<VarKMutableProperty>()

internal fun AnyKCallable.asKCallable(): AnyKCallable? = asType<AnyKCallable>()
internal fun AnyKCallable.asKProperty(): AnyKProperty? = asType<AnyKProperty>()
internal fun AnyKCallable.asKMutableProperty(): AnyKMutableProperty? = asType<AnyKMutableProperty>()

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