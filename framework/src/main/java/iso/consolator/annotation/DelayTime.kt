package iso.consolator.annotation

import iso.consolator.no_delay
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

@Retention(SOURCE)
@Target(FUNCTION, PROPERTY)
annotation class DelayTime(
    val millis: Long = no_delay,
    val tag: TagType = no_tag)