package com.ruhanazevedo.openrep.data.seeder

import android.content.Context
import com.ruhanazevedo.openrep.data.db.dao.ExerciseDao
import com.ruhanazevedo.openrep.data.db.entity.ExerciseEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exerciseDao: ExerciseDao
) {
    suspend fun seedIfNeeded() {
        val count = exerciseDao.countSeeded()
        if (count > 0) return // idempotent — already seeded

        val entities = loadSeedExercises().map { seed ->
            ExerciseEntity(
                id = UUID.randomUUID().toString(),
                name = seed.name,
                muscleGroups = seed.muscleGroups,
                secondaryMuscleGroups = seed.secondaryMuscleGroups,
                equipment = seed.equipment,
                difficulty = seed.difficulty,
                instructions = seed.instructions,
                youtubeVideoId = seed.youtubeVideoId,
                isCustom = false,
                isDeleted = false,
                createdAt = System.currentTimeMillis()
            )
        }
        exerciseDao.insertAll(entities)
    }

    private fun loadSeedExercises(): List<SeedExercise> {
        val json = context.assets.open("seed_exercises.json")
            .bufferedReader()
            .use { it.readText() }

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val listType = Types.newParameterizedType(List::class.java, SeedExercise::class.java)
        val adapter = moshi.adapter<List<SeedExercise>>(listType)
        return adapter.fromJson(json) ?: emptyList()
    }
}
