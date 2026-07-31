package com.beautymanager.app.core.util

import java.security.MessageDigest

/**
 * PIN nunca é guardado em texto puro: só o hash SHA-256 (+salt fixo do app) fica no banco.
 * Tudo local, sem envio para servidor nenhum.
 *
 * Nota de produção: para um salto de segurança além deste esqueleto, trocar por um salt
 * por instalação gerado uma vez e guardado no Android Keystore, em vez do salt fixo abaixo.
 */
object SecurityUtils {
    private const val APP_SALT = "beautymanager_v1_"

    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest((APP_SALT + pin).toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
