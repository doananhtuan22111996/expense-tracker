package dev.tuandoan.expensetracker.data.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ThemePreferencesRepositoryImplTest {
    private lateinit var repository: FakeThemePreferencesRepository

    @Before
    fun setup() {
        repository = FakeThemePreferencesRepository()
    }

    @Test
    fun defaultThemePreference_isSystem() =
        runTest {
            val pref = repository.themePreference.first()
            assertEquals(ThemePreference.SYSTEM, pref)
        }

    @Test
    fun setThemeDark_persistsAndEmitsDark() =
        runTest {
            repository.setTheme(ThemePreference.DARK)

            val pref = repository.themePreference.first()
            assertEquals(ThemePreference.DARK, pref)
        }

    @Test
    fun setThemeLight_persistsAndEmitsLight() =
        runTest {
            repository.setTheme(ThemePreference.LIGHT)

            val pref = repository.themePreference.first()
            assertEquals(ThemePreference.LIGHT, pref)
        }

    @Test
    fun setThemeSystem_persistsAndEmitsSystem() =
        runTest {
            repository.setTheme(ThemePreference.DARK)
            repository.setTheme(ThemePreference.SYSTEM)

            val pref = repository.themePreference.first()
            assertEquals(ThemePreference.SYSTEM, pref)
        }
}

class FakeThemePreferencesRepository(
    initialPreference: ThemePreference = ThemePreference.SYSTEM,
) : ThemePreferencesRepository {
    private val _themePreference = MutableStateFlow(initialPreference)
    override val themePreference: Flow<ThemePreference> = _themePreference

    override suspend fun setTheme(pref: ThemePreference) {
        _themePreference.value = pref
    }
}
