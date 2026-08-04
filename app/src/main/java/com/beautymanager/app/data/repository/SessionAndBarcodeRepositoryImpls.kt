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
import com.beautymanager.app.data.remote.barcode.CosmosApi
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
        val BIOMETRIC_USER_ID = longPreferencesKey("biometric_user_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    override suspend fun isAnyUserConfigured(): Boolean = userDao.count() > 0

    override suspend fun loginWithPin(pin: String): AppUser? {
        val match = userDao.getAll().firstOrNull { it.pinHash == SecurityUtils.hashPin(pin) } ?: return null
        context.sessionDataStore.edit { it[Keys.CURRENT_USER_ID] = match.id }
        return match.toAppUser()
    }

    override suspend fun loginDirectly(userId: Long): AppUser? {
        val entity = userDao.getById(userId) ?: return null
        context.sessionDataStore.edit { it[Keys.CURRENT_USER_ID] = entity.id }
        return entity.toAppUser()
    }

    override fun observeCurrentUser(): Flow<AppUser?> =
        context.sessionDataStore.data.map { it[Keys.CURRENT_USER_ID] }.flatMapLatest { id ->
            flow {
                if (id == null) {
                    emit(null)
                } else {
                    emit(userDao.getById(id)?.toAppUser())
                }
            }
        }

    override suspend fun logout() {
        context.sessionDataStore.edit { it.remove(Keys.CURRENT_USER_ID) }
    }

    override suspend fun isBiometricEnabled(): Boolean =
        context.sessionDataStore.data.first()[Keys.BIOMETRIC_ENABLED] ?: false

    override suspend fun setBiometricEnabled(enabled: Boolean, userId: Long?) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_ENABLED] = enabled
            if (enabled && userId != null) {
                prefs[Keys.BIOMETRIC_USER_ID] = userId
            } else if (!enabled) {
                prefs.remove(Keys.BIOMETRIC_USER_ID)
            }
        }
    }

    override suspend fun getBiometricUserId(): Long? =
        context.sessionDataStore.data.first()[Keys.BIOMETRIC_USER_ID]

    override fun observeThemeMode(): Flow<ThemeMode> =
        context.sessionDataStore.data.map { prefs ->
            prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SISTEMA
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.sessionDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }
}

private fun com.beautymanager.app.data.local.entity.UserEntity.toAppUser() = AppUser(
    id = id, name = name, role = UserRole.valueOf(role), canManageProducts = canManageProducts,
    canManageSales = canManageSales, canViewReports = canViewReports, canManageUsers = canManageUsers
)

/**
 * Consulta bases públicas de produto só para SUGERIR nome/marca/foto ao cadastrar
 * um produto novo pelo código de barras — o usuário sempre confirma preço de
 * custo, venda e quantidade manualmente. Tenta primeiro a Bluesoft Cosmos (melhor
 * cobertura de produtos brasileiros, mas exige token cadastrado); se não tiver
 * token configurado, ou a consulta falhar/não achar, cai para a Open Beauty Facts
 * (pública, sem chave). Se nenhuma achar, a tela cai para preenchimento manual
 * (ver LookupProductByBarcodeUseCase) — nunca quebra o fluxo de cadastro.
 */
class BarcodeLookupRepositoryImpl @Inject constructor(
    private val openBeautyFactsApi: BarcodeApi,
    private val cosmosApi: CosmosApi
) : BarcodeLookupRepository {

    override suspend fun lookup(barcode: String): BarcodeProductInfo? {
        if (com.beautymanager.app.BuildConfig.COSMOS_API_TOKEN.isNotBlank()) {
            lookupOnCosmos(barcode)?.let { return it }
        }
        return lookupOnOpenBeautyFacts(barcode)
    }

    private suspend fun lookupOnCosmos(barcode: String): BarcodeProductInfo? {
        return try {
            val response = cosmosApi.getProduct(barcode)
            if (response.description.isNullOrBlank()) return null
            BarcodeProductInfo(
                name = response.description,
                brandName = response.brand?.name,
                imageUrl = response.thumbnail
            )
        } catch (e: Exception) {
            null // Sem token válido, sem internet, ou GTIN não encontrado: tenta a próxima fonte.
        }
    }

    private suspend fun lookupOnOpenBeautyFacts(barcode: String): BarcodeProductInfo? {
        return try {
            val response = openBeautyFactsApi.getProduct(barcode)
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
