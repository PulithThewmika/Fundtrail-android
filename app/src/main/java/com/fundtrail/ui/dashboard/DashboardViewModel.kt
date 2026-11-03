package com.fundtrail.ui.dashboard

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.GoalProgress
import com.fundtrail.domain.model.GoalStatus
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.NatureShare
import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SafeToSpend
import com.fundtrail.domain.model.SalaryStatus
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.usecase.category.ObserveCategoriesUseCase
import com.fundtrail.domain.usecase.goal.GetGoalProgressUseCase
import com.fundtrail.domain.usecase.goal.ObserveActiveGoalUseCase
import com.fundtrail.domain.usecase.income.GetSalaryStatusUseCase
import com.fundtrail.domain.usecase.recurring.ConfirmRecurringUseCase
import com.fundtrail.domain.usecase.recurring.GetDueRecurringUseCase
import com.fundtrail.domain.usecase.recurring.SkipRecurringUseCase
import com.fundtrail.domain.usecase.summary.GetMonthlySummaryUseCase
import com.fundtrail.domain.usecase.summary.GetSafeToSpendUseCase
import com.fundtrail.domain.usecase.summary.GetSpendingBreakdownUseCase
import com.fundtrail.domain.usecase.sync.ObserveSyncStatusUseCase
import com.fundtrail.ui.common.UiState
import com.fundtrail.ui.quickadd.AmountInput
import com.fundtrail.ui.common.mapErrorToMessage
import com.fundtrail.ui.common.uiStateOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject
import kotlin.math.roundToInt

data class DashboardUiState(
    val month: YearMonth,
    /** False on the current month: there is nothing to show in the future. */
    val canGoNext: Boolean,
    val content: UiState<DashboardData> = UiState.Loading,
    /** As-of-today; does not depend on [month] (Empty = no active goal). */
    val goal: UiState<GoalCardData> = UiState.Loading,
    /** Recurring items due today; empty hides the card. As-of-today, independent of [month] (#200). */
    val due: List<DueItemData> = emptyList(),
    /** The variable-amount confirm sheet, or null when it is closed (#202). */
    val dueConfirm: DueConfirmSheet? = null,
    val salaryStatus: SalaryStatus = SalaryStatus.NotDue,
    /** A failed one-tap confirm or skip, shown once and then cleared (#200). */
    @StringRes val dueError: Int? = null,
    /** As-of-today, so only set on the current month. Null while loading, on an error, or on a past month: the card is hidden (#217). */
    val safeToSpend: SafeToSpend? = null,
    /** The "how is this worked out" sheet behind the Safe to spend card (#217). */
    val safeToSpendExplainerOpen: Boolean = false,
)

/** One row of the dashboard "Due" card (#202). */
data class DueItemData(
    val templateId: String,
    val name: String,
    val expectedMinor: Long,
    val dueDate: LocalDate,
    val isVariableAmount: Boolean,
)

/** The small sheet a variable item opens, prefilled with the expected amount (#202). */
data class DueConfirmSheet(
    val templateId: String,
    val name: String,
    val amountText: String,
    val isSaving: Boolean = false,
    /** Set when the write failed, so the sheet stays open with the typed amount and the user can retry. */
    @StringRes val error: Int? = null,
)

data class DashboardData(
    val receivedMinor: Long,
    val spentMinor: Long,
    val savedMinor: Long,
    /** Largest first. */
    val incomeBySource: List<Pair<IncomeSource, Long>>,
    val committedMinor: Long,
    val discretionaryMinor: Long,
    val topCategories: List<RankedCategory>,
)

/** [name] is null when the category is not in Room (yet). */
data class RankedCategory(val name: String?, val spendKind: SpendKind, val amountMinor: Long, val sharePercent: Int)

/** Status shown on the dashboard goal card. [GoalStatus.BEHIND] splits into BEHIND or NEEDS_ADJUSTING by [GoalProgress.isInfeasible] (ADR-0006). */
enum class GoalCardStatus { NOT_ENOUGH_DATA, AHEAD, ON_TRACK, BEHIND, NEEDS_ADJUSTING }

data class GoalCardData(
    val name: String,
    val savedToDateMinor: Long,
    val targetMinor: Long,
    /** 0..100. */
    val percent: Int,
    val requiredMonthlyMinor: Long,
    val actualPaceMinor: Long?,
    val status: GoalCardStatus,
    val monthlyGapMinor: Long?,
    val projectedDate: LocalDate?,
    val deadline: LocalDate,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    getMonthlySummary: GetMonthlySummaryUseCase,
    getSpendingBreakdown: GetSpendingBreakdownUseCase,
    observeCategories: ObserveCategoriesUseCase,
    observeSyncStatus: ObserveSyncStatusUseCase,
    observeActiveGoal: ObserveActiveGoalUseCase,
    getGoalProgress: GetGoalProgressUseCase,
    getDueRecurring: GetDueRecurringUseCase,
    private val confirmRecurring: ConfirmRecurringUseCase,
    private val skipRecurring: SkipRecurringUseCase,
    getSalaryStatus: GetSalaryStatusUseCase,
    getSafeToSpend: GetSafeToSpendUseCase,
    private val clock: Clock,
) : ViewModel() {

    /** The variable-amount confirm sheet; null while closed. Combined into [uiState]. */
    private val dueConfirm = MutableStateFlow<DueConfirmSheet?>(null)

    /** A failed confirm or skip taken outside the sheet; the screen shows it and calls [onDueErrorShown]. */
    private val actionError = MutableStateFlow<Int?>(null)

    private val currentMonth = YearMonth.now(APP_ZONE)
    private val selection = MutableStateFlow(Selection(currentMonth))

    private val monthState: Flow<DashboardUiState> = selection
        .flatMapLatest { (month) ->
            combine(
                getMonthlySummary(month),
                getSpendingBreakdown(month),
                observeCategories(),
                observeSyncStatus(),
                getSalaryStatus(month),
            ) { summary, breakdown, categories, sync, salaryStatus ->
                val names = categories.associate { it.id to it.name }
                val content = uiStateOf(summary.incomeBySource.isNotEmpty() || breakdown.categories.isNotEmpty(), sync) {
                    DashboardData(
                        receivedMinor = summary.receivedIncomeMinor,
                        spentMinor = summary.spentMinor,
                        savedMinor = summary.netSavedMinor,
                        incomeBySource = summary.incomeBySource.toList().sortedByDescending { it.second },
                        committedMinor = breakdown.byNature.amountOf(SpendKind.COMMITTED),
                        discretionaryMinor = breakdown.byNature.amountOf(SpendKind.DISCRETIONARY),
                        topCategories = breakdown.categories.take(TOP_CATEGORIES).map {
                            RankedCategory(names[it.categoryId], it.spendKind, it.amountMinor, it.percent.roundToInt())
                        },
                    )
                }
                stateFor(month, content, salaryStatus)
            }
                .onStart { emit(stateFor(month, UiState.Loading)) }
                .catch { emit(stateFor(month, UiState.Error(mapErrorToMessage(it)))) }
        }

    // Sibling of monthState, not folded into it: goal progress is "as of today" and must not
    // restart every time the user pages to a different month (FR-96 live updates).
    private val goalState: Flow<UiState<GoalCardData>> =
        combine(observeActiveGoal(), getGoalProgress(), observeSyncStatus()) { goal, progress, sync ->
            val data = if (goal != null && progress != null) goalCardData(goal, progress) else null
            uiStateOf(data != null, sync) { data!! }
        }
            .onStart { emit(UiState.Loading) }
            .catch { emit(UiState.Error(mapErrorToMessage(it))) }

    // Sibling of monthState too: due items are as-of-today and must not restart when the user pages months.
    private val dueState: Flow<List<DueItemData>> =
        getDueRecurring()
            .map { templates -> templates.map { it.toDueItem() } }
            .onStart { emit(emptyList()) }
            .catch { emit(emptyList()) }

    /** The "how is this worked out" sheet; closed by default. */
    private val safeToSpendExplainerOpen = MutableStateFlow(false)

    // Sibling too: Safe to spend is "as of today" and the use case follows the calendar itself. A failure only
    // hides the card (null) - it must not blank the rest of the dashboard.
    private val safeToSpendState: Flow<SafeToSpend?> =
        getSafeToSpend()
            .map<SafeToSpend, SafeToSpend?> { it }
            .onStart { emit(null) }
            .catch { emit(null) }

    private val dashboardState: Flow<DashboardUiState> =
        combine(monthState, goalState, dueState, dueConfirm, actionError) { base, goal, due, confirm, error ->
            base.copy(goal = goal, due = due, dueConfirm = confirm, dueError = error)
        }

    val uiState: StateFlow<DashboardUiState> =
        combine(dashboardState, safeToSpendState, safeToSpendExplainerOpen) { base, safeToSpend, explainerOpen ->
            // The figure is for this month, so it is not shown while the user pages to a past month.
            val shown = safeToSpend.takeIf { base.month == currentMonth }
            base.copy(safeToSpend = shown, safeToSpendExplainerOpen = explainerOpen && shown != null)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), stateFor(currentMonth, UiState.Loading))

    fun onSafeToSpendClick() { safeToSpendExplainerOpen.value = true }

    fun onSafeToSpendDismiss() { safeToSpendExplainerOpen.value = false }

    /** Fixed items confirm in one tap; a variable item opens the sheet prefilled with its expected amount (#202). */
    fun onConfirmDue(templateId: String) {
        val item = uiState.value.due.firstOrNull { it.templateId == templateId } ?: return
        if (item.isVariableAmount) {
            dueConfirm.value = DueConfirmSheet(templateId, item.name, AmountInput.fromMinor(item.expectedMinor))
        } else {
            confirm(templateId, item.expectedMinor, item.dueDate)
        }
    }

    fun onDueConfirmAmountChange(value: String) =
        dueConfirm.update { it?.copy(amountText = AmountInput.sanitize(value), error = null) }

    fun onDueConfirmDismiss() { dueConfirm.value = null }

    fun onDueConfirmSave() {
        val sheet = dueConfirm.value ?: return
        if (sheet.isSaving) return
        val amount = AmountInput.toMinor(sheet.amountText)?.takeIf { it > 0 } ?: return
        val item = uiState.value.due.firstOrNull { it.templateId == sheet.templateId } ?: return
        dueConfirm.update { it?.copy(isSaving = true, error = null) }
        confirm(sheet.templateId, amount, item.dueDate)
    }

    fun onSkipDue(templateId: String) {
        val item = uiState.value.due.firstOrNull { it.templateId == templateId } ?: return
        viewModelScope.launch {
            val result = skipRecurring(templateId, item.dueDate)
            if (result is Result.Error) actionError.value = mapErrorToMessage(result.exception)
        }
    }

    fun onDueErrorShown() { actionError.value = null }

    private fun confirm(templateId: String, amountMinor: Long, dueDate: LocalDate) {
        // Book the expense in the month it settles, so a caught-up bill lands in its own month, not today.
        val occurredAt = dueDate.atTime(LocalTime.NOON).atZone(APP_ZONE).toInstant().toEpochMilli()
        viewModelScope.launch {
            // The sheet closes only once the write succeeded: a failure keeps the typed amount for a retry.
            when (val result = confirmRecurring(templateId, amountMinor, occurredAt, dueDate)) {
                is Result.Success -> dueConfirm.value = null
                is Result.Error -> {
                    val message = mapErrorToMessage(result.exception)
                    if (dueConfirm.value == null) actionError.value = message
                    else dueConfirm.update { it?.copy(isSaving = false, error = message) }
                }
            }
        }
    }

    private fun RecurringTemplate.toDueItem() =
        DueItemData(id, name, expectedAmountMinor, nextDueDate, isVariableAmount)

    fun onPreviousMonth() = selection.update { Selection(it.month.minusMonths(1)) }

    fun onNextMonth() = selection.update { if (it.month < currentMonth) Selection(it.month.plusMonths(1)) else it }

    /** A new [Selection] instance restarts the flows even though the month is the same. */
    fun onRetry() = selection.update { Selection(it.month) }

    private fun stateFor(
        month: YearMonth,
        content: UiState<DashboardData>,
        salaryStatus: SalaryStatus = SalaryStatus.NotDue,
    ) = DashboardUiState(
        month = month,
        canGoNext = month < currentMonth,
        content = content,
        salaryStatus = salaryStatus,
    )

    private fun List<NatureShare>.amountOf(kind: SpendKind) =
        firstOrNull { it.spendKind == kind }?.amountMinor ?: 0L

    private fun goalCardData(goal: Goal, progress: GoalProgress): GoalCardData {
        val percent = if (goal.targetLkrMinor <= 0L) {
            0
        } else {
            (progress.savedToDateMinor * 100 / goal.targetLkrMinor).coerceIn(0L, 100L).toInt()
        }
        val status = when {
            progress.isInfeasible -> GoalCardStatus.NEEDS_ADJUSTING
            else -> when (progress.status) {
                GoalStatus.NOT_ENOUGH_DATA -> GoalCardStatus.NOT_ENOUGH_DATA
                GoalStatus.AHEAD -> GoalCardStatus.AHEAD
                GoalStatus.ON_TRACK -> GoalCardStatus.ON_TRACK
                GoalStatus.BEHIND -> GoalCardStatus.BEHIND
            }
        }
        return GoalCardData(
            name = goal.name,
            savedToDateMinor = progress.savedToDateMinor,
            targetMinor = goal.targetLkrMinor,
            percent = percent,
            requiredMonthlyMinor = progress.requiredMonthlyMinor,
            actualPaceMinor = progress.actualPaceMinor,
            status = status,
            monthlyGapMinor = progress.monthlyGapMinor,
            projectedDate = progress.projectedDate,
            deadline = goal.deadline,
        )
    }

    // Not a data class: two selections of the same month must stay unequal so retry re-subscribes.
    private class Selection(val month: YearMonth) {
        operator fun component1() = month
    }

    private companion object {
        const val TOP_CATEGORIES = 5
    }
}
