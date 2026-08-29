package com.example.ul_fitness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.launch

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

@Composable
fun TrainingApp() {
    val context = LocalContext.current
    val api = remember { ApiClient(context) }
    var loggedIn by remember { mutableStateOf(false) }
    var checkingAuth by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loggedIn = api.isLoggedIn()
        checkingAuth = false
    }

    if (checkingAuth) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (!loggedIn) {
        LoginScreen(api = api, onLoginSuccess = { loggedIn = true })
    } else {
        MainScreen(api = api, onLogout = {
            loggedIn = false
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(api: ApiClient, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf("home") }
    var selectedGym by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var isStartingWorkout by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("UL Fitness") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                navigationIcon = {
                    if (screen != "home") {
                        IconButton(onClick = {
                            screen = "home"
                            selectedGym = null
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                        }
                    }
                },
                actions = {
                    if (screen == "home") {
                        TextButton(onClick = {
                            scope.launch {
                                api.logout()
                                onLogout()
                            }
                        }) {
                            Text("Abmelden", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (screen) {
            "home" -> HomeScreen(
                modifier = Modifier.padding(padding),
                api = api,
                onStartWorkout = { gymId, gymName ->
                    selectedGym = gymId to gymName
                    isStartingWorkout = true
                    screen = "workout"
                }
            )
            "workout" -> WorkoutScreen(
                modifier = Modifier.padding(padding),
                api = api,
                gymId = selectedGym?.first ?: 1,
                gymName = selectedGym?.second ?: "",
                onFinish = {
                    screen = "home"
                    selectedGym = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    api: ApiClient,
    onStartWorkout: (Long, String) -> Unit
) {
    var gyms by remember { mutableStateOf<List<GymDto>>(emptyList()) }
    var recentWorkouts by remember { mutableStateOf<List<WorkoutSummaryDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var gymExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        gyms = api.getGyms()
        if (gyms.isNotEmpty()) {
            recentWorkouts = api.getWorkouts(gyms.first().id ?: 1)
        }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Willkommen!", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Text("Studio wählen", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))

            ExposedDropdownMenuBox(
                expanded = gymExpanded,
                onExpandedChange = { gymExpanded = !gymExpanded }
            ) {
                OutlinedTextField(
                    value = gyms.find { it.id == (recentWorkouts.firstOrNull()?.gymId) }?.name ?: gyms.firstOrNull()?.name ?: "",
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
                    gyms.forEach { gym ->
                        DropdownMenuItem(
                            text = { Text(gym.name) },
                            onClick = {
                                gymExpanded = false
                                onStartWorkout(gym.id ?: 1, gym.name)
                            }
                        )
                    }
                }
            }
        }

        if (recentWorkouts.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Letzte Trainings", style = MaterialTheme.typography.titleMedium)
            }
            items(recentWorkouts) { workout ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                workout.gymName ?: "Unbekannt",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (workout.endedAt != null) {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Check, contentDescription = "Beendet", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(workout.startedAt.take(16), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!workout.notes.isNullOrBlank()) {
                            Text(workout.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    modifier: Modifier = Modifier,
    api: ApiClient,
    gymId: Long,
    gymName: String,
    onFinish: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var exercises by remember { mutableStateOf<List<ExerciseDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var selectedExercise by remember { mutableStateOf<ExerciseDto?>(null) }
    var exerciseExpanded by remember { mutableStateOf(false) }
    var reps by remember { mutableStateOf("10") }
    var weight by remember { mutableStateOf("0") }
    var sets by remember { mutableStateOf("3") }
    var rpe by remember { mutableStateOf("") }
    var savedWorkoutId by remember { mutableStateOf<Long?>(null) }
    var finished by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(gymId) {
        exercises = api.getExercises(gymId)
        isLoading = false
    }

    if (isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (finished) {
        Column(
            modifier = modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Training beendet!", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onFinish) { Text("Zurück") }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Training: $gymName", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Text("Übung wählen", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))

            ExposedDropdownMenuBox(
                expanded = exerciseExpanded,
                onExpandedChange = { exerciseExpanded = !exerciseExpanded }
            ) {
                OutlinedTextField(
                    value = selectedExercise?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Übung") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exerciseExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = exerciseExpanded,
                    onDismissRequest = { exerciseExpanded = false }
                ) {
                    exercises.forEach { ex ->
                        DropdownMenuItem(
                            text = { Text(ex.name) },
                            onClick = {
                                selectedExercise = ex
                                exerciseExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (selectedExercise != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(selectedExercise!!.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(selectedExercise!!.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = sets,
                        onValueChange = { sets = it },
                        label = { Text("Sätze") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it },
                        label = { Text("Wdh.") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Gewicht (kg)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = rpe,
                        onValueChange = { rpe = it },
                        label = { Text("RPE") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))

                AnimatedVisibility(visible = errorMsg != null) {
                    Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                errorMsg = null
                                if (savedWorkoutId == null) {
                                    val id = api.createWorkout(CreateWorkoutRequest(gymId = gymId))
                                    if (id == null) {
                                        errorMsg = "Workout erstellen fehlgeschlagen"
                                        isSaving = false
                                        return@launch
                                    }
                                    savedWorkoutId = id
                                }
                                isSaving = false
                                errorMsg = null
                                selectedExercise = null
                                sets = "3"
                                reps = "10"
                                weight = "0"
                                rpe = ""
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        enabled = !isSaving
                    ) {
                        if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Satz hinzufügen")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        if (savedWorkoutId != null) {
                            api.finishWorkout(savedWorkoutId!!)
                        }
                        finished = true
                        isSaving = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                enabled = !isSaving
            ) {
                Text("Training beenden")
            }
        }
    }
}
