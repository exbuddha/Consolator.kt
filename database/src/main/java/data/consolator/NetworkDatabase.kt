@file:JvmName(JVM_CLASS_NAME)
@file:JvmMultifileClass

package data.consolator

import androidx.room.*
import data.consolator.dao.*
import data.consolator.entity.*
import ctx.consolator.JVM_CLASS_NAME

var netDb: NetworkDatabase? = null
    set(value) {
        field = ::netDb.receiveInDataModule(value) }

@Database(version = DB_VERSION, exportSchema = false, entities = [
    NetworkCapabilitiesEntity::class,
    NetworkStateEntity::class,
])
@File("net.db")
abstract class NetworkDatabase : RoomDatabase() {
    abstract fun networkDao(): NetworkDao
}