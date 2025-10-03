package iso.consolator.exception

import java.util.concurrent.CancellationException

internal open class TimeoutCancellation(
    override val message: String? = null,
    override val cause: Throwable? = null
) : CancellationException(message)