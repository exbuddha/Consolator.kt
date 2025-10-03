package iso.consolator.annotation

import iso.consolator.no_timeout
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

@Retention(SOURCE)
@Target(FUNCTION)
annotation class Timeout(
    val millis: Long = no_timeout)