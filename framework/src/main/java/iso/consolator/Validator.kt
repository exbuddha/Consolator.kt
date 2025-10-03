@file:JvmName("Validation")
@file:JvmMultifileClass

package iso.consolator

import kotlin.reflect.*

internal interface Validator {
    fun <T, R : T> T.validate(): R = validateType()
}

@Suppress("UNCHECKED_CAST")
internal fun <T, R : Any?> T.validateType() = this as R

inline fun <reified T, reified O> isType(it: O) = it is T

inline fun <reified T, reified O> isNotType(it: O) = it !is T

infix fun <T> Any?.isObject(obj: T) = this === obj

infix fun <T> Any?.isNotObject(obj: T) = this !== obj

infix fun <T> Any?.`is`(value: T) = this == value

infix fun <T> Any?.isNot(value: T) = this != value

internal fun Any?.isTrue() = `is`(true)

internal fun Any?.isFalse() = `is`(false)

internal fun Any?.isNotTrue() = isNot(true)

internal fun Any?.isNotFalse() = isNot(false)

internal fun trueWhenNull(it: Any?) = it isObject null

internal fun trueWhenNotNull(it: Any?) = it isNotObject null

internal fun <T> KProperty<T?>.isNull() = getInstance() isObject null

internal fun <T> KProperty<T?>.isNotNull() = getInstance() isNotObject null

internal fun <T> KProperty<T?>.isTrue() = getInstance() `is` true

internal fun <T> KProperty<T?>.isFalse() = getInstance() `is` false