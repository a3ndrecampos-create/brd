package com.beautymanager.app.domain.usecase

import com.beautymanager.app.domain.model.*
import com.beautymanager.app.domain.repository.*
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * Fecha uma venda: cria o registro da venda + itens, decrementa o estoque de cada
 * produto (com o respectivo StockMovement de SAÍDA) e atualiza os contadores
 * agregados do produto (totalSold/totalProfit/lastSaleAt) para o Dashboard não
 * precisar recalcular tudo a cada abertura de tela.
 */
class RegisterSaleUseCase @Inject constructor(
    private val saleRepository: SaleRepository,
    private val productRepository: ProductRepository,
    private val stockMovementRepository: StockMovementRepository
) {
    suspend operator fun invoke(
        cartItems: List<CartItem>,
        customerId: Long?,
        sellerUserId: Long,
        discount: Double,
        paymentMethod: PaymentMethod,
        now: Long = System.currentTimeMillis()
    ): Result<Long> {
        if (cartItems.isEmpty()) return Result.failure(IllegalArgumentException("Carrinho vazio"))

        val totalAmount = cartItems.sumOf { it.subtotal } - discount
        val totalProfit = cartItems.sumOf { (it.product.salePrice - it.product.costPrice) * it.quantity } - discount

        val sale = Sale(
            customerId = customerId,
            sellerUserId = sellerUserId,
            dateTimeEpochMillis = now,
            discount = discount,
            paymentMethod = paymentMethod,
            totalAmount = totalAmount,
            totalProfit = totalProfit
        )
        val saleItems = cartItems.map {
            SaleItem(
                saleId = 0, // preenchido pelo repositório ao persistir a venda + itens na mesma transação
                productId = it.product.id,
                quantity = it.quantity,
                unitPrice = it.product.salePrice,
                unitCost = it.product.costPrice
            )
        }

        val saleId = saleRepository.registerSale(sale, saleItems)

        cartItems.forEach { item ->
            productRepository.applyStockDelta(item.product.id, -item.quantity)
            stockMovementRepository.record(
                StockMovement(
                    productId = item.product.id,
                    type = StockMovementType.SAIDA,
                    quantity = item.quantity,
                    dateTimeEpochMillis = now,
                    notes = "Venda #$saleId"
                )
            )
            val profitForItem = (item.product.salePrice - item.product.costPrice) * item.quantity
            productRepository.registerSaleImpact(item.product.id, item.quantity, profitForItem, now)
        }

        return Result.success(saleId)
    }
}

/**
 * Busca um produto por código de barras: primeiro no banco local (já cadastrado antes,
 * "nunca pedir de novo"); se não existir, consulta a base pública (Open Food Facts) só
 * para sugerir nome/marca/foto — o usuário sempre confirma preço de custo, venda e
 * quantidade manualmente antes de salvar.
 */
class LookupProductByBarcodeUseCase @Inject constructor(
    private val productRepository: ProductRepository,
    private val barcodeLookupRepository: BarcodeLookupRepository
) {
    suspend operator fun invoke(barcode: String): BarcodeLookupResult {
        productRepository.getByBarcode(barcode)?.let { return BarcodeLookupResult.AlreadyRegistered(it) }

        val info = barcodeLookupRepository.lookup(barcode)
        return if (info != null) {
            BarcodeLookupResult.SuggestionFound(barcode, info)
        } else {
            BarcodeLookupResult.NotFound(barcode)
        }
    }
}

sealed interface BarcodeLookupResult {
    data class AlreadyRegistered(val product: Product) : BarcodeLookupResult
    data class SuggestionFound(val barcode: String, val info: BarcodeProductInfo) : BarcodeLookupResult
    data class NotFound(val barcode: String) : BarcodeLookupResult
}

/**
 * Roda diariamente (via WorkManager) para regenerar a lista de lembretes de recompra:
 * para cada cliente, olha a última compra em cada categoria e compara com o limiar de
 * dias configurado na regra daquela categoria (ex.: perfume=90 dias, shampoo=30 dias).
 */
class GenerateRemindersUseCase @Inject constructor(
    private val reminderRuleRepository: ReminderRuleRepository,
    private val reminderRepository: ReminderRepository
) {
    suspend operator fun invoke(now: Long = System.currentTimeMillis()) {
        val rules = reminderRuleRepository.observeAll().first()
        if (rules.isEmpty()) return
        reminderRepository.regenerateFromRules(rules, now)
    }
}

class DashboardMetricsUseCase @Inject constructor(
    private val saleRepository: SaleRepository,
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
    private val reminderRepository: ReminderRepository
) {
    suspend operator fun invoke(now: LocalDateTime = LocalDateTime.now()): DashboardMetrics {
        val zone = ZoneId.systemDefault()
        val startOfDay = now.toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = now.toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val soldToday = saleRepository.getTotalSoldBetween(startOfDay, endOfDay)
        val soldMonth = saleRepository.getTotalSoldBetween(startOfMonth, endOfDay)
        val salesCountToday = saleRepository.getSalesCountBetween(startOfDay, endOfDay)
        val profitToday = saleRepository.getTotalProfitBetween(startOfDay, endOfDay)
        val topProducts = saleRepository.getTopProducts(limit = 5, sinceEpochMillis = startOfMonth)
        val lowStockCount = productRepository.observeLowStock().first().size
        val pendingReminders = reminderRepository.observePending().first().size
        val birthdays = customerRepository.getBirthdaysInMonth(now.monthValue).size

        // productsSoldToday: soma das quantidades vendidas hoje (aproximação via itens dos top products
        // seria imprecisa; em uma implementação futura, expor uma query dedicada no SaleItemDao).
        val productsSoldToday = salesCountToday // TODO: trocar por soma real de itens vendidos hoje

        return DashboardMetrics(
            totalSoldToday = soldToday,
            totalSoldThisMonth = soldMonth,
            salesCountToday = salesCountToday,
            productsSoldToday = productsSoldToday,
            profitToday = profitToday,
            lowStockCount = lowStockCount,
            customersNeedingContactCount = pendingReminders,
            birthdaysThisMonthCount = birthdays,
            topProducts = topProducts
        )
    }
}

/** Monta a mensagem de WhatsApp personalizada para um lembrete, pronta para edição pelo usuário. */
class BuildWhatsAppMessageUseCase @Inject constructor() {
    operator fun invoke(customerName: String, productName: String, template: String): String =
        template
            .replace("{cliente}", customerName)
            .replace("{produto}", productName)
}
