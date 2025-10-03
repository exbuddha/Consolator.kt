package iso.consolator.reflect

// provides array access to get
interface PropertyArray<I : Number> {
    operator fun <T> PropertyArray<I>.get(key: I? = null): T
}