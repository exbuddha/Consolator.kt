package iso.consolator.annotation

import iso.consolator.typeIs
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

@Retention(SOURCE)
@Target(ANNOTATION_CLASS, CLASS, CONSTRUCTOR, FUNCTION, PROPERTY, PROPERTY_GETTER)
annotation class Tag(
    val id: TagType,
    val keep: Boolean = true)

internal fun filterIsTag(it: Any) = it.typeIs<Tag, _>()

internal const val no_tag = 0

internal typealias TagConversion = (TagType) -> TagType
typealias TagType = Int /* symbolic to hashCode() */