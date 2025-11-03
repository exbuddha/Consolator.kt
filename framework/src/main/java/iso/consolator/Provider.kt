@file:JvmName("Provider")
@file:JvmMultifileClass

package iso.consolator

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import kotlin.reflect.*
import iso.consolator.annotation.TagType

interface FunctionProvider {
    fun <R> provide(vararg tag: TagType): KCallable<R>
}

fun interface ObjectProvider {
    fun provide(type: AnyKClass): Any
}

interface ObjectType<out T : Any> {
    val type: KClass<out T>
}

internal fun LifecycleOwner.provide(cls: AnyKClass): Any =
    asAnyProvide(cls)

internal fun Activity.provide(cls: AnyKClass): Any =
    asAnyProvide(cls)

internal fun Context.provide(cls: AnyKClass): Any =
    asAnyProvide(cls)

internal fun Any.provide(cls: AnyKClass): Any =
    asObjectProvider()!!.provide(cls)

internal fun Any?.asContextProvide(cls: AnyKClass): Any? =
    asContext()?.provide(cls)

internal fun Any.asAnyProvide(cls: AnyKClass): Any =
    provide(cls)

sealed interface Addressed<R> {
    val target: KCallable<R>?
    fun setTarget(target: KCallable<R>): Addressed<R>
}

sealed interface Tagged {
    val tag: TagType?
    fun setTag(tag: TagType): Tagged
}

fun Any?.asFunctionProvider(): FunctionProvider? = asType<FunctionProvider>()
fun Any?.asObjectProvider(): ObjectProvider? = asType<ObjectProvider>()

internal typealias Provider = (AnyKClass) -> Any
internal typealias CallableProvider = (Array<out TagType>) -> AnyKCallable