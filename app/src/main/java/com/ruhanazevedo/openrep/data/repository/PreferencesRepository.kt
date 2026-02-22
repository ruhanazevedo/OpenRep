package com.ruhanazevedo.openrep.data.repository

import com.ruhanazevedo.openrep.data.db.dao.UserPreferencesDao
import com.ruhanazevedo.openrep.data.db.entity.UserPreferencesEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val dao: UserPreferencesDao
) {
    val preferences: Flow<UserPreferencesEntity> = dao.get().map { it ?: UserPreferencesEntity() }

    suspend fun ensureDefaults() {
        dao.insertDefaults(UserPreferencesEntity())
    }

    suspend fun update(prefs: UserPreferencesEntity) {
        dao.insertDefaults(prefs) // no-op if row exists
        dao.update(prefs)
    }
}
