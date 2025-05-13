package browserpicker.presentation.test.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import browserpicker.presentation.test.navigation.HomeRoute

/**
 * Onboarding Screen - First-time user experience.
 *
 * This screen guides new users through:
 * - App introduction and purpose
 * - Setting the app as default browser
 * - Explaining core features (browser selection, folders, etc.)
 * - Required permissions and setup
 *
 * It ensures users understand the app's functionality and
 * completes all necessary setup steps.
 *
 * Uses: OnboardingViewModel
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    navController: androidx.navigation.NavController,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Effect to navigate to home after setup is complete
    LaunchedEffect(uiState.setupComplete) {
        if (uiState.setupComplete) {
            navController.navigate(HomeRoute) { // Use the serializable route object
                popUpTo(HomeRoute) { inclusive = true } // Use the serializable route object
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browser Picker Setup") },
                actions = {
                    TextButton(onClick = { viewModel.skipOnboarding() }) {
                        Text("Skip")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Content area (70% of screen)
            Box(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Different content based on current step
                AnimatedContent(
                    targetState = uiState.currentStep,
                    label = "Step Animation"
                ) { step ->
                    when (step) {
                        OnboardingStep.WELCOME.ordinal -> WelcomeStep()
                        OnboardingStep.DEFAULT_BROWSER.ordinal -> DefaultBrowserStep(
                            isDefaultBrowser = uiState.isDefaultBrowser,
                            onSetDefaultBrowser = { viewModel.setAsDefaultBrowser() },
                            onOpenSettings = { viewModel.openBrowserPreferences() }
                        )
                        OnboardingStep.BROWSER_SELECTION.ordinal -> BrowserSelectionStep()
                        OnboardingStep.FOLDERS.ordinal -> FoldersStep()
                        OnboardingStep.COMPLETE.ordinal -> CompleteStep()
                        else -> WelcomeStep()
                    }
                }
            }

            // Navigation area (30% of screen)
            Box(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Step indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until OnboardingStep.values().size) {
                            StepIndicator(
                                isActive = i == uiState.currentStep,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    // Navigation buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Back button
                        if (uiState.currentStep > 0) {
                            Button(
                                onClick = { viewModel.previousStep() }
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Back")
                            }
                        } else {
                            Spacer(modifier = Modifier.width(100.dp)) // Space for alignment
                        }

                        // Next button
                        Button(
                            onClick = { viewModel.nextStep() }
                        ) {
                            if (uiState.currentStep < OnboardingStep.values().size - 1) {
                                Text("Next")
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                            } else {
                                Text("Get Started")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Welcome step - Introduction to the app
 */
@Composable
private fun WelcomeStep() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App icon or welcome illustration would go here
        // Placeholder for illustration
        Box(
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("App Icon", style = MaterialTheme.typography.headlineMedium)
        }

        Text(
            "Welcome to Browser Picker",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Choose the right browser for every link. Organize your browsing experience with bookmarks and blocked sites.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Default browser step - Setting the app as default browser
 */
@Composable
private fun DefaultBrowserStep(
    isDefaultBrowser: Boolean,
    onSetDefaultBrowser: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Default browser illustration would go here
        // Placeholder for illustration
        Box(
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Browser Icon", style = MaterialTheme.typography.headlineMedium)
        }

        Text(
            "Set as Default Browser",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "To intercept links, Browser Picker needs to be set as your default browser.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isDefaultBrowser) {
            Text(
                "✓ Browser Picker is your default browser",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Button(
                onClick = onSetDefaultBrowser
            ) {
                Text("Set as Default Browser")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onOpenSettings
            ) {
                Text("Open Browser Settings")
            }
        }
    }
}

/**
 * Browser selection step - Explaining how to choose browsers
 */
@Composable
private fun BrowserSelectionStep() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Browser selection illustration would go here
        // Placeholder for illustration
        Box(
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Selection Icon", style = MaterialTheme.typography.headlineMedium)
        }

        Text(
            "Choose Your Browser",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "When you click a link, Browser Picker will show all your installed browsers. Choose which one to use for each link.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "You can set preferred browsers for specific websites.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Folders step - Explaining bookmarks and blocked sites
 */
@Composable
private fun FoldersStep() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Folders illustration would go here
        // Placeholder for illustration
        Box(
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Folders Icon", style = MaterialTheme.typography.headlineMedium)
        }

        Text(
            "Organize Your Sites",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Bookmark your favorite sites or block unwanted ones. Organize them in folders for easy access.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "You can view analytics about your browsing habits.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Complete step - Ready to use the app
 */
@Composable
private fun CompleteStep() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Complete illustration would go here
        // Placeholder for illustration
        Box(
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Complete Icon", style = MaterialTheme.typography.headlineMedium)
        }

        Text(
            "You're All Set!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Browser Picker is ready to use. Click the button below to start browsing with control.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Step indicator dot
 */
@Composable
private fun StepIndicator(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .size(12.dp)
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .align(Alignment.Center)
                .background(
                    color = color,
                    shape = CircleShape
                )
        )
    }
}
