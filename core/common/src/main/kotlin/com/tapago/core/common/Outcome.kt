package com.tapago.core.common

/**
 * Wrapper padrão de resultado usado em todos os use cases e repositórios,
 * conforme padronizado na especificação técnica (seção 2.4).
 */
sealed class Outcome<out T> {
    data class Success<T>(val data: T) : Outcome<T>()
    data class Error(val throwable: Throwable, val message: String? = null) : Outcome<Nothing>()
    data object Loading : Outcome<Nothing>()
}

inline fun <T> Outcome<T>.onSuccess(action: (T) -> Unit): Outcome<T> {
    if (this is Outcome.Success) action(data)
    return this
}

inline fun <T> Outcome<T>.onError(action: (Throwable, String?) -> Unit): Outcome<T> {
    if (this is Outcome.Error) action(throwable, message)
    return this
}
