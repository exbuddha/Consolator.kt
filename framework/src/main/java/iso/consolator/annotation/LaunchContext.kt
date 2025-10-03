package iso.consolator.annotation

import iso.consolator.typeIs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

@Retention(SOURCE)
@Target(ANNOTATION_CLASS, CONSTRUCTOR, FUNCTION, PROPERTY, PROPERTY, PROPERTY_GETTER)
annotation class LaunchContext(
    val type: Type = Type.Implicit) {

    enum class Type(
        private var token: String,
        var dispatcher: CoroutineDispatcher = Dispatchers.Unconfined) {

        Implicit("Implicit"),

        IO("IO", Dispatchers.IO);
    }
}

internal fun filterIsLaunchContext(it: Any) = it.typeIs<LaunchContext, _>()