package de.westnordost.streetcomplete.data.elementfilter.filters

import de.westnordost.streetcomplete.data.elementfilter.matches
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HasDateTagLessThanTest {
    private val date = LocalDate.of(2000, 11, 11)
    private val c = HasDateTagLessThan("check_date", FixedDate(date))

    @Test fun matches() {
        assertFalse(c.matches(mapOf()))
        assertFalse(c.matches(mapOf("check_date" to "bla")))
        assertFalse(c.matches(mapOf("check_date" to "2000-11-12")))
        assertFalse(c.matches(mapOf("check_date" to "2000-11-11")))
        assertTrue(c.matches(mapOf("check_date" to "2000-11-10")))
    }

    @Test fun toStringMethod() {
        assertEquals("check_date < $date", c.toString())
    }
}
