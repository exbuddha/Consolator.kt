@file:JvmName("Validation")
@file:JvmMultifileClass

package iso.consolator

import kotlin.reflect.*

internal interface Validator {
    fun <T, R : T> T.validate(): R = validateType()
}

@Suppress("UNCHECKED_CAST")
internal fun <T, R> T.validateType(): R = this as R

inline fun <reified T, reified O> isType(it: O): Boolean = it is T

inline fun <reified T, reified O> isNotType(it: O): Boolean = it !is T

infix fun <T> Any?.isObject(obj: T): Boolean = this === obj

infix fun <T> Any?.isNotObject(obj: T): Boolean = this !== obj

infix fun <T> Any?.`is`(value: T): Boolean = this == value

infix fun <T> Any?.isNot(value: T): Boolean = this != value

internal fun Any?.isTrue(): Boolean = `is`(true)

internal fun Any?.isFalse(): Boolean = `is`(false)

internal fun Any?.isNotTrue(): Boolean = isNot(true)

internal fun Any?.isNotFalse(): Boolean = isNot(false)

internal fun trueWhenNull(it: Any?): Boolean = it.isNullValue()

internal fun trueWhenNotNull(it: Any?): Boolean = it.isNotNullValue()

internal fun <T> KProperty<T?>.isNull(): Boolean = getInstance().isNullValue()

internal fun <T> KProperty<T?>.isNotNull(): Boolean = getInstance().isNotNullValue()

internal fun <T> KProperty<T?>.isTrue(): Boolean = getInstance().isTrue()

internal fun <T> KProperty<T?>.isFalse(): Boolean = getInstance().isFalse()