package iso.consolator.reflect

// provides array access to set
interface ReceiverArray<I : Number> {
    operator fun <T> set(key: I? = null, value: T): ReceiverArray<I>
}