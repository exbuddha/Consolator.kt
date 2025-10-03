package iso.consolator

import kotlinx.coroutines.CoroutineScope

internal object HandlerScope : ResolverScope {
    fun <T> transactor(): CallableTransactor<T> = TODO()

    override fun commit(step: AnyCoroutineStep) =
        attach(step, ::handle)

    lateinit var threads: List<Thread>

    // loop functions (external-facing scopes) - program context

    internal fun <I : CoroutineScope, U : ResolverScope, B : suspend (U) -> S, S : ActiveState.BooleanTypeState<*>> I.resolve(scope: I = this, block: B): S =
        TODO()

    private tailrec suspend fun <I : CoroutineScope, U : ResolverScope, B : suspend (U) -> S, S : Responsive<B>, R : S> I.loop(scope: I = this, block: B): R =
        loop(scope, block)
}