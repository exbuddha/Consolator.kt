package iso.consolator.annotation

import iso.consolator.AnyKClass
import iso.consolator.SchedulerNode
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

@Retention(SOURCE)
@Target(ANNOTATION_CLASS, CONSTRUCTOR, FUNCTION, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER)
internal annotation class Synchronous(
    val node: SchedulerNode = Annotation::class,
    val group: AnyKClass = Any::class)