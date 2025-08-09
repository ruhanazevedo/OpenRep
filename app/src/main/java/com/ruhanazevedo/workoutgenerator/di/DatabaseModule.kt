package com.ruhanazevedo.workoutgenerator.di

import android.content.Context
import androidx.room.Room
import com.ruhanazevedo.workoutgenerator.data.db.AppDatabase
import com.ruhanazevedo.workoutgenerator.data.db.dao.ExerciseDao
import com.ruhanazevedo.workoutgenerator.data.db.dao.SessionSetDao
import com.ruhanazevedo.workoutgenerator.data.db.dao.UserPreferencesDao
import com.ruhanazevedo.workoutgenerator.data.db.dao.WorkoutPlanDao
import com.ruhanazevedo.workoutgenerator.data.db.dao.WorkoutPlanExerciseDao
import com.ruhanazevedo.workoutgenerator.data.db.dao.WorkoutSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "workout_generator.db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideExerciseDao(db: AppDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun provideWorkoutPlanDao(db: AppDatabase): WorkoutPlanDao = db.workoutPlanDao()

    @Provides
    fun provideWorkoutPlanExerciseDao(db: AppDatabase): WorkoutPlanExerciseDao = db.workoutPlanExerciseDao()

    @Provides
    fun provideWorkoutSessionDao(db: AppDatabase): WorkoutSessionDao = db.workoutSessionDao()

    @Provides
    fun provideSessionSetDao(db: AppDatabase): SessionSetDao = db.sessionSetDao()

    @Provides
    fun provideUserPreferencesDao(db: AppDatabase): UserPreferencesDao = db.userPreferencesDao()
}
