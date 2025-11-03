@file:JvmName(JVM_CLASS_NAME)
@file:JvmMultifileClass

package ctx.consolator

import android.content.Context
import java.lang.ref.WeakReference

/**
 * A referred context in android OS.
 *
 * @property ref the weak reference.
 */
interface ReferredContext {
    var ref: WeakContext?

    fun <T : Context> WeakReference<out T>?.receive(context: T) =
        this ?: WeakReference(context)
}

/** Acquires the weak reference to the context. */
fun Context.asWeakReference() =
    if (this is ReferredContext) ref!!
    else run(::WeakReference)

typealias WeakContext = WeakReference<out Context>

/**
 * A uniquely identified context.
 *
 * @property uid the context unique ID.
 */
interface UniqueContext<ID> {
    var uid: ID

    /**
     * The instance of the unique context.
     *
     * @property uid the context unique time.
     */
    interface Instance : UniqueContext<Long> {
        override var uid: Long
    }
}

/**
 * Returns the current local calendar time.
 */
external fun now(): Long

const val JVM_CLASS_NAME = abs.consolator.JVM_CLASS_NAME