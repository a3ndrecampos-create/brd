package com.beautymanager.app.core.util

import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Wrapper fino sobre o BiometricPrompt para uso a partir de Compose. Precisa que
 * a Activity hospedeira seja uma FragmentActivity (ver MainActivity).
 */
@Composable
fun rememberBiometricAuthenticator(
    onSuccess: () -> Unit,
    onError: (String) -> Unit = {}
): () -> Unit {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val prompt = remember(activity) {
        activity?.let {
            BiometricPrompt(
                it,
                ContextCompat.getMainExecutor(it),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        onError(errString.toString())
                    }
                }
            )
        }
    }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Entrar no BeautyManager")
            .setSubtitle("Use sua digital ou reconhecimento facial")
            .setNegativeButtonText("Usar PIN")
            .build()
    }

    return {
        if (prompt != null) prompt.authenticate(promptInfo) else onError("Biometria indisponível neste aparelho")
    }
}
