@file:JvmName("Session")
@file:JvmMultifileClass

package iso.consolator

import data.consolator.session

internal var session_id: Long? = null
    get() = field ?: session?.id