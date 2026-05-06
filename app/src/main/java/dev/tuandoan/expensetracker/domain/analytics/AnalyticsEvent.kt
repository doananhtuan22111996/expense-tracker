package dev.tuandoan.expensetracker.domain.analytics

/**
 * Typed enumeration of the events this app is allowed to emit (v3.11.0,
 * PRD FR-A6). The sealed shape is the privacy contract: **the ONLY events
 * the app can log are subtypes of this interface, and the ONLY parameters
 * are the enum-valued fields on each subtype.**
 *
 * That design makes FR-A7 compliance structural rather than procedural —
 * a future contributor cannot accidentally smuggle a category name, a
 * transaction amount, or free-text notes into an event without first
 * adding a field of that type here, which would not pass code review.
 *
 * When the event is serialised to the Firebase Analytics SDK (in
 * `FirebaseAnalyticsImpl`, Task 4.4), the event name is the kebab-cased
 * subtype name (`app_open`, `widget_added`, …) and the parameters are
 * the enum fields' `wireValue` strings. No other parameters are attached.
 *
 * Out of scope by design (PRD §6): user properties, user IDs, e-commerce
 * events, custom audiences.
 */
sealed interface AnalyticsEvent {
    /**
     * Fired at `MainActivity.onCreate` first frame. The only parameter is
     * the build type so I can separate release-user signal from debug
     * noise during development — not user-identifying.
     */
    data class AppOpen(
        val buildType: BuildType,
    ) : AnalyticsEvent

    /** Fired when the user completes the onboarding flow for the first time. */
    data object OnboardingCompleted : AnalyticsEvent

    /** Fired when the user adds a home-screen widget (size captured at add time). */
    data class WidgetAdded(
        val size: WidgetSize,
    ) : AnalyticsEvent

    /** Fired when the user removes a home-screen widget. */
    data object WidgetRemoved : AnalyticsEvent

    /**
     * Fired on a successful transaction save. **Only the type** (expense
     * vs. income) is captured — never the amount, category, note, or
     * currency. FR-A7 invariant.
     */
    data class TransactionAdded(
        val type: TransactionKind,
    ) : AnalyticsEvent

    /** Fired on a successful backup export. Captures only the format. */
    data class BackupExported(
        val format: BackupFormat,
    ) : AnalyticsEvent

    /** Fired on a successful backup import. Captures only the format. */
    data class BackupImported(
        val format: BackupFormat,
    ) : AnalyticsEvent

    /**
     * Fired on the first populated-Insights emission per Summary visit
     * (de-duplicated by the VM to avoid per-recomposition spam). Captures
     * only which row type surfaced — never the actual insight content.
     */
    data class InsightShown(
        val rowType: InsightRowType,
    ) : AnalyticsEvent
}

/**
 * Base for enum fields used inside [AnalyticsEvent] subtypes. The
 * [wireValue] is the lowercase string that ships to Firebase Analytics
 * when the event is logged — it's a **non-reversible label**, not a user
 * identifier, and is part of the app's public privacy disclosure (see
 * `privacy-policy.md` Section 2).
 */
sealed interface AnalyticsEventParam {
    val wireValue: String
}

enum class BuildType(
    override val wireValue: String,
) : AnalyticsEventParam {
    DEBUG("debug"),
    RELEASE("release"),
}

enum class WidgetSize(
    override val wireValue: String,
) : AnalyticsEventParam {
    SMALL("small"),
    MEDIUM("medium"),
}

enum class TransactionKind(
    override val wireValue: String,
) : AnalyticsEventParam {
    EXPENSE("expense"),
    INCOME("income"),
}

enum class BackupFormat(
    override val wireValue: String,
) : AnalyticsEventParam {
    JSON("json"),
    ENCRYPTED("encrypted"),
}

enum class InsightRowType(
    override val wireValue: String,
) : AnalyticsEventParam {
    BIGGEST_MOVER("biggest_mover"),
    DAILY_PACE("daily_pace"),
    NO_BUDGET_FALLBACK("no_budget_fallback"),
    DAY_OF_MONTH("day_of_month"),
}
