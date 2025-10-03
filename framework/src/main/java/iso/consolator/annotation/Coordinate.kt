package iso.consolator.annotation

import iso.consolator.AnyKClass
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

@Retention(SOURCE)
@Target(ANNOTATION_CLASS, CLASS, CONSTRUCTOR, FUNCTION, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER)
annotation class Coordinate(
    val target: AnyKClass = Any::class,
    val key: KeyType = 0)