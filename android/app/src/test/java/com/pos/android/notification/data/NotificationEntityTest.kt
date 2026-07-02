package com.pos.android.notification.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationEntityTest {

    @Test
    fun `entity with all fields`() {
        val entity = NotificationEntity(
            id = 1L,
            userId = 42L,
            titulo = "Stock crítico",
            mensaje = "El producto X está por debajo del mínimo",
            leido = false,
            creadoEn = "2026-06-28T10:00:00"
        )

        assertEquals(1L, entity.id)
        assertEquals(42L, entity.userId)
        assertEquals("Stock crítico", entity.titulo)
        assertEquals("El producto X está por debajo del mínimo", entity.mensaje)
        assertFalse(entity.leido)
        assertEquals("2026-06-28T10:00:00", entity.creadoEn)
    }

    @Test
    fun `entity with default values`() {
        val entity = NotificationEntity(id = 99L)

        assertEquals(99L, entity.id)
        assertFalse(entity.leido)
    }

    @Test
    fun `entity marked as read`() {
        val entity = NotificationEntity(
            id = 2L,
            titulo = "Venta completada",
            leido = true
        )

        assertTrue(entity.leido)
    }

    @Test
    fun `entity with null optional fields`() {
        val entity = NotificationEntity(id = 3L)

        assertEquals(3L, entity.id)
        assertEquals(null, entity.titulo)
        assertEquals(null, entity.mensaje)
        assertEquals(null, entity.userId)
        assertEquals(null, entity.creadoEn)
    }
}
