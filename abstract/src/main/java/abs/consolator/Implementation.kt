@file:JvmName(JVM_CLASS_NAME)
@file:JvmMultifileClass

package abs.consolator

fun now(): Long = java.util.Calendar.getInstance().timeInMillis

const val JVM_CLASS_NAME = "Companion"