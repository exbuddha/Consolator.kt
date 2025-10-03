package iso.consolator.reflect

import iso.consolator.getInstance
import iso.consolator.annotation.Tag
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

// provides one-way access to set
interface ReceiverItem<in T> {
    fun <R> ReceiverItem<R>.set(value: R): ReceiverItem<T>

    @Suppress("UNCHECKED_CAST")
    fun <R> setTo(target: KProperty<R?>) =
        with(this) { set(value = target.getInstance() as T) }

    fun <R> setTo(target: KProperty<R?>, tag: Tag?) =
        setTo(target).also { /* define tag locally, implicitly, or in source */ }

    interface Stub<in T : Any> : ReceiverItem<T> {
        val type: KClass<in T>

        fun setType(type: KClass<out T>)
    }
}