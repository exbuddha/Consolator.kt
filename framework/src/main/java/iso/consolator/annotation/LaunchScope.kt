package iso.consolator.annotation

import iso.consolator.typeIs
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

@Retention(SOURCE)
@Target(ANNOTATION_CLASS, CONSTRUCTOR, FUNCTION, PROPERTY, PROPERTY_GETTER)
annotation class LaunchScope

internal fun filterIsLaunchScope(it: Any) = it.typeIs<LaunchScope, _>()