package net.consolator

import android.app.*
import android.content.*
import iso.consolator.*
import java.lang.ref.*

internal open class BaseService : Service(), BaseServiceScope {
    override var uid = 0L

    override fun onCreate() {
        super.onCreate()
        service = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) =
        intent.invoke(flags, startId,
            super.onStartCommand(intent, flags, startId))
        ?: START_NOT_STICKY

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        service = null
        super.onDestroy()
    }

    override var ref: WeakReference<out Context>? = null
        get() = field.receive(this).also { field = it }
}