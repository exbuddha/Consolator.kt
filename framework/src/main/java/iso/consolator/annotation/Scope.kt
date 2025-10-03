package iso.consolator.annotation

import iso.consolator.AnyKClass
import iso.consolator.Scheduler
import iso.consolator.typeIs
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

@Retention(SOURCE)
@Target(ANNOTATION_CLASS, CLASS, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER)
annotation class Scope(
    val type: KClass<out CoroutineScope> = Scheduler::class,
    val provider: AnyKClass = Any::class)

internal fun filterIsScope(it: Any) = it.typeIs<Scope, _>()