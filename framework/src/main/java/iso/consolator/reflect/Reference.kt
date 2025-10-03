package iso.consolator.reflect

import iso.consolator.mapToTypedArray
import iso.consolator.AnnotationsMap
import iso.consolator.KParameterMap
import kotlin.reflect.KCallable

@Suppress("warnings")
sealed class Reference<R>(target: KCallable<R>) : KCallable<R> by target {
    private var ref: KCallable<R>? = null
    private var pointer: (() -> R)? = null

    init { setSource(target) }

    internal open fun setSource(source: R): Reference<R> {
        TODO("retrieve annotations from source object")
        pointer = { source }
        return this }

    protected fun setSource(target: KCallable<R>? = null): Reference<R> {
        target?.apply(::ref::set)
        return this }

    internal var extraAnnotations: AnnotationsMap? = null

    internal open class Stub<R>(target: KCallable<R>) : Reference<R>(target) {
        internal constructor(source: R) : this(TODO("retrieve annotations from source object"))
    }

    internal companion object {
        // useful in cases where invoke operator is explicitly annotated
        protected fun <R> (() -> R).asFunction(): KCallable<R> = ::invoke
    }

    protected open fun requireReference() = ref ?: this

    override fun call(vararg args: Any?) = requireReference().call(*args)
    override fun callBy(args: KParameterMap) = call(*mapToTypedArray(args))
    override val annotations = requireReference().annotations
    override val isAbstract = requireReference().isAbstract
    override val isFinal = requireReference().isFinal
    override val isOpen = requireReference().isOpen
    override val isSuspend = requireReference().isSuspend
    override val name = requireReference().name
    override val parameters = requireReference().parameters
    override val returnType = requireReference().returnType
    override val typeParameters = requireReference().typeParameters
    override val visibility = requireReference().visibility
}