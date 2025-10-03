package iso.consolator.exception

internal open class InterruptedStepException(
    val step: Any,
    override val message: String? = null,
    override val cause: Throwable? = null
) : InterruptedPathException(message, cause)