package dev.tuandoan.expensetracker.data.analytics

import dev.tuandoan.expensetracker.domain.analytics.AnalyticsEvent
import dev.tuandoan.expensetracker.domain.analytics.AnalyticsEventParam
import dev.tuandoan.expensetracker.domain.analytics.BackupFormat
import dev.tuandoan.expensetracker.domain.analytics.BuildType
import dev.tuandoan.expensetracker.domain.analytics.InsightRowType
import dev.tuandoan.expensetracker.domain.analytics.TransactionKind
import dev.tuandoan.expensetracker.domain.analytics.WidgetSize
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runtime parameter-validation test (PRD FR-A11) — belt-and-suspenders
 * over the compile-time FR-A7 guarantee from PR #115's sealed
 * [AnalyticsEvent] hierarchy.
 *
 * ### Why both guarantees exist
 *
 * PR #115's sealed types already make it *structurally impossible* for
 * a contributor to add a `String` or numeric parameter (e.g. a
 * `categoryName: String`, `amount: Long`, free-text note) to any event
 * — every event's parameters are typed as enum classes implementing
 * [AnalyticsEventParam].
 *
 * This test guards the layer below that: even granting the compile-time
 * invariant, we verify that the **observable wire output** of every
 * event, when routed through the real [FirebaseAnalyticsImpl], only
 * ever produces:
 *
 * 1. Event names from the PRD FR-A6 allowlist
 * 2. Parameter keys from the PRD FR-A6 allowlist
 * 3. Parameter values from the union of declared enum `wireValue`s
 *
 * If a future refactor accidentally broke the serialization (e.g. a
 * new event that smuggles a `Bundle.putString("note", ...)` through a
 * custom bypass), this test catches it. Combined with PR #116's
 * per-subtype mapping tests and PR #115's enum-shape tests, this
 * forms the complete FR-A7 defense layer.
 *
 * ### Why the runtime assertion isn't redundant
 *
 * A contributor who wanted to add a leak would need to:
 * 1. Modify the sealed `AnalyticsEvent` hierarchy (PR #115 tests fail)
 * 2. Modify `FirebaseAnalyticsImpl.toWire` (PR #116 tests fail)
 * 3. Modify this test's allowlists (this test would compile but
 *    surface the new key/value for human review)
 *
 * Step 3 is the last-line defense: the diff on the allowlists is a
 * loud, PR-reviewable signal that the privacy contract is expanding.
 */
class FirebaseAnalyticsImplParameterValidationTest {
    /**
     * One representative instance of every [AnalyticsEvent] subtype,
     * exercising every possible enum value for parameterised events.
     * If a future PR adds a new subtype, adding it to this list is
     * mandatory — the wire-value allowlist assertions iterate this
     * list and would silently miss the new event otherwise.
     *
     * The sealed-hierarchy completeness check at the end of the test
     * class makes "did you remember to add the new subtype?" a
     * test-failure, not a code-review fishing expedition.
     */
    private val exhaustiveEventInstances: List<AnalyticsEvent> =
        listOf(
            AnalyticsEvent.AppOpen(BuildType.DEBUG),
            AnalyticsEvent.AppOpen(BuildType.RELEASE),
            AnalyticsEvent.OnboardingCompleted,
            AnalyticsEvent.WidgetAdded(WidgetSize.SMALL),
            AnalyticsEvent.WidgetAdded(WidgetSize.MEDIUM),
            AnalyticsEvent.WidgetRemoved,
            AnalyticsEvent.TransactionAdded(TransactionKind.EXPENSE),
            AnalyticsEvent.TransactionAdded(TransactionKind.INCOME),
            AnalyticsEvent.BackupExported(BackupFormat.JSON),
            AnalyticsEvent.BackupExported(BackupFormat.ENCRYPTED),
            AnalyticsEvent.BackupImported(BackupFormat.JSON),
            AnalyticsEvent.BackupImported(BackupFormat.ENCRYPTED),
            AnalyticsEvent.InsightShown(InsightRowType.BIGGEST_MOVER),
            AnalyticsEvent.InsightShown(InsightRowType.DAILY_PACE),
            AnalyticsEvent.InsightShown(InsightRowType.NO_BUDGET_FALLBACK),
            AnalyticsEvent.InsightShown(InsightRowType.DAY_OF_MONTH),
        )

    /**
     * PRD FR-A6 event-name allowlist. Any event-name that reaches the
     * wrapper outside this set is a privacy-contract violation.
     */
    private val allowedEventNames: Set<String> =
        setOf(
            FirebaseAnalyticsImpl.EVENT_APP_OPEN,
            FirebaseAnalyticsImpl.EVENT_ONBOARDING_COMPLETED,
            FirebaseAnalyticsImpl.EVENT_WIDGET_ADDED,
            FirebaseAnalyticsImpl.EVENT_WIDGET_REMOVED,
            FirebaseAnalyticsImpl.EVENT_TRANSACTION_ADDED,
            FirebaseAnalyticsImpl.EVENT_BACKUP_EXPORTED,
            FirebaseAnalyticsImpl.EVENT_BACKUP_IMPORTED,
            FirebaseAnalyticsImpl.EVENT_INSIGHT_SHOWN,
        )

    /**
     * PRD FR-A6 parameter-key allowlist. Any parameter key outside this
     * set is a privacy-contract violation — an accidental new key would
     * be caught here regardless of what value it carried.
     */
    private val allowedParamKeys: Set<String> =
        setOf(
            FirebaseAnalyticsImpl.PARAM_BUILD_TYPE,
            FirebaseAnalyticsImpl.PARAM_WIDGET_SIZE,
            FirebaseAnalyticsImpl.PARAM_TYPE,
            FirebaseAnalyticsImpl.PARAM_FORMAT,
            FirebaseAnalyticsImpl.PARAM_ROW_TYPE,
        )

    /**
     * Union of every enum's `wireValue`. Any parameter value outside
     * this set would represent either:
     * - A new enum variant not yet disclosed in the privacy policy
     * - A raw string smuggled through a custom `toWire` branch
     * Both are bugs this test catches.
     */
    private val allowedParamValues: Set<String> =
        (
            BuildType.entries +
                WidgetSize.entries +
                TransactionKind.entries +
                BackupFormat.entries +
                InsightRowType.entries
        ).map { it.wireValue }.toSet()

    @Test
    fun everyAnalyticsEventSubtype_isRepresentedInExhaustiveList() {
        // The exhaustive-list enumeration above is the load-bearing
        // guard — if a future PR adds a new subtype but forgets to add
        // an instance here, the rest of this class's tests would miss
        // the new event entirely. Using reflection on sealed subclasses
        // turns that silent gap into a test-failure.
        val declaredSubtypes = AnalyticsEvent::class.sealedSubclasses.map { it.simpleName }.toSet()
        val listedSubtypes = exhaustiveEventInstances.map { it::class.simpleName }.toSet()
        assertTrue(
            "Missing ${declaredSubtypes - listedSubtypes} from exhaustiveEventInstances " +
                "— add representative instance(s) with every enum variant",
            (declaredSubtypes - listedSubtypes).isEmpty(),
        )
    }

    @Test
    fun everyLoggedEvent_usesAllowedEventName() {
        val wrapper = RecordingFirebaseAnalyticsWrapper()
        val impl = FirebaseAnalyticsImpl(wrapper)

        exhaustiveEventInstances.forEach { event -> impl.logEvent(event) }

        wrapper.recordedCalls.forEach { (name, _) ->
            assertTrue(
                "Event '$name' is not in FR-A6 allowlist $allowedEventNames",
                name in allowedEventNames,
            )
        }
    }

    @Test
    fun everyLoggedEvent_usesAllowedParamKeys() {
        val wrapper = RecordingFirebaseAnalyticsWrapper()
        val impl = FirebaseAnalyticsImpl(wrapper)

        exhaustiveEventInstances.forEach { event -> impl.logEvent(event) }

        wrapper.recordedCalls.forEach { (name, params) ->
            params.keys.forEach { key ->
                assertTrue(
                    "Event '$name' used param key '$key' outside FR-A6 allowlist $allowedParamKeys",
                    key in allowedParamKeys,
                )
            }
        }
    }

    @Test
    fun everyLoggedEvent_usesAllowedParamValues() {
        // The privacy-critical assertion: ANY string value reaching the
        // wrapper must be a declared enum wireValue. A raw amount,
        // category name, or note text would fail this assertion loudly.
        val wrapper = RecordingFirebaseAnalyticsWrapper()
        val impl = FirebaseAnalyticsImpl(wrapper)

        exhaustiveEventInstances.forEach { event -> impl.logEvent(event) }

        wrapper.recordedCalls.forEach { (name, params) ->
            params.values.forEach { value ->
                assertTrue(
                    "Event '$name' emitted param value '$value' outside the declared " +
                        "enum wireValue allowlist $allowedParamValues",
                    value in allowedParamValues,
                )
            }
        }
    }

    @Test
    fun everyLoggedEvent_carriesAtMostOneParameter() {
        // PRD FR-A6 pins each event to at most one parameter. A future
        // `putString`-style bypass that added a second key would violate
        // the privacy-policy disclosure even if both keys+values were
        // individually allowlisted.
        val wrapper = RecordingFirebaseAnalyticsWrapper()
        val impl = FirebaseAnalyticsImpl(wrapper)

        exhaustiveEventInstances.forEach { event -> impl.logEvent(event) }

        wrapper.recordedCalls.forEach { (name, params) ->
            assertTrue(
                "Event '$name' emitted ${params.size} params; FR-A6 limits each " +
                    "event to at most one param. params=$params",
                params.size <= 1,
            )
        }
    }
}

/**
 * In-test implementation of [FirebaseAnalyticsWrapper] that records every
 * `logEvent(name, params)` call so the test can inspect the wire output.
 * `setAnalyticsCollectionEnabled` is a no-op — this test isn't about the
 * collection-toggle path (that's covered by PR #116's wrapper tests).
 */
private class RecordingFirebaseAnalyticsWrapper : FirebaseAnalyticsWrapper {
    val recordedCalls: MutableList<Pair<String, Map<String, String>>> = mutableListOf()

    override fun logEvent(
        name: String,
        params: Map<String, String>,
    ) {
        recordedCalls.add(name to params)
    }

    override fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        // No-op: collection-toggle path isn't under test here.
    }
}
