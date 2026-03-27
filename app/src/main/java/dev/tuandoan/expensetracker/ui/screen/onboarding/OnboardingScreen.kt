package dev.tuandoan.expensetracker.ui.screen.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val titleRes: Int,
    val subtitleRes: Int,
)

private val pages =
    listOf(
        OnboardingPage(
            icon = Icons.Outlined.AccountBalanceWallet,
            titleRes = R.string.onboarding_title_welcome,
            subtitleRes = R.string.onboarding_subtitle_welcome,
        ),
        OnboardingPage(
            icon = Icons.Outlined.ReceiptLong,
            titleRes = R.string.onboarding_title_transactions,
            subtitleRes = R.string.onboarding_subtitle_transactions,
        ),
        OnboardingPage(
            icon = Icons.Outlined.Savings,
            titleRes = R.string.onboarding_title_budget,
            subtitleRes = R.string.onboarding_subtitle_budget,
        ),
        OnboardingPage(
            icon = Icons.Outlined.CloudUpload,
            titleRes = R.string.onboarding_title_backup,
            subtitleRes = R.string.onboarding_subtitle_backup,
        ),
    )

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

    Scaffold(
        modifier = modifier,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Skip button (top-right, hidden on last page)
            if (!isLastPage) {
                TextButton(
                    onClick = {
                        viewModel.completeOnboarding()
                        onComplete()
                    },
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(DesignSystemSpacing.large),
                ) {
                    Text(text = stringResource(R.string.onboarding_skip))
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                ) { page ->
                    OnboardingPageContent(pages[page])
                }

                // Page indicator dots
                val pageIndicatorDesc =
                    stringResource(
                        R.string.a11y_page_indicator,
                        pagerState.currentPage + 1,
                        pages.size,
                    )
                Row(
                    modifier =
                        Modifier
                            .padding(bottom = DesignSystemSpacing.large)
                            .semantics {
                                contentDescription = pageIndicatorDesc
                            },
                    horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
                ) {
                    repeat(pages.size) { index ->
                        PageIndicatorDot(isSelected = pagerState.currentPage == index)
                    }
                }

                // Bottom button
                if (isLastPage) {
                    Button(
                        onClick = {
                            viewModel.completeOnboarding()
                            onComplete()
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = DesignSystemSpacing.xl,
                                    vertical = DesignSystemSpacing.large,
                                ),
                    ) {
                        Text(text = stringResource(R.string.onboarding_get_started))
                    }
                } else {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = DesignSystemSpacing.xl,
                                    vertical = DesignSystemSpacing.large,
                                ),
                    ) {
                        Text(text = stringResource(R.string.onboarding_next))
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = DesignSystemSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(DesignSystemSpacing.xl))

        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(DesignSystemSpacing.medium))

        Text(
            text = stringResource(page.subtitleRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PageIndicatorDot(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val color =
        if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    Box(
        modifier =
            modifier
                .size(if (isSelected) 10.dp else 8.dp),
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = color)
        }
    }
}
