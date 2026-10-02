package com.testdone.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.testdone.app.data.local.dao.AttemptDao
import com.testdone.app.data.local.dao.ContentDao
import com.testdone.app.data.local.entity.AttemptEntity
import com.testdone.app.data.local.entity.ContentVersionEntity
import com.testdone.app.data.local.entity.QuestionEntity
import com.testdone.app.data.local.entity.SubjectEntity
import com.testdone.app.data.local.entity.TestEntity

@Database(
    entities = [
        QuestionEntity::class,
        TestEntity::class,
        SubjectEntity::class,
        ContentVersionEntity::class,
        AttemptEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class TestDoneDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
    abstract fun attemptDao(): AttemptDao

    companion object {
        fun build(context: android.content.Context): TestDoneDatabase =
            androidx.room.Room.databaseBuilder(
                context.applicationContext,
                TestDoneDatabase::class.java,
                "testdone.db",
            )
                .fallbackToDestructiveMigration() // content is re-seedable; attempts sync from cloud
                .build()
    }
}
