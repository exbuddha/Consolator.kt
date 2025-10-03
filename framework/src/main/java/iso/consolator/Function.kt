@file:JvmName(JVM_CLASS_NAME)
@file:JvmMultifileClass

package iso.consolator

import kotlin.reflect.KParameter.*
import kotlin.reflect.KParameter.Kind.*

fun interface SuspendFunction<T, out R> : suspend (Array<out T>) -> R
fun interface SuspendFunction0<out R> : suspend () -> R
fun interface SuspendFunction1<P1, out R> : suspend (P1) -> R
fun interface SuspendFunction2<P1, P2, out R> : suspend (P1, P2) -> R
fun interface SuspendFunction3<P1, P2, P3, out R> : suspend (P1, P2, P3) -> R
fun interface SuspendFunction4<P1, P2, P3, P4, out R> : suspend (P1, P2, P3, P4) -> R

fun interface Function<T, out R> : (Array<out T>) -> R
fun interface Function0<out R> : () -> R
fun interface Function1<P1, out R> : (P1) -> R
fun interface Function2<P1, P2, out R> : (P1, P2) -> R
fun interface Function3<P1, P2, P3, out R> : (P1, P2, P3) -> R
fun interface Function4<P1, P2, P3, P4, out R> : (P1, P2, P3, P4) -> R

fun interface InVariantSuspendFunction<R> : suspend () -> R, PointerDefinition.InVariant

fun interface CoVariantSuspendFunction<out R> : suspend () -> R, PointerDefinition.CoVariant

fun interface InVariantFunction<R> : () -> R, PointerDefinition.InVariant

fun interface CoVariantFunction<out R> : () -> R, PointerDefinition.CoVariant

// ties into code unification

sealed interface PointerDefinition {
    sealed interface InVariant : PointerDefinition
    sealed interface CoVariant : PointerDefinition

    enum class Kind {
        Reference,
        Callable,
        Property,
        MutableProperty,
        Function,
        Constructor,
    }
}

sealed interface ParameterDefinition {
    fun define(variance: Variance, vararg position: Position)

    sealed interface InVariant : ParameterDefinition {
        sealed interface OnOne<P : Position, T> : InVariant
        sealed interface OnTwo<P1 : Position, P2 : Position, T> : InVariant
        sealed interface OnThree<P1 : Position, P2 : Position, P3 : Position, T> : InVariant
        sealed interface OnFour<P1 : Position, P2 : Position, P3 : Position, P4 : Position, T> : InVariant
        sealed interface OnFive<P1 : Position, P2 : Position, P3 : Position, P4 : Position, P5 : Position, T> : InVariant
        sealed interface OnSix<P1 : Position, P2 : Position, P3 : Position, P4 : Position, P5 : Position, P6 : Position, T> : InVariant
        sealed interface OnSeven<P1 : Position, P2 : Position, P3 : Position, P4 : Position, P5 : Position, P6 : Position, P7 : Position, T> : InVariant
    }

    sealed interface ContraVariant : ParameterDefinition {
        sealed interface OnOne<P : Position, in T> : ContraVariant
        sealed interface OnTwo<P1 : Position, P2 : Position, in T> : ContraVariant
        sealed interface OnThree<P1 : Position, P2 : Position, P3 : Position, in T> : ContraVariant
        sealed interface OnFour<P1 : Position, P2 : Position, P3 : Position, P4 : Position, in T> : ContraVariant
        sealed interface OnFive<P1 : Position, P2 : Position, P3 : Position, P4 : Position, P5 : Position, in T> : ContraVariant
        sealed interface OnSix<P1 : Position, P2 : Position, P3 : Position, P4 : Position, P5 : Position, P6 : Position, in T> : ContraVariant
        sealed interface OnSeven<P1 : Position, P2 : Position, P3 : Position, P4 : Position, P5 : Position, P6 : Position, P7 : Position, in T> : ContraVariant
    }

    sealed interface CoVariant : ParameterDefinition {
        sealed interface OnOne<P : Position, out T> : CoVariant
        sealed interface OnTwo<P1 : Position, P2 : Position, out T> : CoVariant
        sealed interface OnThree<P1 : Position, P2 : Position, P3 : Position, out T> : CoVariant
        sealed interface OnFour<P1 : Position, P2 : Position, P3 : Position, P4 : Position, out T> : CoVariant
        sealed interface OnFive<P1 : Position, P2 : Position, P3 : Position, P4 : Position, P5 : Position, out T> : CoVariant
        sealed interface OnSix<P1 : Position, P2 : Position, P3 : Position, P4 : Position, P5 : Position, P6 : Position, out T> : CoVariant
        sealed interface OnSeven<P1 : Position, P2 : Position, P3 : Position, P4 : Position, P5 : Position, P6 : Position, P7 : Position, out T> : CoVariant
    }

    sealed interface Vector : ParameterDefinition {
        // describes and performs explications
    }

    enum class Position(private val index: Byte, private val kind: Kind? = VALUE) {
        Receiver(0, EXTENSION_RECEIVER),
        Instance(1, INSTANCE),
        First(1),
        Second(2),
        Third(3),
        Fourth(4),
        Fifth(5),
        Sixth(6),

        ContextParameter(-1, null),
    }

    enum class Variance {
        Invariant,
        Covariant,
        Contravariant,
    }
}

internal fun Any?.asAnyFunction() = asType<AnyFunction>()
internal fun Any?.asAnyToAnyFunction() = asType<AnyToAnyFunction>()

internal typealias AnyFunction = () -> Any?
internal typealias AnyToAnyFunction = (Any?) -> Any?
internal typealias AnyClassStringFunction = Any?.() -> String
internal typealias AnyClassAnyToAnyFunction = Any?.(Any?) -> Any?
internal typealias AnySuspendFunction = suspend () -> Any?
internal typealias AnyToAnySuspendFunction = suspend (Any?) -> Any?
internal typealias AnyClassAnyToAnySuspendFunction = suspend Any?.(Any?) -> Any?