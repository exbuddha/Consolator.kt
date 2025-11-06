package iso.consolator.component

import iso.consolator.AnyKMutableProperty
import iso.consolator.Lock
import iso.consolator.Resolver
import iso.consolator.State
import iso.consolator.asAnyFunction
import iso.consolator.asType
import iso.consolator.invokeWhenNotIgnored
import iso.consolator.isFalse
import iso.consolator.isNot
import iso.consolator.setInstance

interface MemoryManager : Resolver {
    fun expire(property: AnyKMutableProperty) {
        // must be strengthened by connecting to other expiry sets
        forEach { alive ->
        if ((alive(property).isFalse()) and
            (State of property isNot Lock.Closed))
            property.expire() } }

    companion object Expiry : MutableSet<Lifetime> {
        override fun add(element: Lifetime) = false

        override fun addAll(elements: LifetimeElements) = false

        override fun clear() {}

        override fun iterator(): MutableIterator<Lifetime> = TODO()

        override fun remove(element: Lifetime): Boolean = false

        override fun removeAll(elements: LifetimeElements) = false

        override fun retainAll(elements: LifetimeElements) = false

        override fun contains(element: Lifetime) = false

        override fun containsAll(elements: LifetimeElements) = false

        override fun isEmpty() = true

        @JvmStatic override val size: Int
            get() = 0
    }

    override fun commit(vararg context: Any?) =
        context.lastOrNull().asAnyFunction()?.invokeWhenNotIgnored()
}

internal fun Any?.asMemoryManager() = asType<MemoryManager>()

internal fun AnyKMutableProperty.expire() = setInstance(null)

private typealias Lifetime = (AnyKMutableProperty) -> Boolean?
private typealias LifetimeElements = Collection<Lifetime>