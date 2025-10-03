package iso.consolator

import iso.consolator.reflect.*

object Source : ObjectArray<ItemID>, ObjectItem<ItemKey> {
    override fun <T> ItemArray.get(key: ItemID?): T = TODO()

    override fun <T> set(key: ItemID?, value: T): ItemField = this

    override fun <T> PropertyItem.get(): T = TODO()

    override fun <R> ReceiverItem<R>.set(value: R): ReceiverItem<ItemKey> = Source
}

typealias ItemArray = PropertyArray<ItemID>
private typealias ItemField = ReceiverArray<ItemID>
private typealias ItemKey = NumberKCallable /* symbolic to ItemID */