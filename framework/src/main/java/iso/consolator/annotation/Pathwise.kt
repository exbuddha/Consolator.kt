package iso.consolator.annotation

import iso.consolator.SchedulerPath
import iso.consolator.exception.SchedulerIntent
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

@Retention(SOURCE)
@Target(FUNCTION, PROPERTY)
annotation class Pathwise(
    val route: SchedulerPath = []) {

    abstract class FromLastCancellation : SchedulerIntent()
}
