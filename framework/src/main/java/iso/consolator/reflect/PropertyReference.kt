package iso.consolator.reflect

import kotlin.reflect.KCallable
import kotlin.reflect.KProperty

@Suppress("warnings")
sealed class PropertyReference<R>(target: KCallable<R>) : CallableReference<R>(target), KProperty<R> {
    internal open class Stub<R>(target: KCallable<R>) : PropertyReference<R>(target) {
        internal constructor(source: R) : this(TODO("retrieve annotations from source object"))
    }

    override fun requireReference(): KProperty<R> = super.requireReference() as KProperty<R>

    override val getter = requireReference().getter
    override val isConst = requireReference().isConst
    override val isLateinit = requireReference().isLateinit
}