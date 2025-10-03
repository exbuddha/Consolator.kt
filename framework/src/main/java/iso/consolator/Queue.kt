@file:JvmName(JVM_CLASS_NAME)
@file:JvmMultifileClass

package iso.consolator

internal sealed interface PriorityQueue<E> {
    var queue: MutableList<E>
}

internal abstract class Queue<out E> : List<E>

internal fun <E> List<E>.expire(element: E) = Unit