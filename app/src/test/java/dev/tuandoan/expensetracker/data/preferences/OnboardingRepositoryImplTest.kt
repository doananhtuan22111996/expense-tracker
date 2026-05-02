package dev.tuandoan.expensetracker.data.preferences

import dev.tuandoan.expensetracker.testutil.FakeOnboardingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnboardingRepositoryImplTest {
    private lateinit var repository: FakeOnboardingRepository

    @Before
    fun setup() {
        repository = FakeOnboardingRepository()
    }

    @Test
    fun defaultOnboardingComplete_isFalse() =
        runTest {
            val result = repository.isOnboardingComplete.first()
            assertFalse(result)
        }

    @Test
    fun markOnboardingComplete_writesTrue() =
        runTest {
            repository.markOnboardingComplete()

            val result = repository.isOnboardingComplete.first()
            assertTrue(result)
        }

    @Test
    fun subsequentReads_afterMarkComplete_emitTrue() =
        runTest {
            repository.markOnboardingComplete()

            assertTrue(repository.isOnboardingComplete.first())
            assertTrue(repository.isOnboardingComplete.first())
        }
}
