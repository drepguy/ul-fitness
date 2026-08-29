package com.example.ul_fitness

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

val CATEGORY_LIST = listOf(
    "Brust", "Rücken", "Beine", "Schulter", "Arme", "Core", "Ganzkörper", "Cardio"
)

val KIND_LIST = listOf(
    "machine" to "Machine",
    "free_weight" to "Freigewicht",
    "cable" to "Kabelzug",
    "bodyweight" to "Körpergewicht"
)

val ICON_LIST = listOf(
    "dumbbell" to "Hantel",
    "leg_press" to "Beine",
    "barbell" to "Langhantel",
    "cable" to "Kabelzug",
    "bench" to "Bank",
    "pull_up" to "Klimmzug",
    "treadmill" to "Laufband",
    "bike" to "Fahrrad"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(
    modifier: Modifier = Modifier,
    api: ApiClient,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var gyms by remember { mutableStateOf<List<GymDto>>(emptyList()) }
    var selectedGym by remember { mutableStateOf<GymDto?>(null) }
    var exercises by remember { mutableStateOf<List<ExerciseDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var search by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<ExerciseDto?>(null) }
    var showDeleteDialog by remember { mutableStateOf<ExerciseDto?>(null) }
    var gymExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        gyms = api.getGyms()
        if (gyms.isNotEmpty()) {
            selectedGym = gyms.first()
            exercises = api.getExercises(gyms.first().id ?: 1)
        }
        isLoading = false
    }

    fun loadExercises(gymId: Long) {
        scope.launch {
            isLoading = true
            exercises = api.getExercises(gymId)
            isLoading = false
        }
    }

    val filtered = exercises.filter {
        search.isBlank() || it.name.contains(search, ignoreCase = true) ||
                it.aliases.any { a -> a.contains(search, ignoreCase = true) }
    }

    if (showEditDialog) {
        ExerciseEditDialog(
            api = api,
            gymId = selectedGym?.id ?: 1,
            exercise = editingExercise,
            onDismiss = { showEditDialog = false; editingExercise = null },
            onSaved = {
                showEditDialog = false
                editingExercise = null
                selectedGym?.let { loadExercises(it.id ?: 1) }
            }
        )
    }

    showDeleteDialog?.let { ex ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Übung löschen?") },
            text = { Text("\"${ex.name}\" wirklich löschen?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        api.deleteExercise(ex.id ?: 0)
                        showDeleteDialog = null
                        selectedGym?.let { loadExercises(it.id ?: 1) }
                    }
                }) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("Abbrechen") } }
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Übungen verwalten", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))

        // Gym picker
        ExposedDropdownMenuBox(expanded = gymExpanded, onExpandedChange = { gymExpanded = !gymExpanded }) {
            OutlinedTextField(
                value = selectedGym?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Studio") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gymExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = gymExpanded, onDismissRequest = { gymExpanded = false }) {
                gyms.forEach { gym ->
                    DropdownMenuItem(
                        text = { Text(gym.name) },
                        onClick = {
                            selectedGym = gym
                            gymExpanded = false
                            loadExercises(gym.id ?: 1)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Suchen...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    Text("${filtered.size} Übungen", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(filtered) { ex ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(ex.iconKey.take(2).uppercase(), style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ex.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(ex.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(KIND_LIST.find { it.first == ex.kind }?.second ?: ex.kind, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                if (ex.aliases.isNotEmpty()) {
                                    Text("Aliases: ${ex.aliases.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { editingExercise = ex; showEditDialog = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Bearbeiten", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { showDeleteDialog = ex }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Löschen", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { editingExercise = null; showEditDialog = true },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Neue Übung")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseEditDialog(
    api: ApiClient,
    gymId: Long,
    exercise: ExerciseDto?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(exercise?.name ?: "") }
    var category by remember { mutableStateOf(exercise?.category ?: CATEGORY_LIST.first()) }
    var kind by remember { mutableStateOf(exercise?.kind ?: "machine") }
    var iconKey by remember { mutableStateOf(exercise?.iconKey ?: "dumbbell") }
    var aliasesText by remember { mutableStateOf(exercise?.aliases?.joinToString(", ") ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var catExpanded by remember { mutableStateOf(false) }
    var kindExpanded by remember { mutableStateOf(false) }
    var iconExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(if (exercise != null) "Übung bearbeiten" else "Neue Übung") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSaving
                )

                // Category dropdown
                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategorie") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        enabled = !isSaving
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        CATEGORY_LIST.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; catExpanded = false })
                        }
                    }
                }

                // Kind dropdown
                ExposedDropdownMenuBox(expanded = kindExpanded, onExpandedChange = { kindExpanded = !kindExpanded }) {
                    OutlinedTextField(
                        value = KIND_LIST.find { it.first == kind }?.second ?: kind,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Art") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = kindExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        enabled = !isSaving
                    )
                    ExposedDropdownMenu(expanded = kindExpanded, onDismissRequest = { kindExpanded = false }) {
                        KIND_LIST.forEach { (k, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { kind = k; kindExpanded = false })
                        }
                    }
                }

                // Icon dropdown
                ExposedDropdownMenuBox(expanded = iconExpanded, onExpandedChange = { iconExpanded = !iconExpanded }) {
                    OutlinedTextField(
                        value = ICON_LIST.find { it.first == iconKey }?.second ?: iconKey,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Icon") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = iconExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        enabled = !isSaving
                    )
                    ExposedDropdownMenu(expanded = iconExpanded, onDismissRequest = { iconExpanded = false }) {
                        ICON_LIST.forEach { (k, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { iconKey = k; iconExpanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = aliasesText,
                    onValueChange = { aliasesText = it },
                    label = { Text("Aliases (kommasepariert)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )

                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        errorMsg = "Name ist erforderlich"
                        return@TextButton
                    }
                    isSaving = true
                    scope.launch {
                        val aliases = aliasesText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        val req = CreateExerciseRequest(
                            name = name,
                            category = category,
                            kind = kind,
                            iconKey = iconKey,
                            gymId = gymId,
                            aliases = aliases
                        )
                        val ok = if (exercise != null) {
                            val saved = api.updateExercise(exercise.id ?: 0, req)
                            if (saved) {
                                val old = api.getAliases(exercise.id ?: 0)
                                val toAdd = aliases.filter { it !in old }
                                val toRemove = old.filter { it !in aliases }
                                if (toAdd.isNotEmpty() || toRemove.isNotEmpty()) api.updateAliases(exercise.id ?: 0, toAdd, toRemove) else true
                            } else false
                        } else {
                            api.createExercise(req) != null
                        }
                        if (ok) onSaved()
                        else {
                            errorMsg = "Speichern fehlgeschlagen"
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving && name.isNotBlank()
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Abbrechen") }
        }
    )
}
