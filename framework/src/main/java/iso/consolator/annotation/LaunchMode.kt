package iso.consolator.annotation

import iso.consolator.typeIs
import kotlinx.coroutines.CoroutineStart
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

@Retention(SOURCE)
@Target(ANNOTATION_CLASS, CONSTRUCTOR, FUNCTION, PROPERTY, PROPERTY, PROPERTY_GETTER)
annotation class LaunchMode(
    val start: CoroutineStart = CoroutineStart.DEFAULT,
    val async: Boolean = true)

internal fun filterIsLaunchMode(it: Any) = it.typeIs<LaunchMode, _>()