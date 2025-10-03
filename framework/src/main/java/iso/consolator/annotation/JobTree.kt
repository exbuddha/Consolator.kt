package iso.consolator.annotation

import iso.consolator.LevelType
import iso.consolator.PathType
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

@Retention(SOURCE)
@Target(ANNOTATION_CLASS, CONSTRUCTOR, FUNCTION, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER)
internal annotation class JobTree(
    val branch: PathType,
    val level: LevelType = 0)