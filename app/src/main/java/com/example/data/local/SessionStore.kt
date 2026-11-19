package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.remote.CafeOptionDto
import com.example.data.remote.NetworkModule
import com.example.data.remote.UserProfileDto
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.syntelDataStore: DataStore<Preferences> by preferencesDataStore(name = "syntel_session")

enum class AppWorkspace(val key: String, val persianLabel: String) {
    REQUESTER("requester", "درخواست‌دهنده"),
    WAREHOUSE("warehouse", "مدیر / مسئول انبار"),
    PURCHASING("purchasing", "مسئول خرید");

    companion object {
        fun fromKey(value: String?): AppWorkspace =
            entries.firstOrNull { it.key == value } ?: REQUESTER
    }
}

data class UserSession(
    val token: String = "",
    val userId: Int = 0,
    val username: String = "",
    val fullName: String = "",
    val role: String = "",
    val roleDisplay: String = "",
    val secondaryRole: String = "",
    val isRequester: Boolean = false,
    val hasWarehouseRole: Boolean = false,
    val hasPurchasingRole: Boolean = false,
    val hasDualRole: Boolean = false,
    val departmentName: String = "",
    val cafeCode: String = "",
    val cafeName: String = "",
    val availableCafes: List<CafeOptionDto> = emptyList(),
    val activeWorkspace: AppWorkspace = AppWorkspace.REQUESTER,
    val darkTheme: Boolean = false,
) {
    val isLoggedIn: Boolean get() = token.isNotBlank() && cafeCode.isNotBlank()

    fun availableWorkspaces(): List<AppWorkspace> {
        val list = mutableListOf<AppWorkspace>()
        if (isRequester) list.add(AppWorkspace.REQUESTER)
        if (hasWarehouseRole) list.add(AppWorkspace.WAREHOUSE)
        if (hasPurchasingRole) list.add(AppWorkspace.PURCHASING)
        if (list.isEmpty()) list.add(AppWorkspace.REQUESTER)
        return list
    }
}

class SessionStore(private val context: Context) {

    @Volatile
    var cachedToken: String? = null
        private set

    private val cafesAdapter = NetworkModule.moshi.adapter<List<CafeOptionDto>>(
        Types.newParameterizedType(List::class.java, CafeOptionDto::class.java)
    )

    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val USER_ID = intPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val FULL_NAME = stringPreferencesKey("full_name")
        val ROLE = stringPreferencesKey("role")
        val ROLE_DISPLAY = stringPreferencesKey("role_display")
        val SECONDARY_ROLE = stringPreferencesKey("secondary_role")
        val IS_REQUESTER = booleanPreferencesKey("is_requester")
        val HAS_WAREHOUSE = booleanPreferencesKey("has_warehouse")
        val HAS_PURCHASING = booleanPreferencesKey("has_purchasing")
        val HAS_DUAL = booleanPreferencesKey("has_dual")
        val DEPARTMENT_NAME = stringPreferencesKey("department_name")
        val CAFE_CODE = stringPreferencesKey("cafe_code")
        val CAFE_NAME = stringPreferencesKey("cafe_name")
        val AVAILABLE_CAFES = stringPreferencesKey("available_cafes")
        val ACTIVE_WORKSPACE = stringPreferencesKey("active_workspace")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
    }

    val sessionFlow: Flow<UserSession> = context.syntelDataStore.data.map { prefs ->
        val token = prefs[Keys.TOKEN].orEmpty()
        cachedToken = token.ifBlank { null }
        val cafesJson = prefs[Keys.AVAILABLE_CAFES]
        val cafes = if (!cafesJson.isNullOrBlank()) {
            try {
                cafesAdapter.fromJson(cafesJson).orEmpty()
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        UserSession(
            token = token,
            userId = prefs[Keys.USER_ID] ?: 0,
            username = prefs[Keys.USERNAME].orEmpty(),
            fullName = prefs[Keys.FULL_NAME].orEmpty(),
            role = prefs[Keys.ROLE].orEmpty(),
            roleDisplay = prefs[Keys.ROLE_DISPLAY].orEmpty(),
            secondaryRole = prefs[Keys.SECONDARY_ROLE].orEmpty(),
            isRequester = prefs[Keys.IS_REQUESTER] ?: false,
            hasWarehouseRole = prefs[Keys.HAS_WAREHOUSE] ?: false,
            hasPurchasingRole = prefs[Keys.HAS_PURCHASING] ?: false,
            hasDualRole = prefs[Keys.HAS_DUAL] ?: false,
            departmentName = prefs[Keys.DEPARTMENT_NAME].orEmpty(),
            cafeCode = prefs[Keys.CAFE_CODE].orEmpty(),
            cafeName = prefs[Keys.CAFE_NAME].orEmpty(),
            availableCafes = cafes,
            activeWorkspace = AppWorkspace.fromKey(prefs[Keys.ACTIVE_WORKSPACE]),
            darkTheme = prefs[Keys.DARK_THEME] ?: false,
        )
    }

    suspend fun saveLogin(token: String, user: UserProfileDto) {
        cachedToken = token
        val defaultWorkspace = when {
            user.role == "warehouse" || (user.hasWarehouseRole && !user.isRequester && user.role != "purchasing") ->
                AppWorkspace.WAREHOUSE
            user.role == "purchasing" || (user.hasPurchasingRole && !user.isRequester) ->
                AppWorkspace.PURCHASING
            else -> AppWorkspace.REQUESTER
        }
        context.syntelDataStore.edit { prefs ->
            prefs[Keys.TOKEN] = token
            prefs[Keys.USER_ID] = user.id
            prefs[Keys.USERNAME] = user.username
            prefs[Keys.FULL_NAME] = user.fullName
            prefs[Keys.ROLE] = user.role
            prefs[Keys.ROLE_DISPLAY] = user.roleDisplay
            prefs[Keys.SECONDARY_ROLE] = user.secondaryRole
            prefs[Keys.IS_REQUESTER] = user.isRequester
            prefs[Keys.HAS_WAREHOUSE] = user.hasWarehouseRole
            prefs[Keys.HAS_PURCHASING] = user.hasPurchasingRole
            prefs[Keys.HAS_DUAL] = user.hasDualRole
            prefs[Keys.DEPARTMENT_NAME] = user.departmentName
            prefs[Keys.CAFE_CODE] = user.cafeCode
            prefs[Keys.CAFE_NAME] = user.cafeName
            prefs[Keys.AVAILABLE_CAFES] = cafesAdapter.toJson(user.availableCafes)
            prefs[Keys.ACTIVE_WORKSPACE] = defaultWorkspace.key
        }
    }

    suspend fun updateProfile(user: UserProfileDto) {
        context.syntelDataStore.edit { prefs ->
            prefs[Keys.FULL_NAME] = user.fullName
            prefs[Keys.ROLE] = user.role
            prefs[Keys.ROLE_DISPLAY] = user.roleDisplay
            prefs[Keys.SECONDARY_ROLE] = user.secondaryRole
            prefs[Keys.IS_REQUESTER] = user.isRequester
            prefs[Keys.HAS_WAREHOUSE] = user.hasWarehouseRole
            prefs[Keys.HAS_PURCHASING] = user.hasPurchasingRole
            prefs[Keys.HAS_DUAL] = user.hasDualRole
            prefs[Keys.DEPARTMENT_NAME] = user.departmentName
            if (user.cafeCode.isNotBlank()) {
                prefs[Keys.CAFE_CODE] = user.cafeCode
                prefs[Keys.CAFE_NAME] = user.cafeName
            }
            prefs[Keys.AVAILABLE_CAFES] = cafesAdapter.toJson(user.availableCafes)
        }
    }

    suspend fun setActiveWorkspace(workspace: AppWorkspace) {
        context.syntelDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_WORKSPACE] = workspace.key
        }
    }

    suspend fun setActiveCafe(cafe: CafeOptionDto) {
        context.syntelDataStore.edit { prefs ->
            prefs[Keys.CAFE_CODE] = cafe.code
            prefs[Keys.CAFE_NAME] = cafe.name
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.syntelDataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = enabled
        }
    }

    suspend fun clearSession() {
        cachedToken = null
        context.syntelDataStore.edit { prefs ->
            val keepDark = prefs[Keys.DARK_THEME] ?: false
            prefs.clear()
            prefs[Keys.DARK_THEME] = keepDark
        }
    }
}
