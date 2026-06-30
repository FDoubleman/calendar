package com.fmm.calendar.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// version 要与预置数据库的 PRAGMA user_version 对齐。
// 当前 calendar.db 的 user_version 是 0，所以这里使用 version = 1 并在构建器中关闭迁移要求，
// 让 Room 专注于“读取已有表结构”。后续如果我们自己维护数据库版本，再正式引入 Migration。
@Database(
    entities = [CalendarDayEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CalendarDatabase : RoomDatabase() {
    abstract fun calendarDao(): CalendarDao

    companion object {
        @Volatile
        private var INSTANCE: CalendarDatabase? = null

        fun getInstance(context: Context): CalendarDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CalendarDatabase::class.java,
                    "calendar_slim.db",
                )
                    // createFromAsset 会在 App 首次打开数据库时，把 assets 里的瘦身库复制到内部存储。
                    // 之后 Room 查询的是内部存储副本，不会直接修改 assets 文件。
                    .createFromAsset("calendar_slim.db")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
