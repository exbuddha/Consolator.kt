package iso.consolator.annotation

import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

@Retention(SOURCE)
@Target(FUNCTION, PROPERTY)
annotation class WithContext