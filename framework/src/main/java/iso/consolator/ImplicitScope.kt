@file:JvmName(JVM_CLASS_NAME)
@file:JvmMultifileClass

package iso.consolator

import kotlin.reflect.KCallable
import kotlin.reflect.KClass

internal typealias DefaultScope = ImplicitScope<Any>
internal typealias DefaultCallableScope = CallableScope<Any?>
internal typealias ImplicitResultScope = ResultScope<Any?>

internal sealed interface ImplicitScope<in S> {
    // set S or Implication<T> as context parameter
    fun <T> S.implicitly(): KCallable<T>

    fun <T> withLazy(callable: KCallable<T>, block: T.(KCallable<T>) -> Any?): ImplicitScope<in S> {
        // route from current scope to callable reference in order to find intent for the block call
        callable.call().block(callable)
        return this }

    companion object : DefaultScope, Implication<Any?> {
        override fun <T> Any.implicitly(): KCallable<T> = asReference().intercept()

        @Suppress("UNCHECKED_CAST")
        override fun <S : AnyKCallable> AnyKCallable.intercept(vararg args: Any?) = this as S
    }
}

internal sealed interface Implication<in T> : Interceptor<KCallable<T>> {
    override fun <S : KCallable<T>> KCallable<T>.intercept(vararg args: Any?): S
}

// enables masking for addressability
internal sealed interface ResultScope<in R> : Implication<R> {
    override fun <S : KCallable<R>> KCallable<R>.intercept(vararg args: Any?): S = TODO()

    fun <T : Any> by(type: KClass<out T>): Implication<R> = this
}

internal sealed interface ActualResult
internal sealed interface TransactionalResult

internal sealed interface CallableScope<in R> : ResultScope<R>

internal interface ImplicitCallableScope : DefaultCallableScope, ImplicitScope<AnyKCallable> {
    override fun <T> AnyKCallable.implicitly(): KCallable<T> = asTypeUnsafe()

    companion object : ImplicitCallableScope
}

internal sealed interface DeterminedContext

internal sealed interface SelectiveContext<in S> : ImplicitScope<KCallable<S>>, DeterminedContext {
    override fun <T> KCallable<S>.implicitly(): KCallable<T> = TODO()
}

internal fun <T, S : () -> T> select(vararg pointer: S?, predicate: (S?) -> Boolean = ::trueWhenNotNull, condition: (T?) -> Boolean = ::trueWhenNotNull) =
    pointer.select(Array<out S?>::find, predicate, condition)

internal fun <T, S : () -> T> selectLast(vararg pointer: S?, predicate: (S?) -> Boolean = ::trueWhenNotNull, condition: (T?) -> Boolean = ::trueWhenNotNull) =
    pointer.select(Array<out S?>::findLast, predicate, condition)

internal inline fun <I : U, U, T, S : (() -> T)?> I.select(operator: U.((S) -> Boolean) -> S, noinline predicate: (S) -> Boolean = ::trueWhenNotNull, noinline condition: (T?) -> Boolean = ::trueWhenNotNull): T? {
    var value: T? = null
    val condition: (T) -> Boolean = {
        condition(it)
        .alsoOnTrue { value = it } }
    operator {
        it !== null && predicate(it) and condition(it()) }
    return value }

internal inline fun <I : U, U, K, T : K, S : (() -> T)?> I.select(operator: U.((S) -> Boolean) -> S, noinline predicate: (S, ((T?) -> Any?)?) -> Boolean = { it, _ -> trueWhenNotNull(it) }, noinline condition: (K?) -> Boolean = ::trueWhenNotNull, lock: Any? = null, getter: S? = null, noinline setter: ((T?) -> Any?)? = null, noinline isolate: ((K?) -> T?)? = null, determinant: S? = null): T? {
    var value: T? = null
    val accept: (T?) -> Unit =
        { value = it }
    val receive =
        isolate?.run {
            { it.run(::invoke)
                .run(accept) } }
        ?: accept
    val get =
        getter ?: { value }
    val set =
        setter?.run {
            { it.apply(::invoke)
                .apply(receive) } }
        ?: receive
    val filter: (S) -> T? = {
        predicate(it, set)
        .then(get) }
    val block =
        lock?.run { {
            synchronized(lock) { filter(it) } } }
        ?: filter
    operator {
        block(it).let { value ->
            condition(value)
            .alsoOnTrue { set(value) } } }
    return (determinant ?: get)() }