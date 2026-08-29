package com.example.ul_fitness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

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

data class ActiveSet(
    val reps: String = "10",
    val weightKg: String = "0",
    val rpe: String = "",
    val isWarmup: Boolean = false,
    val isFailure: Boolean = false
)

data class ActiveExercise(
    val id: Long,
    val name: String,
    val category: String,
    val iconKey: String,
    val sets: List<ActiveSet> = emptyList()
)

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
        MainScreen(api = api, onLogout = { loggedIn = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(api: ApiClient, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf("home") }
    var selectedGym by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var activeExercises by remember { mutableStateOf(listOf<ActiveExercise>()) }
    var showSummary by remember { mutableStateOf(false) }
    var lastWorkoutSummary by remember { mutableStateOf<Pair<String, Int>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            screen == "workout" && activeExercises.isNotEmpty() ->
                                "${activeExercises.size} Übung${if (activeExercises.size > 1) "en" else ""}"
                            else -> "UL Fitness"
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                navigationIcon = {
                    if (screen != "home") {
                        IconButton(onClick = {
                            if (screen == "workout" && activeExercises.isNotEmpty()) {
                                // Don't lose data accidentally - handled by dialog in workout
                            } else {
                                screen = "home"
                                selectedGym = null
                                activeExercises = emptyList()
                            }
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
                    activeExercises = emptyList()
                    screen = "workout"
                }
            )
            "workout" -> ActiveWorkoutScreen(
                modifier = Modifier.padding(padding),
                api = api,
                gymId = selectedGym?.first ?: 1,
                gymName = selectedGym?.second ?: "",
                exercises = activeExercises,
                onExercisesChange = { activeExercises = it },
                onFinish = { gymName, count ->
                    screen = "home"
                    selectedGym = null
                    activeExercises = emptyList()
                    lastWorkoutSummary = gymName to count
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
            Text("Training starten", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            gyms.forEach { gym ->
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onStartWorkout(gym.id ?: 1, gym.name) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(gym.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            gym.city?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        Icon(Icons.Default.PlayArrow, contentDescription = "Starten", tint = MaterialTheme.colorScheme.primary)
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
fun ActiveWorkoutScreen(
    modifier: Modifier = Modifier,
    api: ApiClient,
    gymId: Long,
    gymName: String,
    exercises: List<ActiveExercise>,
    onExercisesChange: (List<ActiveExercise>) -> Unit,
    onFinish: (String, Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    var availableExercises by remember { mutableStateOf<List<ExerciseDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var showExercisePicker by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showRestTimer by remember { mutableStateOf(false) }
    var restSeconds by remember { mutableIntStateOf(90) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(gymId) {
        availableExercises = api.getExercises(gymId)
        isLoading = false
    }

    fun removeExercise(index: Int) {
        onExercisesChange(exercises.toMutableList().apply { removeAt(index) })
    }

    fun addSet(exerciseIndex: Int) {
        val ex = exercises[exerciseIndex]
        val lastSet = ex.sets.lastOrNull()
        val newSet = ActiveSet(
            reps = lastSet?.reps ?: "10",
            weightKg = lastSet?.weightKg ?: "0",
            rpe = lastSet?.rpe ?: ""
        )
        val updated = ex.copy(sets = ex.sets + newSet)
        onExercisesChange(exercises.toMutableList().apply { set(exerciseIndex, updated) })
    }

    fun removeSet(exerciseIndex: Int, setIndex: Int) {
        val ex = exercises[exerciseIndex]
        val newSets = ex.sets.toMutableList().apply { removeAt(setIndex) }
        val list = exercises.toMutableList()
        if (newSets.isEmpty()) {
            list.removeAt(exerciseIndex)
        } else {
            list[exerciseIndex] = ex.copy(sets = newSets)
        }
        onExercisesChange(list)
    }

    fun updateSet(exerciseIndex: Int, setIndex: Int, transform: ActiveSet.() -> ActiveSet) {
        val ex = exercises[exerciseIndex]
        val newSets = ex.sets.toMutableList()
        newSets[setIndex] = newSets[setIndex].transform()
        val updated = ex.copy(sets = newSets)
        onExercisesChange(exercises.toMutableList().apply { set(exerciseIndex, updated) })
    }

    // Rest Timer Overlay
    if (showRestTimer) {
        RestTimerOverlay(
            initialSeconds = restSeconds,
            onDismiss = { showRestTimer = false },
            onDone = { showRestTimer = false }
        )
    }

    // Exercise Picker Dialog
    if (showExercisePicker) {
        ExercisePickerDialog(
            exercises = availableExercises,
            onDismiss = { showExercisePicker = false },
            onSelect = { ex ->
                val newExercise = ActiveExercise(
                    id = ex.id ?: 0,
                    name = ex.name,
                    category = ex.category,
                    iconKey = ex.iconKey,
                    sets = listOf(ActiveSet(reps = "10", weightKg = "0"))
                )
                onExercisesChange(exercises + newExercise)
                showExercisePicker = false
            }
        )
    }

    // Finish Confirmation
    if (showFinishDialog) {
        FinishDialog(
            exerciseCount = exercises.size,
            totalSets = exercises.sumOf { it.sets.size },
            notes = notes,
            onNotesChange = { notes = it },
            onConfirm = {
                showFinishDialog = false
                isSaving = true
                scope.launch {
                    val req = CreateWorkoutRequest(
                        gymId = gymId,
                        notes = notes.ifBlank { null },
                        exercises = exercises.map { ex ->
                            WorkoutExerciseInput(
                                exerciseId = ex.id,
                                sets = ex.sets.map { s ->
                                    SetInput(
                                        reps = s.reps.toIntOrNull() ?: 0,
                                        weightKg = s.weightKg.toDoubleOrNull() ?: 0.0,
                                        isWarmup = s.isWarmup,
                                        rpe = s.rpe.toIntOrNull(),
                                        isFailure = s.isFailure
                                    )
                                }
                            )
                        }
                    )
                    val id = api.createWorkout(req)
                    if (id != null) {
                        api.finishWorkout(id)
                        onFinish(gymName, exercises.size)
                    } else {
                        errorMsg = "Speichern fehlgeschlagen"
                        isSaving = false
                    }
                }
            },
            onDismiss = { showFinishDialog = false }
        )
    }

    if (errorMsg != null) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { errorMsg = null }) { Text("OK") }
            }
        ) { Text(errorMsg!!) }
    }

    if (isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Exercise list
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Text(gymName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (exercises.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Übung hinzufügen um zu starten", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            itemsIndexed(exercises) { exIdx, exercise ->
                ExerciseCard(
                    exercise = exercise,
                    onAddSet = { addSet(exIdx) },
                    onRemoveSet = { setIdx -> removeSet(exIdx, setIdx) },
                    onUpdateSet = { setIdx, transform -> updateSet(exIdx, setIdx, transform) },
                    onRemove = { removeExercise(exIdx) },
                    onStartRest = { showRestTimer = true }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // Bottom bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showExercisePicker = true },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Übung")
                }
                Button(
                    onClick = {
                        if (exercises.isEmpty()) {
                            showCancelDialog = true
                        } else {
                            showFinishDialog = true
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (exercises.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    ),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (exercises.isEmpty()) "Abbrechen" else "Fertig")
                    }
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Training abbrechen?") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    onFinish(gymName, 0)
                }) { Text("Ja, abbrechen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Weiter") }
            }
        )
    }
}

@Composable
fun ExerciseCard(
    exercise: ActiveExercise,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onUpdateSet: (Int, ActiveSet.() -> ActiveSet) -> Unit,
    onRemove: () -> Unit,
    onStartRest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Exercise header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(exercise.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Entfernen", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Set headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Sat.", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Wdh.", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Text("kg", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Text("RPE", modifier = Modifier.weight(0.7f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.width(32.dp))
            }

            // Sets
            exercise.sets.forEachIndexed { setIdx, set ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "${setIdx + 1}",
                        modifier = Modifier.width(32.dp).clip(RoundedCornerShape(4.dp)).background(
                            if (set.isWarmup) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent
                        ).padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = set.reps,
                        onValueChange = { onUpdateSet(setIdx) { copy(reps = it) } },
                        modifier = Modifier.weight(1f).height(48.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 14.sp)
                    )

                    OutlinedTextField(
                        value = set.weightKg,
                        onValueChange = { onUpdateSet(setIdx) { copy(weightKg = it) } },
                        modifier = Modifier.weight(1f).height(48.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 14.sp)
                    )

                    OutlinedTextField(
                        value = set.rpe,
                        onValueChange = { onUpdateSet(setIdx) { copy(rpe = it) } },
                        modifier = Modifier.weight(0.7f).height(48.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 14.sp),
                        placeholder = { Text("-", textAlign = TextAlign.Center, fontSize = 14.sp) }
                    )

                    IconButton(onClick = { onRemoveSet(setIdx) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Satz entfernen", modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Warmup / Failure toggles + Add set
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Warmup toggle for last set
                if (exercise.sets.isNotEmpty()) {
                    FilterChip(
                        selected = exercise.sets.last().isWarmup,
                        onClick = {
                            onUpdateSet(exercise.sets.lastIndex) { copy(isWarmup = !isWarmup) }
                        },
                        label = { Text("Warmup", fontSize = 11.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                    FilterChip(
                        selected = exercise.sets.last().isFailure,
                        onClick = {
                            onUpdateSet(exercise.sets.lastIndex) { copy(isFailure = !isFailure) }
                        },
                        label = { Text("Muskelversagen", fontSize = 11.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(onClick = onAddSet, modifier = Modifier.height(28.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text("Satz", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun ExercisePickerDialog(
    exercises: List<ExerciseDto>,
    onDismiss: () -> Unit,
    onSelect: (ExerciseDto) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = exercises.filter {
        search.isBlank() || it.name.contains(search, ignoreCase = true) ||
                it.aliases.any { a -> a.contains(search, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Übung wählen") },
        text = {
            Column {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Suchen...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(filtered) { ex ->
                        ListItem(
                            headlineContent = { Text(ex.name) },
                            supportingContent = { Text(ex.category) },
                            leadingContent = {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(ex.iconKey.take(2).uppercase(), style = MaterialTheme.typography.labelMedium)
                                }
                            },
                            modifier = Modifier.clickable { onSelect(ex) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

@Composable
fun FinishDialog(
    exerciseCount: Int,
    totalSets: Int,
    notes: String,
    onNotesChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Training beenden?") },
        text = {
            Column {
                Text("$exerciseCount Übungen, $totalSets Sätze")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text("Notizen (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Speichern & Beenden") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Weiter trainieren") }
        }
    )
}

@Composable
fun RestTimerOverlay(
    initialSeconds: Int,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    var remaining by remember { mutableIntStateOf(initialSeconds) }

    LaunchedEffect(initialSeconds) {
        while (remaining > 0) {
            delay(1000L)
            remaining--
        }
        onDone()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pause", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                val minutes = remaining / 60
                val seconds = remaining % 60
                Text(
                    "%d:%02d".format(minutes, seconds),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 60, 90, 120).forEach { secs ->
                        AssistChip(
                            onClick = { remaining = secs },
                            label = { Text("${secs}s") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDone) { Text("Weiter") }
        }
    )
}
