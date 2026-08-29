package com.example.ul_fitness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                TrainingApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingApp() {
    var screen by remember { mutableStateOf("home") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("UL Fitness") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        when (screen) {
            "home" -> HomeScreen(
                modifier = Modifier.padding(padding),
                onStartWorkout = { screen = "workout" }
            )
            "workout" -> WorkoutScreen(
                modifier = Modifier.padding(padding),
                onFinish = { screen = "home" }
            )
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier, onStartWorkout: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("UL Fitness", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onStartWorkout) {
            Text("Training starten")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(modifier: Modifier = Modifier, onFinish: () -> Unit) {
    var gymExpanded by remember { mutableStateOf(false) }
    val gyms = listOf("Thomas Sport Center" to 1, "All Inclusive Fitness" to 2)
    var selectedGym by remember { mutableStateOf<Pair<String, Int>?>(null) }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Neues Training", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = gymExpanded,
            onExpandedChange = { gymExpanded = !gymExpanded }
        ) {
            OutlinedTextField(
                value = selectedGym?.first ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Studio") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gymExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = gymExpanded,
                onDismissRequest = { gymExpanded = false }
            ) {
                gyms.forEach { (name, id) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            selectedGym = name to id
                            gymExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (selectedGym != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Gym: ${selectedGym!!.first}", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Übungen werden geladen...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onFinish,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Training beenden")
        }
    }
}
