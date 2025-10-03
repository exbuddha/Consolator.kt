package iso.consolator.annotation

import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

@Retention(SOURCE)
@Target(CLASS, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
annotation class Referred // used for different purposes other than describing paths