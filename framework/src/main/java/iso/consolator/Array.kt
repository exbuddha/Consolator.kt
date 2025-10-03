@file:JvmName(JVM_CLASS_NAME)
@file:JvmMultifileClass

package iso.consolator

internal fun <T> List<T>.second() = get(1)

internal inline fun <T, reified R> Array<out T>.mapToTypedArray(transform: (T) -> R) =
    map(transform).toTypedArray()

internal fun <T> Array<out T>.second() = get(1)

internal fun <T> Array<out T>.secondOrNull() =
    resultWhen({ size > 1 }, Array<out T>::second)

internal fun <T> Array<out T>.third() = get(2)

internal fun <T> Array<out T>.thirdOrNull() =
    resultWhen({ size > 2 }, Array<out T>::third)

internal fun <T> Array<out T>.nthOrNull(n: Int) =
    resultWhen({ n in indices }) { get(n) }

internal fun <R> VarArray.with(function: (VarArray) -> R): () -> R = {
    function(this) }

internal fun <T> T.asPointer(): () -> T = { this }

internal fun <S, T : S> T.asTypedPointer(): () -> S = { this }

internal fun Any?.asAnyToBooleanPair() = asType<Pair<Any, Boolean>>()

internal typealias ObjectPointer = () -> Any
internal typealias AnyPredicate = (Any?) -> Boolean
internal typealias ObjectPredicate = (Any) -> Boolean
internal typealias AnyTuplePredicate = (Any?, Any?) -> Boolean
internal typealias AnyTripletPredicate = (Any?, Any?, Any?) -> Boolean
internal typealias AnyClassTypePredicate = Any?.() -> Boolean

internal typealias AnyTripleFunction = (AnyTriple) -> Any?
internal typealias AnyTriplePredicate = (AnyTriple) -> Boolean

internal typealias AnyTriplePointer = () -> AnyTriple

internal fun Any.asAny() = asTypeUnsafe<Any>()
internal fun Any?.asAnyArray() = asType<AnyArray>()

internal typealias AnyPair = Pair<*,*>
internal typealias AnyTriple = Triple<*,*,*>
internal typealias AnyArray = Array<*>
internal typealias VarArray = Array<out Any?>
internal typealias AnyMutableList = MutableList<*>
internal typealias AnyIterable = Iterable<*>
internal typealias IntArray = Array<Int>
internal typealias IntMutableList = MutableList<Int>
internal typealias ShortArray = Array<Short>
internal typealias StringArray = Array<String>
internal typealias AnyFunctionList = MutableList<AnyFunction>