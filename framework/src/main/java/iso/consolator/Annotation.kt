@file:JvmName(JVM_CLASS_NAME)
@file:JvmMultifileClass

package iso.consolator

import iso.consolator.annotation.TagType
import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation

internal inline fun <reified T : Annotation> Any.hasAnnotationType() =
    asReference().hasAnnotation<T>()

internal inline fun <reified T : Annotation> Any.hasNoAnnotationType() =
    hasAnnotationType<T>().not()

internal inline fun <reified T : Annotation> Any?.hasNullableAnnotationType() =
    asReference().hasAnnotation<T>()

internal inline fun <reified T : Annotation> Any?.hasNoNullableAnnotationType() =
    hasNullableAnnotationType<T>().not()

internal inline fun <reified T : Annotation, R, S : R> R.whenHasAnnotation(block: R.() -> S) =
    runWhen({ this?.hasAnnotationType<T>().isTrue() }, block)

internal inline fun <reified T : Annotation, R, S : R> R.whenHasNoAnnotation(block: R.() -> S) =
    runWhen({ this?.hasAnnotationType<T>().isFalse() }, block)

internal fun Any?.asAnnotations() = asTypeUnsafe<Annotations>()

internal typealias Annotations = Iterable<Annotation>
internal typealias AnnotationsMap = Map<AnnotationClass, AnnotationClassMapEntry> // may be simplified to a number given context is well-known
internal typealias AnnotationClassMapEntry = Pair<AnnotationClass, AnnotationValueMap>
internal typealias AnnotationValueMap = Pair<AnnotationValueKey, Any?>
private typealias AnnotationValueKey = TagType /* can be hash code or order of occurrence */

internal typealias AnnotationClass = KClass<out Annotation>