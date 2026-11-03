package com.fundtrail.widget

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.fundtrail.MainActivity
import com.fundtrail.widget.QuickAddLink.wantsQuickAdd
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The widget's "+ Add" request that MainActivity reads (#228). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class QuickAddLinkTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `the widget intent opens MainActivity and asks for quick add`() {
        val intent = QuickAddLink.intent(context)
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertTrue(intent.wantsQuickAdd())
    }

    @Test
    fun `a plain launch does not ask for quick add`() {
        assertFalse(Intent(context, MainActivity::class.java).wantsQuickAdd())
    }
}
