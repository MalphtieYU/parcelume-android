package com.parcelinbox.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ParcelStatusTransitionTest {
    @Test
    fun olderPageCannotMoveStatusBackwards() {
        assertEquals(
            ParcelStatus.OUT_FOR_DELIVERY,
            resolveParcelStatus(ParcelStatus.OUT_FOR_DELIVERY, ParcelStatus.SHIPPED)
        )
    }

    @Test
    fun newerStatusCanMoveForward() {
        assertEquals(
            ParcelStatus.READY_FOR_PICKUP,
            resolveParcelStatus(ParcelStatus.IN_TRANSIT, ParcelStatus.READY_FOR_PICKUP)
        )
    }

    @Test
    fun manualCompletionCannotBeReopenedByCapture() {
        assertEquals(
            ParcelStatus.COMPLETED,
            resolveParcelStatus(ParcelStatus.COMPLETED, ParcelStatus.IN_TRANSIT)
        )
    }

    @Test
    fun exceptionCanRecoverAfterAValidLaterObservation() {
        assertEquals(
            ParcelStatus.IN_TRANSIT,
            resolveParcelStatus(ParcelStatus.EXCEPTION, ParcelStatus.IN_TRANSIT)
        )
    }

    @Test
    fun genericPendingDoesNotEraseAnExistingException() {
        assertEquals(
            ParcelStatus.EXCEPTION,
            resolveParcelStatus(ParcelStatus.EXCEPTION, ParcelStatus.PENDING)
        )
    }

    @Test
    fun lateExceptionDoesNotRegressDeliveredParcel() {
        assertEquals(
            ParcelStatus.DELIVERED,
            resolveParcelStatus(ParcelStatus.DELIVERED, ParcelStatus.EXCEPTION)
        )
    }

    @Test
    fun lateExceptionDoesNotRegressReadyForPickupParcel() {
        assertEquals(
            ParcelStatus.READY_FOR_PICKUP,
            resolveParcelStatus(ParcelStatus.READY_FOR_PICKUP, ParcelStatus.EXCEPTION)
        )
    }
}
