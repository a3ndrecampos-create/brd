package com.tapago.feature.tracking.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EstimateCaloriesUseCaseTest {

    private val useCase = EstimateCaloriesUseCase()

    @Test
    fun `corrida de 30 minutos estima calorias maiores que zero`() {
        val result = useCase(
            activityType = ActivityType.CORRIDA,
            durationSeconds = 1800,
        )
        assertTrue(result > 0)
    }

    @Test
    fun `corrida queima mais calorias que caminhada na mesma duracao`() {
        val running = useCase(ActivityType.CORRIDA, durationSeconds = 1800)
        val walking = useCase(ActivityType.CAMINHADA, durationSeconds = 1800)
        assertTrue(running > walking)
    }

    @Test
    fun `duracao zero resulta em zero calorias`() {
        val result = useCase(ActivityType.CORRIDA, durationSeconds = 0)
        assertEquals(0, result)
    }

    @Test
    fun `peso maior resulta em mais calorias para mesma atividade`() {
        val lighter = useCase(ActivityType.CORRIDA, durationSeconds = 1800, userWeightKg = 60.0)
        val heavier = useCase(ActivityType.CORRIDA, durationSeconds = 1800, userWeightKg = 90.0)
        assertTrue(heavier > lighter)
    }
}
