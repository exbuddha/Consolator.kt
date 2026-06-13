@file:JvmName(JVM_CLASS_NAME)
@file:JvmMultifileClass

package iso.consolator

internal fun Any.asStringCounted(n: Number): String = "$this#$n"

internal fun parseToInt(it: String?): Int? = it?.toInt()

internal fun parseToLong(it: String?): Long? = it?.toLong()

internal fun parseToShort(it: String?): Short? = it?.toShort()

internal fun parseToByte(it: String?): Byte? = it?.toByte()

internal fun parseToDouble(it: String?): Double? = it?.toDouble()

internal fun parseToFloat(it: String?): Float? = it?.toFloat()

internal fun Any?.asString(): String? = asType<String>()

internal typealias StringFunction = () -> String
internal typealias StringPointer = () -> String?
internal typealias CharsPointer = () -> CharSequence?
internal typealias AnyClassCharsFunction = Any?.() -> CharSequence
internal typealias StringToNumberFunction = (String?) -> Number?