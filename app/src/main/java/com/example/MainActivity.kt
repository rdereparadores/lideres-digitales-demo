package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.pregon.ui.MainViewModel
import com.example.pregon.ui.components.SourcesDialog
import com.example.pregon.ui.components.TraceDialog
import com.example.pregon.ui.screens.ChatScreen
import com.example.pregon.ui.screens.NewQueryDialog
import com.example.pregon.ui.screens.QueriesScreen
import com.example.pregon.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val activeTrace by viewModel.activeTrace.collectAsState()
    val activeSources by viewModel.activeSources.collectAsState()
    val activePersistOffer by viewModel.activePersistOffer.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.AutoMirrored.Filled.Chat else Icons.AutoMirrored.Outlined.Chat,
                            contentDescription = stringResource(R.string.tab_preguntar)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_preguntar)) },
                    modifier = Modifier.testTag("tab_preguntar")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.Schedule else Icons.Outlined.Schedule,
                            contentDescription = stringResource(R.string.tab_consultas)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_consultas)) },
                    modifier = Modifier.testTag("tab_consultas")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.tab_ajustes)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_ajustes)) },
                    modifier = Modifier.testTag("tab_ajustes")
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> ChatScreen(viewModel = viewModel)
                1 -> QueriesScreen(viewModel = viewModel)
                2 -> SettingsScreen(viewModel = viewModel)
            }
        }
    }

    // Modal Dialogs
    activeTrace?.let { trace ->
        TraceDialog(
            trace = trace,
            onDismiss = { viewModel.closeTraceDialog() }
        )
    }

    activeSources?.let { sources ->
        SourcesDialog(
            sources = sources,
            onDismiss = { viewModel.closeSourcesDialog() }
        )
    }

    activePersistOffer?.let { offer ->
        NewQueryDialog(
            offer = offer,
            onDismiss = { viewModel.closeNewQueryDialog() },
            onSave = { title, schedule ->
                viewModel.saveQuery(title, schedule, offer)
            }
        )
    }
}
