package com.tapago.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Classe [Application] raiz do Tá Pago.
 * Ponto de entrada do grafo de injeção de dependências (Hilt).
 */
@HiltAndroidApp
class TaPagoApplication : Application()
