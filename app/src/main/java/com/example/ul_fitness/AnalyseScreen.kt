package com.example.ul_fitness

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate

private val PERIOD_OPTIONS = listOf(
    "4W" to 28,
    "12W" to 84,
    "6M" to 180,
    "1J" to 365,
    "Alle" to 9999
)

private val METRIC_OPTIONS = listOf(
    "e1RM" to "e1rm",
    "Volumen" to "volume",
    "Max Gewicht" to "maxWeight"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyseScreen(
    modifier: Modifier = Modifier,
    api: ApiClient
) {
    val scope = rememberCoroutineScope()
    var gyms by remember { mutableStateOf<List<GymDto>>(emptyList()) }
    var selectedGym by remember { mutableStateOf<GymDto?>(null) }
    var gymExpanded by remember { mutableStateOf(false) }

    var exercises by remember { mutableStateOf<List<ExerciseDto>>(emptyList()) }
    var selectedExercise by remember { mutableStateOf<ExerciseDto?>(null) }
    var showExercisePicker by remember { mutableStateOf(false) }

    var selectedPeriod by remember { mutableIntStateOf(1) }
    var selectedMetric by remember { mutableIntStateOf(0) }

    var dailyData by remember { mutableStateOf<List<AggregatedDayDto>>(emptyList()) }
    var prs by remember { mutableStateOf(PrDto(0.0, null, 0.0, null, 0.0, null)) }
    var dashboard by remember { mutableStateOf(DashboardStatsDto(0, 0, 0.0, 0.0, 0, 0)) }
    var monthlyVolume by remember { mutableStateOf<List<MonthlyVolumeDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    fun loadExerciseData() {
        val ex = selectedExercise ?: return
        val gymId = selectedGym?.id
        val days = PERIOD_OPTIONS[selectedPeriod].second
        val from = LocalDate.now().minusDays(days.toLong()).toString()
        val to = LocalDate.now().toString()

        scope.launch {
            isLoading = true
            dailyData = api.getProgressDaily(ex.id ?: 0, gymId, from, to)
            prs = api.getPrs(ex.id ?: 0, gymId)
            isLoading = false
        }
    }

    fun loadDashboard() {
        val gymId = selectedGym?.id
        val days = PERIOD_OPTIONS[selectedPeriod].second
        val from = LocalDate.now().minusDays(days.toLong()).toString()
        val to = LocalDate.now().toString()

        scope.launch {
            isLoading = true
            dashboard = api.getDashboard(gymId, from, to)
            monthlyVolume = api.getMonthlyVolume(gymId, from, to)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        gyms = api.getGyms()
        if (gyms.isNotEmpty()) {
            selectedGym = gyms.first()
            exercises = api.getExercises(gyms.first().id ?: 1)
        }
        loadDashboard()
    }

    LaunchedEffect(selectedExercise, selectedPeriod, selectedMetric) {
        if (selectedExercise != null) loadExerciseData()
    }

    if (showExercisePicker) {
        ExercisePickerDialog(
            exercises = exercises,
            onDismiss = { showExercisePicker = false },
            onSelect = { ex ->
                selectedExercise = ex
                showExercisePicker = false
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Analyse", style = MaterialTheme.typography.headlineMedium)
        }

        item {
            ExposedDropdownMenuBox(
                expanded = gymExpanded,
                onExpandedChange = { gymExpanded = !gymExpanded }
            ) {
                OutlinedTextField(
                    value = selectedGym?.name ?: "",
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
                    DropdownMenuItem(
                        text = { Text("Alle Studios") },
                        onClick = {
                            selectedGym = null
                            gymExpanded = false
                            exercises = emptyList()
                            selectedExercise = null
                            loadDashboard()
                        }
                    )
                    gyms.forEach { gym ->
                        DropdownMenuItem(
                            text = { Text(gym.name) },
                            onClick = {
                                selectedGym = gym
                                gymExpanded = false
                                scope.launch {
                                    exercises = api.getExercises(gym.id ?: 1)
                                    selectedExercise = null
                                }
                                loadDashboard()
                            }
                        )
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PERIOD_OPTIONS.forEachIndexed { index, (label, _) ->
                    FilterChip(
                        selected = selectedPeriod == index,
                        onClick = {
                            selectedPeriod = index
                            loadDashboard()
                            if (selectedExercise != null) loadExerciseData()
                        },
                        label = { Text(label) }
                    )
                }
            }
        }

        item {
            Text("Übung wählen", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = { showExercisePicker = true },
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(selectedExercise?.name ?: "Übung auswählen...")
            }
        }

        if (selectedExercise != null) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    METRIC_OPTIONS.forEachIndexed { index, (label, _) ->
                        FilterChip(
                            selected = selectedMetric == index,
                            onClick = { selectedMetric = index },
                            label = { Text(label) }
                        )
                    }
                }
            }

            item {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (dailyData.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Keine Daten für diese Übung", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    val metricLabel = METRIC_OPTIONS[selectedMetric].first
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "$metricLabel — ${selectedExercise?.name}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val values = when (METRIC_OPTIONS[selectedMetric].second) {
                                "e1rm" -> dailyData.map { it.e1RM }
                                "volume" -> dailyData.map { it.volume }
                                "maxWeight" -> dailyData.map { it.maxWeight }
                                else -> dailyData.map { it.e1RM }
                            }
                            val chartDates = dailyData.map { it.date.take(5) }
                            SimpleLineChart(
                                values = values,
                                dates = chartDates,
                                modifier = Modifier.fillMaxWidth().height(200.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${dailyData.size} Trainingspunkte",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Text("Persönliche Rekorde", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrCard(
                        title = "Max Gewicht",
                        value = "%.1f kg".format(prs.maxWeight),
                        date = prs.maxWeightDate?.let { formatLocalDateTime(it) },
                        modifier = Modifier.weight(1f)
                    )
                    PrCard(
                        title = "Beste e1RM",
                        value = "%.1f kg".format(prs.maxE1RM),
                        date = prs.maxE1RMDate?.let { formatLocalDateTime(it) },
                        modifier = Modifier.weight(1f)
                    )
                    PrCard(
                        title = "Max Volumen",
                        value = "%.0f kg".format(prs.maxVolume),
                        date = prs.maxVolumeDate?.let { formatLocalDateTime(it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Dashboard", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Trainings", "${dashboard.totalWorkouts}", Modifier.weight(1f))
                StatCard("Sätze", "${dashboard.totalSets}", Modifier.weight(1f))
                StatCard("Pro Woche", "%.1f".format(dashboard.workoutsPerWeek), Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Volumen", "%.0f kg".format(dashboard.totalVolume), Modifier.weight(1f))
                StatCard("Übungen", "${dashboard.exercisesTrained}", Modifier.weight(1f))
                StatCard("Zeitraum", "${dashboard.periodDays} Tage", Modifier.weight(1f))
            }
        }

        if (monthlyVolume.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Monatliches Volumen", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        monthlyVolume.forEach { mv ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    mv.month,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.width(80.dp)
                                )
                                LinearProgressIndicator(
                                    progress = {
                                        val maxVol = monthlyVolume.maxOf { it.volume }
                                        if (maxVol > 0) (mv.volume / maxVol).toFloat() else 0f
                                    },
                                    modifier = Modifier.weight(1f).height(16.dp).padding(horizontal = 8.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Text(
                                    "%.0f kg".format(mv.volume),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.width(70.dp),
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    " (${mv.workouts})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun SimpleLineChart(
    values: List<Double>,
    dates: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    if (values.isEmpty()) return

    val density = LocalDensity.current
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Canvas(modifier = modifier) {
        val paddingLeft = with(density) { 48.dp.toPx() }
        val paddingBottom = with(density) { 52.dp.toPx() }
        val paddingTop = with(density) { 16.dp.toPx() }
        val chartWidth = size.width - paddingLeft
        val chartHeight = size.height - paddingTop - paddingBottom

        val minVal = values.min()
        val maxVal = values.max()
        val range = if (maxVal - minVal < 0.001) 1.0 else maxVal - minVal

        val gridPaint = android.graphics.Paint().apply {
            color = gridColor.hashCode()
            strokeWidth = 1f
            style = android.graphics.Paint.Style.STROKE
        }

        val textPaint = android.graphics.Paint().apply {
            color = textColor.hashCode()
            textSize = with(density) { 10.sp.toPx() }
            textAlign = android.graphics.Paint.Align.RIGHT
        }

        val datePaint = android.graphics.Paint().apply {
            color = textColor.hashCode()
            textSize = with(density) { 9.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
        }

        for (i in 0..4) {
            val y = paddingTop + chartHeight * (1f - i / 4f)
            drawContext.canvas.nativeCanvas.drawLine(paddingLeft, y, size.width, y, gridPaint)
            val label = "%.0f".format(minVal + range * i / 4)
            drawContext.canvas.nativeCanvas.drawText(label, paddingLeft - 8f, y + 4f, textPaint)
        }

        if (values.size == 1) {
            val x = paddingLeft + chartWidth / 2f
            val y = paddingTop + chartHeight * (1f - ((values[0] - minVal) / range)).toFloat()
            drawCircle(lineColor, 5f, Offset(x, y))
            if (dates.isNotEmpty()) {
                drawContext.canvas.nativeCanvas.drawText(dates[0], x, size.height - 4f, datePaint)
            }
            return@Canvas
        }

        val path = Path()
        val stepX = chartWidth / (values.size - 1)

        values.forEachIndexed { i, v ->
            val x = paddingLeft + stepX * i
            val y = paddingTop + chartHeight * (1f - ((v - minVal) / range)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path, lineColor, style = Stroke(width = 3f))

        values.forEachIndexed { i, v ->
            val x = paddingLeft + stepX * i
            val y = paddingTop + chartHeight * (1f - ((v - minVal) / range)).toFloat()
            drawCircle(lineColor, 4f, Offset(x, y))
        }

        if (dates.isNotEmpty()) {
            val maxLabels = (chartWidth / with(density) { 40.dp.toPx() }).toInt().coerceAtLeast(2)
            val step = if (dates.size <= maxLabels) 1 else dates.size / maxLabels
            val indicesToShow = (dates.indices step step.coerceAtLeast(1)).toMutableSet()
            indicesToShow.add(dates.size - 1)
            for (i in indicesToShow) {
                val x = paddingLeft + stepX * i
                drawContext.canvas.nativeCanvas.save()
                drawContext.canvas.nativeCanvas.rotate(90f, x, size.height - 4f)
                drawContext.canvas.nativeCanvas.drawText(dates[i], x, size.height - 4f, datePaint)
                drawContext.canvas.nativeCanvas.restore()
            }
        }
    }
}

@Composable
fun PrCard(title: String, value: String, date: String?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            date?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}
