package iso.consolator.reflect

// provides one-way access to get
interface PropertyItem {
    fun <T> PropertyItem.get(): T
}