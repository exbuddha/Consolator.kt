package data.consolator.entity

import android.os.Parcelable
import androidx.room.*
import ctx.consolator.UniqueContext
import ctx.consolator.now
import data.consolator.*
import data.consolator.RuntimeDatabase.Companion.CURRENT_TIMESTAMP
import kotlinx.parcelize.Parcelize

open class TimeSensitiveSessionEntity(
    override val id: Long,
    override var sid: Long? = session?.id,
) : BaseSessionEntity(id, sid) {
    @Parcelize
    open class Internal(
        @ColumnInfo(name = RuntimeSessionEntity.DB_TIME, defaultValue = CURRENT_TIMESTAMP)
        internal open var dbTime: String,

        override val id: Long,
        override var sid: Long?,
    ) : TimeSensitiveSessionEntity(id, sid), Parcelable

    @Parcelize
    data class Local(
        @ColumnInfo(name = RuntimeSessionEntity.CTX_TIME, defaultValue = CURRENT_TIMESTAMP)
        override var uid: Long = now(),

        override val id: Long,
        override var sid: Long?,
    ) : TimeSensitiveSessionEntity(id, sid), UniqueContext.Instance, Parcelable

    @Parcelize
    data class Differential(
        @ColumnInfo(name = RuntimeSessionEntity.CTX_TIME, defaultValue = CURRENT_TIMESTAMP)
        override var uid: Long = now(),

        override var dbTime: String,
        override val id: Long,
        override var sid: Long?,
    ) : Internal(dbTime, id, sid), UniqueContext.Instance {
        internal constructor(instance: Internal, uid: Long = now(), dbTime: String = instance.dbTime) : this(uid, dbTime, instance.id, instance.sid)
    }

    internal companion object /* for time conversions */
}