@file:JvmName(JVM_CLASS_NAME)
@file:JvmMultifileClass

package iso.consolator

import kotlinx.coroutines.*
import kotlin.reflect.*

internal typealias DefaultScope = ImplicitScope<Any>
internal typealias DefaultCallableScope = CallableScope<Any?>
internal typealias ImplicitResultScope = ResultScope<Any?>

internal sealed interface ImplicitScope<in S> {
    context(_: Implication<T>)
    fun <T> S.implicitly(): KCallable<T>

    context(scope: CoroutineScope)
    fun <T> withLazy(callable: KCallable<T>, block: T.(KCallable<T>) -> Any?): CoroutineScope {
        // route from current scope, such as SchedulerScope, to callable reference in order to find intent for the block call
        callable.call().block(callable)
        return scope }

    companion object : DefaultScope, Implication<Any?> {
        context(_: Implication<T>)
        override fun <T> Any.implicitly(): KCallable<T> = asReference().intercept()

        @Suppress("UNCHECKED_CAST")
        override fun <S : AnyKCallable> AnyKCallable.intercept(vararg args: Any?) = this as S
    }
}

sealed interface Implication<in T> : Interceptor<KCallable<T>> {
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
    context(_: Implication<T>)
    override fun <T> AnyKCallable.implicitly(): KCallable<T> = this.asTypeUnsafe()

    companion object : ImplicitCallableScope
}

internal sealed interface DeterminedContext

internal sealed interface SelectiveContext<in S> : ImplicitScope<KCallable<S>>, DeterminedContext {
    context(_: Implication<T>)
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