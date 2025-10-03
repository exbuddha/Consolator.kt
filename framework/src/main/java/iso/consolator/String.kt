@file:JvmName("String")
@file:JvmMultifileClass

package iso.consolator

internal fun Any.asStringCounted(n: Number) = "$this#$n"

internal fun parseToInt(it: String?) = it?.toInt()

internal fun parseToLong(it: String?) = it?.toLong()

internal fun parseToShort(it: String?) = it?.toShort()

internal fun parseToByte(it: String?) = it?.toByte()

internal fun parseToDouble(it: String?) = it?.toDouble()

internal fun parseToFloat(it: String?) = it?.toFloat()

internal fun Any?.asString() = asType<String>()

internal typealias StringFunction = () -> String
internal typealias StringPointer = () -> String?
internal typealias CharsPointer = () -> CharSequence?
internal typealias AnyClassCharsFunction = Any?.() -> CharSequence
internal typealias StringToNumberFunction = (String?) -> Number?