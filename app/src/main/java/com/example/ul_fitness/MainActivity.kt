package com.example.ul_fitness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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

fun formatLocalDateTime(isoString: String): String {
    return try {
        val cleaned = isoString.replace(",", ".").take(26)
        val parsed = LocalDateTime.parse(cleaned)
        val utc = parsed.atZone(ZoneId.of("UTC"))
        val local = utc.withZoneSameInstant(ZoneId.systemDefault())
        local.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.GERMAN))
    } catch (e: Exception) {
        android.util.Log.e("FormatDate", "Failed to parse: '$isoString'", e)
        isoString.take(16)
    }
}

data class ActiveSet(
    val reps: String = "10",
    val weightKg: String = "0",
    val rpe: String = ""
)

data class ActiveExercise(
    val id: Long,
    val name: String,
    val category: String,
    val iconKey: String,
    val sets: List<ActiveSet> = emptyList(),
    val lastSets: List<ApiClient.LastSetDto> = emptyList()
)

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    data object Training : BottomNavItem("home", "Training", Icons.Default.Home)
    data object Uebungen : BottomNavItem("exercises", "Übungen", Icons.AutoMirrored.Filled.List)
    data object Analyse : BottomNavItem("analysis", "Analyse", Icons.Default.DateRange)
}

@Composable
fun SplashScreen() {
    val alpha = rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOut), RepeatMode.Reverse), label = "alpha"
    )
    val scale = rememberInfiniteTransition(label = "scale").animateFloat(
        initialValue = 0.85f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse), label = "scale"
    )
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.ic_ul_logo),
                contentDescription = "UL Fitness",
                modifier = Modifier.size(140.dp).graphicsLayer { this.alpha = alpha.value; scaleX = scale.value; scaleY = scale.value },
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("UL Fitness", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        if (!api.isLoggedIn()) api.login("ulrich@ulf.local", "UlFitness2026!")
        loggedIn = true
        checkingAuth = false
    }

    if (checkingAuth) {
        SplashScreen()
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
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val bottomItems = listOf(BottomNavItem.Training, BottomNavItem.Uebungen, BottomNavItem.Analyse)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTopLevel = currentRoute in listOf("home", "exercises", "analysis")

    Scaffold(
        topBar = {
            if (isTopLevel) {
                TopAppBar(
                    title = {
                        Text(when (currentRoute) {
                            "exercises" -> "Übungen"
                            "analysis" -> "Analyse"
                            else -> "UL Fitness"
                        })
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    actions = {
                        if (currentRoute == "home") {
                            TextButton(onClick = {
                                scope.launch { api.logout(); onLogout() }
                            }) {
                                Text("Abmelden", color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(
                    api = api,
                    onStartWorkout = { gymId, gymName ->
                        navController.navigate("workout/$gymId/$gymName")
                    },
                    onOpenWorkout = { workoutId ->
                        navController.navigate("detail/$workoutId")
                    }
                )
            }

            composable("exercises") {
                ExerciseListScreen(
                    api = api,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("analysis") {
                AnalyseScreen(api = api)
            }

            composable(
                "workout/{gymId}/{gymName}",
                arguments = listOf(navArgument("gymId") { type = NavType.LongType }, navArgument("gymName") { type = NavType.StringType })
            ) { backStackEntry ->
                val gymId = backStackEntry.arguments?.getLong("gymId") ?: 1
                val gymName = backStackEntry.arguments?.getString("gymName") ?: ""
                ActiveWorkoutContainer(
                    api = api,
                    gymId = gymId,
                    gymName = gymName,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                "detail/{workoutId}",
                arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
            ) { backStackEntry ->
                val workoutId = backStackEntry.arguments?.getLong("workoutId") ?: 0
                WorkoutDetailScreen(
                    api = api,
                    workoutId = workoutId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun ActiveWorkoutContainer(api: ApiClient, gymId: Long, gymName: String, onBack: () -> Unit) {
    var activeExercises by remember { mutableStateOf(listOf<ActiveExercise>()) }

    ActiveWorkoutScreen(
        api = api,
        gymId = gymId,
        gymName = gymName,
        exercises = activeExercises,
        onExercisesChange = { activeExercises = it },
        onFinish = { _, _ ->
            activeExercises = emptyList()
            onBack()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    api: ApiClient,
    onStartWorkout: (Long, String) -> Unit,
    onOpenWorkout: (Long) -> Unit
) {
    var gyms by remember { mutableStateOf<List<GymDto>>(emptyList()) }
    var recentWorkouts by remember { mutableStateOf<List<WorkoutSummaryDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        var g = api.getGyms()
        if (g.isEmpty()) { kotlinx.coroutines.delay(1000); g = api.getGyms() }
        gyms = g
        val all = mutableListOf<WorkoutSummaryDto>()
        for (gm in gyms) { all += api.getWorkouts(gm.id ?: continue) }
        recentWorkouts = all.sortedByDescending { it.startedAt }.take(20)
        isLoading = false
    }

    if (isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Willkommen!", style = MaterialTheme.typography.headlineMedium); Spacer(modifier = Modifier.height(8.dp)) }
        item {
            Text("Training starten", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            gyms.forEach { gym ->
                OutlinedCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onStartWorkout(gym.id ?: 1, gym.name) }) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
            item { Spacer(modifier = Modifier.height(8.dp)); Text("Letzte Trainings", style = MaterialTheme.typography.titleMedium) }
            items(recentWorkouts) { workout ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onOpenWorkout(workout.id) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(workout.gymName ?: "Unbekannt", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (workout.endedAt != null) { Spacer(modifier = Modifier.weight(1f)); Icon(Icons.Default.Check, contentDescription = "Beendet", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
                        }
                        Text(formatLocalDateTime(workout.startedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!workout.notes.isNullOrBlank()) { Text(workout.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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

    LaunchedEffect(gymId) { availableExercises = api.getExercises(gymId); isLoading = false }

    fun removeExercise(index: Int) { onExercisesChange(exercises.toMutableList().apply { removeAt(index) }) }
    fun addSet(exerciseIndex: Int) {
        val ex = exercises[exerciseIndex]
        val nextIdx = ex.sets.size
        val historySet = ex.lastSets.getOrNull(nextIdx)
        val lastSet = ex.sets.lastOrNull()
        val newSet = if (historySet != null) {
            ActiveSet(reps = historySet.reps.toString(), weightKg = if (historySet.weightKg == historySet.weightKg.toLong().toDouble()) historySet.weightKg.toLong().toString() else historySet.weightKg.toString(), rpe = historySet.rpe?.toString() ?: "")
        } else {
            ActiveSet(reps = lastSet?.reps ?: "10", weightKg = lastSet?.weightKg ?: "0", rpe = lastSet?.rpe ?: "")
        }
        onExercisesChange(exercises.toMutableList().apply { set(exerciseIndex, ex.copy(sets = ex.sets + newSet)) })
    }
    fun removeSet(exerciseIndex: Int, setIndex: Int) {
        val ex = exercises[exerciseIndex]
        val newSets = ex.sets.toMutableList().apply { removeAt(setIndex) }
        val list = exercises.toMutableList()
        if (newSets.isEmpty()) list.removeAt(exerciseIndex) else list[exerciseIndex] = ex.copy(sets = newSets)
        onExercisesChange(list)
    }
    fun updateSet(exerciseIndex: Int, setIndex: Int, transform: ActiveSet.() -> ActiveSet) {
        val ex = exercises[exerciseIndex]
        val newSets = ex.sets.toMutableList().apply { set(setIndex, get(setIndex).transform()) }
        onExercisesChange(exercises.toMutableList().apply { set(exerciseIndex, ex.copy(sets = newSets)) })
    }

    if (showRestTimer) RestTimerOverlay(initialSeconds = restSeconds, onDismiss = { showRestTimer = false }, onDone = { showRestTimer = false })
    if (showExercisePicker) {
        ExercisePickerDialog(exercises = availableExercises, onDismiss = { showExercisePicker = false }, onSelect = { ex ->
            showExercisePicker = false
            scope.launch {
                val lastSets = api.getLastSets(ex.id ?: 0, gymId)
                val firstSet = lastSets.firstOrNull()?.let { ls ->
                    ActiveSet(
                        reps = ls.reps.toString(),
                        weightKg = if (ls.weightKg == ls.weightKg.toLong().toDouble()) ls.weightKg.toLong().toString() else ls.weightKg.toString(),
                        rpe = ls.rpe?.toString() ?: ""
                    )
                } ?: ActiveSet()
                onExercisesChange(exercises + ActiveExercise(id = ex.id ?: 0, name = ex.name, category = ex.category, iconKey = ex.iconKey, sets = listOf(firstSet), lastSets = lastSets))
            }
        })
    }
    if (showFinishDialog) {
        FinishDialog(exerciseCount = exercises.size, totalSets = exercises.sumOf { it.sets.size }, notes = notes, onNotesChange = { notes = it }, onConfirm = {
            showFinishDialog = false; isSaving = true; errorMsg = null
            scope.launch {
                val req = CreateWorkoutRequest(gymId = gymId, notes = notes.ifBlank { null }, exercises = exercises.map { ex ->
                    WorkoutExerciseInput(exerciseId = ex.id, sets = ex.sets.map { s -> SetInput(reps = s.reps.toIntOrNull() ?: 0, weightKg = s.weightKg.toDoubleOrNull() ?: 0.0, rpe = s.rpe.toIntOrNull()) })
                })
                val id = api.createWorkout(req)
                if (id != null) { api.finishWorkout(id); onFinish(gymName, exercises.size) }
                else { errorMsg = "Speichern fehlgeschlagen"; isSaving = false }
            }
        }, onDismiss = { showFinishDialog = false })
    }

    if (isLoading) { Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
            item { Text(gymName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (exercises.isEmpty()) { item { Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) { Text("Übung hinzufügen um zu starten", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
            itemsIndexed(exercises) { exIdx, exercise ->
                ExerciseCard(exercise = exercise, onAddSet = { addSet(exIdx) }, onRemoveSet = { setIdx -> removeSet(exIdx, setIdx) }, onUpdateSet = { setIdx, transform -> updateSet(exIdx, setIdx, transform) }, onRemove = { removeExercise(exIdx) })
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
        Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showExercisePicker = true }, modifier = Modifier.weight(1f).height(48.dp)) { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Übung") }
                Button(onClick = { if (exercises.isEmpty()) showCancelDialog = true else showFinishDialog = true }, modifier = Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = if (exercises.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error), enabled = !isSaving) {
                    if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) else Text(if (exercises.isEmpty()) "Abbrechen" else "Fertig")
                }
            }
        }
    }

    if (errorMsg != null) Snackbar(modifier = Modifier.padding(16.dp), action = { TextButton(onClick = { errorMsg = null }) { Text("OK") } }) { Text(errorMsg!!) }
    if (showCancelDialog) AlertDialog(onDismissRequest = { showCancelDialog = false }, title = { Text("Training abbrechen?") }, confirmButton = { TextButton(onClick = { showCancelDialog = false; onFinish(gymName, 0) }) { Text("Ja, abbrechen", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { showCancelDialog = false }) { Text("Weiter") } })
}

@Composable
fun ExerciseCard(exercise: ActiveExercise, onAddSet: () -> Unit, onRemoveSet: (Int) -> Unit, onUpdateSet: (Int, ActiveSet.() -> ActiveSet) -> Unit, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) { Text(exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(exercise.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, contentDescription = "Entfernen", modifier = Modifier.size(18.dp)) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Sat.", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Wdh.", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Text("kg", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Text("RPE", modifier = Modifier.weight(0.7f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.width(32.dp))
            }
            exercise.sets.forEachIndexed { setIdx, set ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${setIdx + 1}", modifier = Modifier.width(32.dp).padding(vertical = 8.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = set.reps, onValueChange = { onUpdateSet(setIdx) { copy(reps = it) } }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 14.sp))
                    OutlinedTextField(value = set.weightKg, onValueChange = { onUpdateSet(setIdx) { copy(weightKg = it) } }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 14.sp))
                    OutlinedTextField(value = set.rpe, onValueChange = { onUpdateSet(setIdx) { copy(rpe = it) } }, modifier = Modifier.weight(0.7f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 14.sp), placeholder = { Text("-", textAlign = TextAlign.Center, fontSize = 14.sp) })
                    IconButton(onClick = { onRemoveSet(setIdx) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, contentDescription = "Satz entfernen", modifier = Modifier.size(14.dp)) }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onAddSet, modifier = Modifier.height(28.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp)); Text("Satz", fontSize = 11.sp) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(modifier: Modifier = Modifier, api: ApiClient, workoutId: Long, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<WorkoutDetailDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editNotes by remember { mutableStateOf("") }
    var editExercises by remember { mutableStateOf<List<ActiveExercise>>(emptyList()) }
    var showExercisePicker by remember { mutableStateOf(false) }
    var availableExercises by remember { mutableStateOf<List<ExerciseDto>>(emptyList()) }

    fun loadDetail() {
        scope.launch {
            isLoading = true
            detail = api.getWorkoutDetail(workoutId)
            if (detail != null) editNotes = detail!!.notes ?: ""
            isLoading = false
        }
    }

    fun enterEdit() {
        val d = detail ?: return
        editExercises = d.exercises.map { ex ->
            ActiveExercise(
                id = ex.exerciseId,
                name = ex.name,
                category = "",
                iconKey = ex.iconKey,
                sets = ex.sets.map { s ->
                    ActiveSet(reps = s.reps.toString(), weightKg = s.weightKg.toLong().toString(), rpe = s.rpe?.toString() ?: "")
                }
            )
        }
        isEditing = true
    }

    fun removeExercise(index: Int) {
        editExercises = editExercises.toMutableList().apply { removeAt(index) }
    }

    fun addSet(exerciseIndex: Int) {
        val ex = editExercises[exerciseIndex]
        val nextIdx = ex.sets.size
        val historySet = ex.lastSets.getOrNull(nextIdx)
        val lastSet = ex.sets.lastOrNull()
        val newSet = if (historySet != null) {
            ActiveSet(reps = historySet.reps.toString(), weightKg = if (historySet.weightKg == historySet.weightKg.toLong().toDouble()) historySet.weightKg.toLong().toString() else historySet.weightKg.toString(), rpe = historySet.rpe?.toString() ?: "")
        } else {
            ActiveSet(reps = lastSet?.reps ?: "10", weightKg = lastSet?.weightKg ?: "0", rpe = lastSet?.rpe ?: "")
        }
        editExercises = editExercises.toMutableList().apply {
            set(exerciseIndex, ex.copy(sets = ex.sets + newSet))
        }
    }

    fun removeSet(exerciseIndex: Int, setIndex: Int) {
        val ex = editExercises[exerciseIndex]
        val newSets = ex.sets.toMutableList().apply { removeAt(setIndex) }
        val list = editExercises.toMutableList()
        if (newSets.isEmpty()) list.removeAt(exerciseIndex) else list[exerciseIndex] = ex.copy(sets = newSets)
        editExercises = list
    }

    fun updateSet(exerciseIndex: Int, setIndex: Int, transform: ActiveSet.() -> ActiveSet) {
        val ex = editExercises[exerciseIndex]
        val newSets = ex.sets.toMutableList().apply { set(setIndex, get(setIndex).transform()) }
        editExercises = editExercises.toMutableList().apply { set(exerciseIndex, ex.copy(sets = newSets)) }
    }

    LaunchedEffect(workoutId) { loadDetail() }

    if (showExercisePicker) {
        ExercisePickerDialog(exercises = availableExercises, onDismiss = { showExercisePicker = false }, onSelect = { ex ->
            showExercisePicker = false
            scope.launch {
                val lastSets = api.getLastSets(ex.id ?: 0, detail?.gymId)
                val firstSet = lastSets.firstOrNull()?.let { ls ->
                    ActiveSet(
                        reps = ls.reps.toString(),
                        weightKg = if (ls.weightKg == ls.weightKg.toLong().toDouble()) ls.weightKg.toLong().toString() else ls.weightKg.toString(),
                        rpe = ls.rpe?.toString() ?: ""
                    )
                } ?: ActiveSet()
                editExercises = editExercises + ActiveExercise(id = ex.id ?: 0, name = ex.name, category = ex.category, iconKey = ex.iconKey, sets = listOf(firstSet), lastSets = lastSets)
            }
        })
    }

    if (isLoading) { Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }

    val d = detail
    if (d == null) {
        Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Training nicht gefunden", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) { Text("Zurück") }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Training bearbeiten" else "Training ansehen") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück") } }
            )
        },
        bottomBar = {
            Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isEditing) {
                        OutlinedButton(onClick = { showExercisePicker = true }, modifier = Modifier.weight(1f).height(48.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Übung +")
                        }
                        Button(
                            onClick = {
                                isSaving = true
                                scope.launch {
                                    val exercises = editExercises.map { ex ->
                                        WorkoutExerciseInput(
                                            exerciseId = ex.id,
                                            sets = ex.sets.map { s ->
                                                SetInput(
                                                    reps = s.reps.toIntOrNull() ?: 0,
                                                    weightKg = s.weightKg.toDoubleOrNull() ?: 0.0,
                                                    rpe = s.rpe.toIntOrNull()
                                                )
                                            }
                                        )
                                    }
                                    val notesOk = api.updateWorkoutNotes(workoutId, editNotes)
                                    val exOk = api.replaceWorkoutExercises(workoutId, exercises)
                                    if (notesOk && exOk) {
                                        isEditing = false
                                        loadDetail()
                                    } else {
                                        errorMsg = "Speichern fehlgeschlagen"
                                    }
                                    isSaving = false
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            enabled = !isSaving
                        ) {
                            if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text("Speichern")
                        }
                    } else {
                        OutlinedButton(onClick = { showDeleteDialog = true }, modifier = Modifier.weight(1f).height(48.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Löschen", color = MaterialTheme.colorScheme.error)
                        }
                        Button(onClick = {
                            scope.launch {
                                availableExercises = api.getExercises(d.gymId ?: 1)
                                enterEdit()
                            }
                        }, modifier = Modifier.weight(1f).height(48.dp)) {
                            Text("Bearbeiten")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                item {
                    Text("Training", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatLocalDateTime(d.startedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (d.endedAt != null) Text("bis ${formatLocalDateTime(d.endedAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    if (isEditing) OutlinedTextField(value = editNotes, onValueChange = { editNotes = it }, label = { Text("Notizen") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                    else if (!d.notes.isNullOrBlank()) Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Text(d.notes, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium) }
                }
                if (isEditing) {
                    itemsIndexed(editExercises) { exIdx, exercise ->
                        ExerciseCard(
                            exercise = exercise,
                            onAddSet = { addSet(exIdx) },
                            onRemoveSet = { setIdx -> removeSet(exIdx, setIdx) },
                            onUpdateSet = { setIdx, transform -> updateSet(exIdx, setIdx, transform) },
                            onRemove = { removeExercise(exIdx) }
                        )
                    }
                    item {
                        OutlinedButton(
                            onClick = { showExercisePicker = true },
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Übung hinzufügen")
                        }
                    }
                } else {
                    items(d.exercises) { ex ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(ex.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("#", modifier = Modifier.width(24.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Wdh.", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                    Text("kg", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                    Text("RPE", modifier = Modifier.weight(0.7f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                }
                                ex.sets.forEachIndexed { idx, s ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                                        Text("${idx + 1}", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                                        Text("${s.reps}", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                                        Text("${s.weightKg}", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                                        Text("${s.rpe ?: "-"}", modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Training löschen?") },
                    confirmButton = {
                        TextButton(onClick = { showDeleteDialog = false; scope.launch { api.deleteWorkout(workoutId); onBack() } }) {
                            Text("Löschen", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) { Text("Abbrechen") }
                    }
                )
            }
            if (errorMsg != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = { TextButton(onClick = { errorMsg = null }) { Text("OK") } }
                ) { Text(errorMsg!!) }
            }
        }
    }
}

@Composable
fun ExercisePickerDialog(exercises: List<ExerciseDto>, onDismiss: () -> Unit, onSelect: (ExerciseDto) -> Unit) {
    var search by remember { mutableStateOf("") }
    val filtered = exercises.filter { search.isBlank() || it.name.contains(search, ignoreCase = true) || it.aliases.any { a -> a.contains(search, ignoreCase = true) } }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Übung wählen") }, text = {
        Column {
            OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Suchen...") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(filtered) { ex ->
                    ListItem(headlineContent = { Text(ex.name) }, supportingContent = { Text(ex.category) }, modifier = Modifier.clickable { onSelect(ex) })
                }
            }
        }
    }, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } })
}

@Composable
fun FinishDialog(exerciseCount: Int, totalSets: Int, notes: String, onNotesChange: (String) -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Training beenden?") }, text = {
        Column { Text("$exerciseCount Übungen, $totalSets Sätze"); Spacer(modifier = Modifier.height(12.dp)); OutlinedTextField(value = notes, onValueChange = onNotesChange, label = { Text("Notizen (optional)") }, modifier = Modifier.fillMaxWidth(), maxLines = 3) }
    }, confirmButton = { TextButton(onClick = onConfirm) { Text("Speichern & Beenden") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Weiter trainieren") } })
}

@Composable
fun RestTimerOverlay(initialSeconds: Int, onDismiss: () -> Unit, onDone: () -> Unit) {
    var remaining by remember { mutableIntStateOf(initialSeconds) }
    LaunchedEffect(initialSeconds) { while (remaining > 0) { delay(1000L); remaining-- }; onDone() }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Pause", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }, text = {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("%d:%02d".format(remaining / 60, remaining % 60), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(30, 60, 90, 120).forEach { secs -> AssistChip(onClick = { remaining = secs }, label = { Text("${secs}s") }) } }
        }
    }, confirmButton = { TextButton(onClick = onDone) { Text("Weiter") } })
}
