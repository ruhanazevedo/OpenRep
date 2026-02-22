package com.ruhanazevedo.openrep.ui.viewmodel

import com.ruhanazevedo.openrep.data.db.dao.UserPreferencesDao
import com.ruhanazevedo.openrep.data.db.entity.UserPreferencesEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow

class FakeUserPreferencesDao(
    private val flow: MutableStateFlow<UserPreferencesEntity>,
    private val updates: MutableList<UserPreferencesEntity>
) : UserPreferencesDao {

    private var inserted = false

    override fun get(): Flow<UserPreferencesEntity?> = flow

    override suspend fun insertDefaults(prefs: UserPreferencesEntity) {
        if (!inserted) {
            inserted = true
            flow.value = prefs
        }
    }

    override suspend fun update(prefs: UserPreferencesEntity) {
        flow.value = prefs
        updates.add(prefs)
    }
}
