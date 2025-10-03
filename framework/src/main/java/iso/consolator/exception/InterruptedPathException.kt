package iso.consolator.exception

internal open class InterruptedPathException(
    override val message: String? = null,
    override val cause: Throwable? = null
) : InterruptedException()