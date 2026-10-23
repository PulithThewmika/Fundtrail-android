package com.fundtrail.domain.usecase.recurring

import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.fakes.FakeRecurringRepository
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetDueRecurringUseCaseTest {

    private val zone = ZoneId.of("Asia/Colombo")
    // Today is 15 Oct 2026.
    private val clock = Clock.fixed(LocalDate.of(2026, 10, 15).atStartOfDay(zone).toInstant(), zone)
    private val repo = FakeRecurringRepository().apply { setTemplates(emptyList()) }
    private val useCase = GetDueRecurringUseCase(repo, clock)

    private fun template(id: String, nextDueDate: LocalDate, isPaused: Boolean = false, variable: Boolean = false) =
        RecurringTemplate(
            id = id, name = id, categoryId = "cat_rent", accountId = "default-main-bank",
            expectedAmountMinor = 3_400_000L, isVariableAmount = variable, dayOfMonth = 1,
            nextDueDate = nextDueDate, isPaused = isPaused,
        )

    @Test
    fun aTemplateNotYetDueIsNotListed() = runTest {
        repo.setTemplates(listOf(template("rent", LocalDate.of(2026, 11, 1))))
        assertTrue(useCase().first().isEmpty())
    }

    @Test
    fun aTemplateDueTodayIsListed() = runTest {
        repo.setTemplates(listOf(template("rent", LocalDate.of(2026, 10, 15))))
        assertEquals(listOf("rent"), useCase().first().map { it.id })
    }

    @Test
    fun aTwoMonthOverdueTemplateShowsAsOneItemAtItsEarliestMonth() = runTest {
        // nextDueDate is August; it is a single cursor, so it surfaces once, for August.
        repo.setTemplates(listOf(template("rent", LocalDate.of(2026, 8, 1))))
        val due = useCase().first()
        assertEquals(1, due.size)
        assertEquals(LocalDate.of(2026, 8, 1), due.single().nextDueDate)
    }

    @Test
    fun aPausedTemplateIsNeverDue() = runTest {
        repo.setTemplates(listOf(template("rent", LocalDate.of(2026, 8, 1), isPaused = true)))
        assertTrue(useCase().first().isEmpty())
    }

    @Test
    fun catchingUpAdvancesOneMonthAtATime() = runTest {
        repo.setTemplates(listOf(template("rent", LocalDate.of(2026, 8, 1))))

        // Confirm August; the cursor advances to September, which is still due.
        repo.confirm("rent", 3_400_000L, LocalDate.of(2026, 8, 1).atStartOfDay(zone).toInstant().toEpochMilli(), LocalDate.of(2026, 8, 1))
        assertEquals(LocalDate.of(2026, 9, 1), useCase().first().single().nextDueDate)

        // Confirm September too; October is still due.
        repo.confirm("rent", 3_400_000L, LocalDate.of(2026, 9, 1).atStartOfDay(zone).toInstant().toEpochMilli(), LocalDate.of(2026, 9, 1))
        assertEquals(LocalDate.of(2026, 10, 1), useCase().first().single().nextDueDate)

        // Confirm October; the cursor moves to November, which is not due, so the list is empty.
        repo.confirm("rent", 3_400_000L, LocalDate.of(2026, 10, 1).atStartOfDay(zone).toInstant().toEpochMilli(), LocalDate.of(2026, 10, 1))
        assertTrue(useCase().first().isEmpty())
    }
}
