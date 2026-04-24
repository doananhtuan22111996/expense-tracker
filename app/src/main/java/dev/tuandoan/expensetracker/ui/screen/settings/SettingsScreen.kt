package dev.tuandoan.expensetracker.ui.screen.settings

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.util.AppInfo
import dev.tuandoan.expensetracker.data.preferences.ThemePreference
import dev.tuandoan.expensetracker.domain.model.CurrencyDefinition
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.ui.component.PasswordDialog
import dev.tuandoan.expensetracker.ui.component.SectionTitle
import dev.tuandoan.expensetracker.ui.theme.DesignSystemElevation
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToRecurring: () -> Unit = {},
    bottomContentPadding: Dp = 0.dp,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeRecurringCount by viewModel.activeRecurringCount.collectAsStateWithLifecycle()
    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
    val analyticsConsent by viewModel.analyticsConsent.collectAsStateWithLifecycle()
    val budgetAlertsEnabled by viewModel.budgetAlertsEnabled.collectAsStateWithLifecycle()
    val encryptBackupsEnabled by viewModel.encryptBackupsEnabled.collectAsStateWithLifecycle()
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showFeedbackSheet by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var hasNotificationPermission by remember { mutableStateOf(checkNotificationPermission(context)) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Re-check permission when returning from system settings.
    // If permission was revoked while alerts were enabled, disable them to stay in sync.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    val currentPermission = checkNotificationPermission(context)
                    hasNotificationPermission = currentPermission
                    if (!currentPermission && budgetAlertsEnabled) {
                        viewModel.setBudgetAlertsEnabled(false)
                    }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val exportJsonLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
        ) { uri ->
            uri?.let { viewModel.onExportUriReady(it) }
        }

    val exportEncryptedLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        ) { uri ->
            uri?.let { viewModel.onExportUriReady(it) }
        }

    val importLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            uri?.let { viewModel.onRestoreFileSelected(it) }
        }

    val csvExportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/csv"),
        ) { uri ->
            uri?.let { viewModel.exportCsv(it) }
        }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            hasNotificationPermission = granted
            if (granted) {
                viewModel.setBudgetAlertsEnabled(true)
            }
        }

    LaunchedEffect(Unit) {
        viewModel.exportLaunchEvents.collect { event ->
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            when (event) {
                ExportLaunchEvent.Encrypted ->
                    exportEncryptedLauncher.launch("expense-tracker-backup_$date.etbackup")
                ExportLaunchEvent.Plain ->
                    exportJsonLauncher.launch("expense-tracker-backup_$date.json")
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.backupMessage) {
        uiState.backupMessage?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            viewModel.clearBackupMessage()
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_title_settings)) },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = bottomContentPadding),
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = DesignSystemSpacing.screenPadding)
                    .padding(bottom = bottomContentPadding)
                    .verticalScroll(rememberScrollState()),
        ) {
            // Preferences Section
            SettingsSection(title = stringResource(R.string.settings_preferences)) {
                // Theme picker
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(DesignSystemSpacing.large),
                ) {
                    Text(
                        text = stringResource(R.string.settings_theme_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = DesignSystemSpacing.small),
                    )

                    val themeOptions =
                        listOf(
                            ThemePreference.LIGHT to stringResource(R.string.settings_theme_light),
                            ThemePreference.DARK to stringResource(R.string.settings_theme_dark),
                            ThemePreference.SYSTEM to stringResource(R.string.settings_theme_system),
                        )

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        themeOptions.forEachIndexed { index, (pref, label) ->
                            SegmentedButton(
                                selected = themePreference == pref,
                                onClick = { viewModel.setTheme(pref) },
                                shape =
                                    SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = themeOptions.size,
                                    ),
                            ) {
                                Text(text = label)
                            }
                        }
                    }
                }

                HorizontalDivider()

                val selectedCurrency = SupportedCurrencies.byCode(uiState.selectedCurrencyCode)
                val displayText =
                    if (selectedCurrency != null) {
                        "${selectedCurrency.code} - ${selectedCurrency.displayName} ${selectedCurrency.symbol}"
                    } else {
                        uiState.selectedCurrencyCode
                    }

                val currencyA11y = stringResource(R.string.a11y_default_currency, displayText)
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { showCurrencyDialog = true }
                            .padding(DesignSystemSpacing.large)
                            .semantics {
                                contentDescription = currencyA11y
                            },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_default_currency),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = DesignSystemSpacing.xs),
                        )
                    }
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    text = stringResource(R.string.settings_currency_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier.padding(
                            start = DesignSystemSpacing.large,
                            end = DesignSystemSpacing.large,
                            bottom = DesignSystemSpacing.large,
                        ),
                )

                HorizontalDivider()

                val manageCategoriesLabel = stringResource(R.string.manage_categories)
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToCategories() }
                            .padding(DesignSystemSpacing.large)
                            .semantics {
                                contentDescription = manageCategoriesLabel
                            },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.manage_categories),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.manage_categories_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = DesignSystemSpacing.xs),
                        )
                    }
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                HorizontalDivider()

                val recurringLabel = stringResource(R.string.recurring_transactions)
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToRecurring() }
                            .padding(DesignSystemSpacing.large)
                            .semantics {
                                contentDescription = recurringLabel
                            },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.recurring_transactions),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.recurring_transactions_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = DesignSystemSpacing.xs),
                        )
                        Text(
                            text =
                                if (activeRecurringCount > 0) {
                                    stringResource(
                                        R.string.recurring_active_count,
                                        activeRecurringCount,
                                    )
                                } else {
                                    stringResource(R.string.recurring_none_set)
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                if (activeRecurringCount > 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            modifier = Modifier.padding(top = DesignSystemSpacing.xs),
                        )
                    }
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Notifications Section
            SettingsSection(title = stringResource(R.string.settings_notifications)) {
                val subtitleText =
                    if (!hasNotificationPermission && !budgetAlertsEnabled) {
                        stringResource(R.string.settings_budget_alerts_permission)
                    } else {
                        stringResource(R.string.settings_budget_alerts_subtitle)
                    }
                val toggleStateLabel =
                    if (budgetAlertsEnabled) {
                        stringResource(R.string.a11y_on)
                    } else {
                        stringResource(R.string.a11y_off)
                    }
                val budgetAlertsA11y =
                    stringResource(R.string.a11y_budget_alerts_toggle, toggleStateLabel)

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(DesignSystemSpacing.large)
                            .semantics {
                                contentDescription = budgetAlertsA11y
                            },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_budget_alerts_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = DesignSystemSpacing.xs),
                        )
                    }
                    Switch(
                        checked = budgetAlertsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !hasNotificationPermission
                                ) {
                                    val activity = context.findActivity()
                                    if (activity != null &&
                                        activity.shouldShowRequestPermissionRationale(
                                            Manifest.permission.POST_NOTIFICATIONS,
                                        )
                                    ) {
                                        showPermissionRationale = true
                                    } else {
                                        notificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS,
                                        )
                                    }
                                } else {
                                    viewModel.setBudgetAlertsEnabled(true)
                                }
                            } else {
                                viewModel.setBudgetAlertsEnabled(false)
                            }
                        },
                    )
                }
            }

            // Backup & Restore Section
            SettingsSection(title = stringResource(R.string.settings_backup_restore)) {
                val isBusy = uiState.backupOperation != BackupOperation.Idle
                val isExporting = uiState.backupOperation == BackupOperation.Exporting
                val isImporting = uiState.backupOperation == BackupOperation.Importing

                val exportA11y =
                    if (isExporting) {
                        stringResource(R.string.a11y_export_backup_busy)
                    } else {
                        stringResource(R.string.a11y_export_backup)
                    }
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isBusy) {
                                viewModel.onExportClicked()
                            }.padding(DesignSystemSpacing.large)
                            .semantics {
                                contentDescription = exportA11y
                            },
                    horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.FileUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_export_backup),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.settings_export_backup_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = DesignSystemSpacing.xs),
                        )
                    }
                    if (isExporting) {
                        TextButton(onClick = { viewModel.cancelOperation() }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }

                if (isExporting) {
                    BackupProgressBar(progress = uiState.backupProgress)
                }

                HorizontalDivider()

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(DesignSystemSpacing.large),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_encrypt_backup),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.settings_encrypt_backup_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = DesignSystemSpacing.xs),
                        )
                    }
                    Switch(
                        // Optimistically render ON while the forgotten-password warning
                        // dialog is up; otherwise the Switch snaps back under the dialog
                        // (the DataStore write is gated on confirming) and users see a
                        // confusing bounce. If the user cancels, pendingEncryptToggleAck
                        // flips false and the Switch returns to its real (OFF) state.
                        checked = encryptBackupsEnabled || uiState.pendingEncryptToggleAck,
                        onCheckedChange = { viewModel.setEncryptBackupsEnabled(it) },
                        enabled = !isBusy,
                    )
                }

                HorizontalDivider()

                val csvExportLabel = stringResource(R.string.export_csv)
                val csvExportSubtitle = stringResource(R.string.export_csv_subtitle)
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isBusy) {
                                val date =
                                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                csvExportLauncher.launch("expense-tracker-export-$date.csv")
                            }.padding(DesignSystemSpacing.large)
                            .semantics {
                                contentDescription = csvExportLabel
                            },
                    horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.FileUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = csvExportLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = csvExportSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = DesignSystemSpacing.xs),
                        )
                    }
                }

                HorizontalDivider()

                val importA11y =
                    if (isImporting) {
                        stringResource(R.string.a11y_import_backup_busy)
                    } else {
                        stringResource(R.string.a11y_import_backup)
                    }
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isBusy) {
                                importLauncher.launch(
                                    arrayOf("application/json", "application/gzip", "application/octet-stream"),
                                )
                            }.padding(DesignSystemSpacing.large)
                            .semantics {
                                contentDescription = importA11y
                            },
                    horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_import_backup),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.settings_import_backup_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = DesignSystemSpacing.xs),
                        )
                    }
                    if (isImporting) {
                        TextButton(onClick = { viewModel.cancelOperation() }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }

                if (isImporting) {
                    BackupProgressBar(progress = uiState.backupProgress)
                }

                Text(
                    text = stringResource(R.string.settings_backup_privacy_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier.padding(
                            start = DesignSystemSpacing.large,
                            end = DesignSystemSpacing.large,
                            bottom = DesignSystemSpacing.large,
                        ),
                )
            }

            // Feedback Section
            SettingsSection(title = stringResource(R.string.settings_send_feedback)) {
                val feedbackLabel = stringResource(R.string.settings_send_feedback)
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { showFeedbackSheet = true }
                            .padding(DesignSystemSpacing.large)
                            .semantics {
                                contentDescription = feedbackLabel
                            },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_send_feedback),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                HorizontalDivider()

                // Share App
                val shareText = stringResource(R.string.share_app_text)
                val shareChooserTitle = stringResource(R.string.share_app_chooser_title)
                val shareLabel = stringResource(R.string.settings_share_app)
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent =
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                context.startActivity(
                                    Intent.createChooser(intent, shareChooserTitle),
                                )
                            }.padding(DesignSystemSpacing.large)
                            .semantics {
                                contentDescription = shareLabel
                            },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_share_app),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // More Apps Section
            MoreAppsSection()

            // App Information Section
            SettingsSection(title = stringResource(R.string.settings_app_information)) {
                SettingsItem(
                    title = stringResource(R.string.settings_version),
                    subtitle = AppInfo.getFullVersionInfo(),
                )

                HorizontalDivider()

                SettingsItem(
                    title = stringResource(R.string.settings_contact_email),
                    subtitle = "doananhtuan22111996@gmail.com",
                )
            }

            // Privacy Section
            SettingsSection(title = stringResource(R.string.settings_privacy)) {
                // Anonymous crash reporting toggle
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(DesignSystemSpacing.large),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_analytics_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.settings_analytics_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = DesignSystemSpacing.xs),
                        )
                    }
                    Switch(
                        checked = analyticsConsent,
                        onCheckedChange = { viewModel.setAnalyticsConsent(it) },
                    )
                }

                HorizontalDivider()

                // Privacy Policy link
                SettingsExternalLinkRow(
                    label = stringResource(R.string.settings_privacy_policy),
                    url = stringResource(R.string.privacy_policy_url),
                )

                HorizontalDivider()

                // Terms of Service link
                SettingsExternalLinkRow(
                    label = stringResource(R.string.settings_terms_of_service),
                    url = stringResource(R.string.terms_of_service_url),
                )
            }

            // About Section
            SettingsSection(title = stringResource(R.string.settings_about)) {
                Column(modifier = Modifier.padding(DesignSystemSpacing.large)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = DesignSystemSpacing.small),
                    )
                    Text(
                        text = stringResource(R.string.settings_about_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = DesignSystemSpacing.medium),
                    )

                    // Application details for support/debugging
                    if (AppInfo.isDebugBuild()) {
                        Text(
                            text = stringResource(R.string.settings_debug_info),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = DesignSystemSpacing.xs),
                        )
                        Text(
                            text =
                                "Application ID: ${AppInfo.getApplicationId()}\n" +
                                    "Build Type: ${AppInfo.getBuildType()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }

    // Currency Selection Dialog
    if (showCurrencyDialog) {
        CurrencySelectionDialog(
            availableCurrencies = uiState.availableCurrencies,
            selectedCurrencyCode = uiState.selectedCurrencyCode,
            onCurrencySelected = { code ->
                viewModel.onCurrencySelected(code)
                showCurrencyDialog = false
            },
            onDismiss = { showCurrencyDialog = false },
        )
    }

    // Restore Confirmation Dialog
    if (uiState.pendingRestoreUri != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRestoreConfirmation() },
            title = { Text(stringResource(R.string.settings_restore_title)) },
            text = {
                Text(stringResource(R.string.settings_restore_message))
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmRestore() },
                ) {
                    Text(
                        stringResource(R.string.settings_replace_all),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRestoreConfirmation() }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // Export Password Dialog (encrypted backups only)
    if (uiState.pendingExportUri != null) {
        PasswordDialog(
            title = stringResource(R.string.settings_backup_password_title),
            message = stringResource(R.string.settings_backup_password_message),
            confirmLabel = stringResource(R.string.settings_backup_password_encrypt_action),
            onConfirm = { password ->
                viewModel.onExportPasswordConfirmed(password)
                password.fill('\u0000')
            },
            onDismiss = { viewModel.dismissExportPasswordDialog() },
        )
    }

    // Import Password Dialog (surfaced when the picked file's first 4 bytes are ETBK)
    if (uiState.pendingImportDecryptUri != null) {
        PasswordDialog(
            title = stringResource(R.string.settings_backup_import_password_title),
            message = stringResource(R.string.settings_backup_import_password_message),
            confirmLabel = stringResource(R.string.settings_backup_import_password_decrypt_action),
            requireConfirm = false,
            errorMessage = uiState.importPasswordError?.asString(context),
            onConfirm = { password ->
                viewModel.onImportPasswordConfirmed(password)
                password.fill(' ')
            },
            onDismiss = { viewModel.dismissImportPasswordDialog() },
        )
    }

    // Encrypt-Backup Warning Dialog (one-time, surfaced when the user first flips
    // the encrypt toggle on — persisting the toggle is gated on confirming here).
    if (uiState.pendingEncryptToggleAck) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPasswordWarning() },
            title = { Text(stringResource(R.string.settings_backup_warning_title)) },
            text = { Text(stringResource(R.string.settings_backup_warning_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmPasswordWarning() }) {
                    Text(stringResource(R.string.settings_backup_warning_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPasswordWarning() }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // Permission Rationale Dialog
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text(stringResource(R.string.alert_rationale_title)) },
            text = { Text(stringResource(R.string.alert_rationale_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionRationale = false
                        val intent =
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                        context.startActivity(intent)
                    },
                ) {
                    Text(stringResource(R.string.alert_rationale_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text(stringResource(R.string.alert_rationale_not_now))
                }
            },
        )
    }

    // Feedback Bottom Sheet
    if (showFeedbackSheet) {
        FeedbackBottomSheet(
            sheetState = rememberModalBottomSheetState(),
            onDismiss = { showFeedbackSheet = false },
            onPositiveFeedback = {
                // In-app review would be triggered here when Play Review is available
            },
        )
    }
}

@Composable
private fun CurrencySelectionDialog(
    availableCurrencies: List<CurrencyDefinition>,
    selectedCurrencyCode: String,
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_select_currency)) },
        text = {
            LazyColumn {
                items(availableCurrencies, key = { it.code }) { currency ->
                    val isSelected = currency.code == selectedCurrencyCode
                    val currencyItemA11y =
                        if (isSelected) {
                            stringResource(
                                R.string.a11y_currency_currently_selected,
                                currency.code,
                                currency.displayName,
                            )
                        } else {
                            stringResource(R.string.a11y_select_currency_item, currency.code, currency.displayName)
                        }
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onCurrencySelected(currency.code) }
                                .padding(DesignSystemSpacing.medium)
                                .semantics {
                                    contentDescription = currencyItemA11y
                                },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${currency.code} - ${currency.displayName} ${currency.symbol}",
                            style = MaterialTheme.typography.bodyLarge,
                            color =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.padding(bottom = DesignSystemSpacing.large)) {
        SectionTitle(title = title)

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = DesignSystemElevation.low),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(DesignSystemSpacing.large),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = DesignSystemSpacing.xs),
        )
    }
}

@Composable
private fun MoreAppsSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val taskTrackerPackage = "dev.tuandoan.tasktracker"

    SettingsSection(
        title = stringResource(R.string.settings_more_apps),
        modifier = modifier,
    ) {
        val viewOnPlayStore = stringResource(R.string.cross_promo_view_on_play_store)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=$taskTrackerPackage"),
                                ),
                            )
                        } catch (e: ActivityNotFoundException) {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(
                                        "https://play.google.com/store/apps/details?id=$taskTrackerPackage",
                                    ),
                                ),
                            )
                        }
                    }.padding(DesignSystemSpacing.large)
                    .semantics {
                        contentDescription = viewOnPlayStore
                    },
            horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cross_promo_task_tracker_name),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.cross_promo_task_tracker_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = DesignSystemSpacing.xs),
                )
                Text(
                    text = stringResource(R.string.cross_promo_view_on_play_store),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = DesignSystemSpacing.small),
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun BackupProgressBar(
    progress: Float?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DesignSystemSpacing.large,
                    vertical = DesignSystemSpacing.small,
                ),
    ) {
        val displayProgress = progress ?: 0f
        val progressA11y = stringResource(R.string.a11y_backup_progress, (displayProgress * 100).toInt())
        LinearProgressIndicator(
            progress = { displayProgress },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = progressA11y
                    },
        )
        Text(
            text = "${(displayProgress * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = DesignSystemSpacing.xs),
        )
    }
}

private fun checkNotificationPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

@Composable
private fun SettingsExternalLinkRow(
    label: String,
    url: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val snackbarMessage = stringResource(R.string.settings_no_browser, url)
    val opensInBrowser = stringResource(R.string.settings_opens_in_browser)
    val a11yLabel = "$label. $opensInBrowser"
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (_: ActivityNotFoundException) {
                        android.widget.Toast
                            .makeText(context, snackbarMessage, android.widget.Toast.LENGTH_LONG)
                            .show()
                    }
                }.padding(DesignSystemSpacing.large)
                .semantics {
                    contentDescription = a11yLabel
                },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = opensInBrowser,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
