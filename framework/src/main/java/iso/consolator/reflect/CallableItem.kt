package iso.consolator.reflect

// provides access to call
interface CallableItem<out R> {
    fun call(vararg args: Any?): R
}