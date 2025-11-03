@file:JvmName("Interception")
@file:JvmMultifileClass

package iso.consolator

sealed interface Interceptor<in T> {
    fun <S : T> T.intercept(vararg args: Any?): S

    companion object : BaseInterceptor, Validator {
        override fun <S : Any?> Any?.intercept(vararg args: Any?): S =
            result { Unit.nullType(this::class) }
            .validate<_, S>()

        override operator fun invoke(step: Interception?): BaseInterceptor =
            super.invoke(step)

        @JvmStatic override operator fun invoke(): BaseInterceptor =
            super.invoke(null)
    }

    operator fun invoke(step: Interception? = null): Interceptor<T> = this

    operator fun invoke(): Any = Unit
}

internal typealias BaseInterceptor = Interceptor<Any?>
internal typealias Interception = Interceptor<*>.() -> Unit