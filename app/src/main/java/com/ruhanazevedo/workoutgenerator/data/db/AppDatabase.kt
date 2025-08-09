package com.ruhanazevedo.workoutgenerator.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ruhanazevedo.workoutgenerator.data.db.converter.Converters
import com.ruhanazevedo.workoutgenerator.data.db.dao.ExerciseDao
import com.ruhanazevedo.workoutgenerator.data.db.dao.SessionSetDao
import com.ruhanazevedo.workoutgenerator.data.db.dao.UserPreferencesDao
import com.ruhanazevedo.workoutgenerator.data.db.dao.WorkoutPlanDao
import com.ruhanazevedo.workoutgenerator.data.db.dao.WorkoutPlanExerciseDao
import com.ruhanazevedo.workoutgenerator.data.db.dao.WorkoutSessionDao
import com.ruhanazevedo.workoutgenerator.data.db.entity.ExerciseEntity
import com.ruhanazevedo.workoutgenerator.data.db.entity.SessionSetEntity
import com.ruhanazevedo.workoutgenerator.data.db.entity.UserPreferencesEntity
import com.ruhanazevedo.workoutgenerator.data.db.entity.WorkoutPlanEntity
import com.ruhanazevedo.workoutgenerator.data.db.entity.WorkoutPlanExerciseEntity
import com.ruhanazevedo.workoutgenerator.data.db.entity.WorkoutSessionEntity

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutPlanEntity::class,
        WorkoutPlanExerciseEntity::class,
        WorkoutSessionEntity::class,
        SessionSetEntity::class,
        UserPreferencesEntity::class,
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun workoutPlanExerciseDao(): WorkoutPlanExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun sessionSetDao(): SessionSetDao
    abstract fun userPreferencesDao(): UserPreferencesDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_preferences ADD COLUMN last_days_per_week INTEGER NOT NULL DEFAULT 3")
                db.execSQL("ALTER TABLE user_preferences ADD COLUMN last_split_type TEXT NOT NULL DEFAULT 'A'")
                db.execSQL("ALTER TABLE user_preferences ADD COLUMN last_selected_muscles TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
