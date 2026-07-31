package com.beautymanager.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.beautymanager.app.core.util.SecurityUtils
import com.beautymanager.app.data.local.dao.UserDao
import com.beautymanager.app.data.remote.barcode.BarcodeApi
import com.beautymanager.app.domain.model.AppUser
import com.beautymanager.app.domain.model.UserRole
import com.beautymanager.app.domain.repository.BarcodeLookupRepository
import com.beautymanager.app.domain.repository.BarcodeProductInfo
import com.beautymanager.app.domain.repository.SessionRepository
import com.beautymanager.app.domain.repository.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.sessionDataStore by preferencesDataStore(name = "session")

/**
 * Sessão do app: quem está logado agora, se a biometria está ligada e o tema escolhido.
 * Tudo local via DataStore — nenhuma credencial em texto puro, nenhuma chamada de rede.
 */
class SessionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDao: UserDao
) : SessionRepository {

    private object Keys {
        val CURRENT_USER_ID = longPreferencesKey("current_user_id")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    override suspend fun isAnyUserConfigured(): Boolean = userDao.count() > 0

    override suspend fun loginWithPin(pin: String): AppUser? {
        val match = userDao.getAll().firstOrNull { it.pinHash == SecurityUtils.hashPin(pin) } ?: return null
        context.sessionDataStore.edit { it[Keys.CURRENT_USER_ID] = match.id }
        return AppUser(
            id = match.id, name = match.name, role = UserRole.valueOf(match.role),
            canManageProducts = match.canManageProducts, canManageSales = match.canManageSales,
            canViewReports = match.canViewReports, canManageUsers = match.canManageUsers
        )
    }

    override fun observeCurrentUser(): Flow<AppUser?> =
        context.sessionDataStore.data.map { it[Keys.CURRENT_USER_ID] }.flatMapLatest { id ->
            flow {
                if (id == null) {
                    emit(null)
                } else {
                    val entity = userDao.getById(id)
                    emit(
                        entity?.let {
                            AppUser(
                                id = it.id, name = it.name, role = UserRole.valueOf(it.role),
                                canManageProducts = it.canManageProducts, canManageSales = it.canManageSales,
                                canViewReports = it.canViewReports, canManageUsers = it.canManageUsers
                            )
                        }
                    )
                }
            }
        }

    override suspend fun logout() {
        context.sessionDataStore.edit { it.remove(Keys.CURRENT_USER_ID) }
    }

    override suspend fun isBiometricEnabled(): Boolean =
        context.sessionDataStore.data.first()[Keys.BIOMETRIC_ENABLED] ?: false

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        context.sessionDataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    override fun observeThemeMode(): Flow<ThemeMode> =
        context.sessionDataStore.data.map { prefs ->
            prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SISTEMA
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.sessionDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }
}

/**
 * Consulta a Open Beauty Facts só para SUGERIR nome/marca/foto ao cadastrar um produto
 * novo pelo código de barras. Cobertura é comunitária/parcial — quando não achar, a tela
 * cai para preenchimento manual (ver LookupProductByBarcodeUseCase).
 */
class BarcodeLookupRepositoryImpl @Inject constructor(
    private val api: BarcodeApi
) : BarcodeLookupRepository {
    override suspend fun lookup(barcode: String): BarcodeProductInfo? {
        return try {
            val response = api.getProduct(barcode)
            if (response.status != 1 || response.product == null) return null
            val product = response.product
            BarcodeProductInfo(
                name = product.product_name,
                brandName = product.brands,
                imageUrl = product.image_front_url ?: product.image_url
            )
        } catch (e: Exception) {
            null // Sem internet ou API fora do ar: cai para cadastro manual, nunca quebra o fluxo.
        }
    }
}
