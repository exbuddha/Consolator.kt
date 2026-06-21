package iso.consolator.component

import abs.consolator.MutableGrid
import iso.consolator.AnyKMutableProperty
import iso.consolator.Lock
import iso.consolator.Resolver
import iso.consolator.State
import iso.consolator.asAnyFunction
import iso.consolator.asTypeUnsafe
import iso.consolator.invokeWhenNotIgnored
import iso.consolator.isFalse
import iso.consolator.isNot
import iso.consolator.setInstance

interface MemoryManager : Resolver {
    context(_: Any)
    fun commit(level: Int) = Unit

    fun expire(property: AnyKMutableProperty) {
        // must be strengthened by connecting to other expiry sets
        forEach { alive ->
        if (alive(property).isFalse() and
            (State of property isNot Lock.Closed))
            property.expire() } }

    companion object Expiry : Set<Lifetime> {
        private lateinit var grid: MutableGrid<Entry>

        override fun iterator(): MutableIterator<Lifetime> = TODO()

        override fun contains(element: Lifetime) = false

        override fun containsAll(elements: LifetimeElements) = false

        override fun isEmpty() = true

        @JvmStatic override val size: Int
            get() = 0

        private interface Entry {
            val code: Int
        }

        /** Expires when parent expires. */
        private interface ChildEntry : Entry {
            val parent: Int?

            class Instance(override val code: Int, override val parent: Int?) : ChildEntry
        }

        /** Expires when all siblings expire. */
        private interface SiblingEntry : Entry {
            fun addSibling(code: Int)

            class Instance(override val code: Int) : SiblingEntry {
                override fun addSibling(code: Int) {}
            }
        }

        /** Expires either when parent or all siblings expire. */
        private interface RelativeEntry : ChildEntry, SiblingEntry {
            class Instance(override val code: Int, override val parent: Int?) : RelativeEntry {
                override fun addSibling(code: Int) {}
            }
        }
    }

    override fun commit(vararg context: Any?): Unit? =
        context.lastOrNull().asAnyFunction()?.invokeWhenNotIgnored()
}

internal fun Any.asMemoryManager(): MemoryManager = asTypeUnsafe<MemoryManager>()

internal fun AnyKMutableProperty.expire(): Unit = setInstance(null)

private typealias Lifetime = (AnyKMutableProperty) -> Boolean?
private typealias LifetimeElements = Collection<Lifetime>