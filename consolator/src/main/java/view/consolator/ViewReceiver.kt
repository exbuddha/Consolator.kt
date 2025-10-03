@file:JvmName("Receiver")
@file:JvmMultifileClass

package view.consolator

import iso.consolator.reflect.ReceiverArray

interface ViewReceiver : ReceiverArray<Int> {
    override operator fun <T> set(key: Int?, value: T): ViewReceiver
}