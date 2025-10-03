package data.consolator.entity

import androidx.room.*
import ctx.consolator.*
import data.consolator.toLocalTimestamp
import data.consolator.RuntimeDatabase.Companion.CURRENT_TIMESTAMP

@Entity(tableName = RuntimeSessionEntity.TABLE)
data class RuntimeSessionEntity(
    @ColumnInfo(name = CTX_TIME)
    override var uid: Long,

    @ColumnInfo(name = INIT_TIME)
    internal var initTime: Long,

    @ColumnInfo(name = INIT_TIMESTAMP)
    internal var initTimestamp: String = initTime.toLocalTimestamp(),

    @ColumnInfo(name = DB_TIME, defaultValue = CURRENT_TIMESTAMP)
    internal var dbTime: String,

    @ColumnInfo(name = DB_VERSION)
    val dbVersion: Int? = null,

    @ColumnInfo(name = BUILD_TAGS)
    val buildTags: String? = null,

    @ColumnInfo(name = BUILD_TIME)
    val buildTime: Long? = null,

    override val id: Long,
) : BaseEntity(id), UniqueContext.Instance {
    internal companion object {
        const val CTX_TIME = "ctx_time"
        const val INIT_TIME = "init_time"
        const val INIT_TIMESTAMP = "init_timestamp"
        const val DB_TIME = "db_time"
        const val DB_VERSION = "db_version"
        const val BUILD_TAGS = "build_tags"
        const val BUILD_TIME = "build_time"
        const val TABLE = "sessions" } }