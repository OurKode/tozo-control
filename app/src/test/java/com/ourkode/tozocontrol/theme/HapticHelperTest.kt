package com.ourkode.tozocontrol.theme

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeHapticFeedback : HapticFeedback {
    var callCount = 0
    var lastType: HapticFeedbackType? = null

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        callCount++
        lastType = hapticFeedbackType
    }
}

class HapticHelperTest {

    @Test
    fun performIf_whenEnabled_delegatesToHapticFeedbackWithoutRecursion() {
        val fake = FakeHapticFeedback()
        // Must perform exactly 1 feedback call and NOT recurse or crash
        fake.performIf(enabled = true, type = HapticFeedbackType.TextHandleMove)

        assertEquals(1, fake.callCount)
        assertEquals(HapticFeedbackType.TextHandleMove, fake.lastType)
    }

    @Test
    fun performIf_whenDisabled_doesNotTriggerFeedback() {
        val fake = FakeHapticFeedback()
        fake.performIf(enabled = false, type = HapticFeedbackType.TextHandleMove)

        assertEquals(0, fake.callCount)
    }
}
