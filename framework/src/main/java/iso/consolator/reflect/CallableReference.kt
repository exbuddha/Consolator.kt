package iso.consolator.reflect

import kotlin.reflect.KCallable

sealed class CallableReference<R>(target: KCallable<R>) : Reference<R>(target) {
    internal open class Stub<R>(target: KCallable<R>) : CallableReference<R>(target) {
        internal constructor(source: R) : this(TODO("retrieve annotations from source object"))
    }
}