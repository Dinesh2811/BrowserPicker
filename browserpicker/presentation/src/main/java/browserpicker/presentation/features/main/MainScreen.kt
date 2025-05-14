package browserpicker.presentation.features.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.compose.rememberNavController
import browserpicker.presentation.features.browserpicker.AppPickerSheetContent
import browserpicker.presentation.navigation.BrowserPickerNavHost
import browserpicker.presentation.util.BrowserDefault

@OptIn(ExperimentalLayoutApi::class)
fun Modifier.bottomSheetContentModifier(): Modifier {
    return this
        .fillMaxWidth()
        .fillMaxHeight(0.75f)
        .navigationBarsPadding()
        .imePadding()
//        .imeNestedScroll()
        .padding(horizontal = 16.dp)
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNewIntent: (String) -> Unit = {}
) {
    val navController = rememberNavController()
    val snackBarHostState = remember { SnackbarHostState() }
    val sheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.Expanded), snackbarHostState = snackBarHostState
    )
    HandleBackPress(navController)

    BottomSheetScaffold(
        scaffoldState = sheetState,
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        sheetPeekHeight = 0.dp,
        sheetSwipeEnabled = true,
        sheetContent = {
//            Column(Modifier.height(500.dp)) {  }
            AppPickerSheetContent(
                modifier = Modifier.bottomSheetContentModifier(),
                uri = BrowserDefault.URL.toUri(),
            )
        },
        sheetTonalElevation = BottomSheetDefaults.Elevation,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContentColor = MaterialTheme.colorScheme.onSurface,
        sheetShadowElevation = 8.dp,
        sheetDragHandle = null,
    ) { paddingValues: PaddingValues ->
        Scaffold(
            bottomBar = {
                AppBottomNavigation(navController = navController)
            }
        ) { innerPadding ->
            BrowserPickerNavHost(navController = navController, modifier = Modifier.padding(innerPadding))
        }
    }
}
