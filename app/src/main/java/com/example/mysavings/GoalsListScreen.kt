package com.example.mysavings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.LinearProgressIndicator

@Composable
fun GoalsListScreen(navController: NavController, viewModel: GoalsViewModel) {
    val goalsState by viewModel.goalsState.collectAsState()
    val activeGoals = goalsState.activeGoals
    val completedGoals = goalsState.completedGoals

    LaunchedEffect(key1 = Unit) {
        viewModel.navigateToGoal.collect { goalId ->
            navController.navigate(Screen.AddGoalScreen.route)
        }
    }

    if (viewModel.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeletion() },
            title = { Text("Подтверждение") },
            text = { Text("Вы уверены, что хотите удалить эту цель?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeletion() }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDeletion() }) { Text("Отмена") }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAddGoalClicked() }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить новую цель")
            }
        }
    ) { paddingValues ->
        if (activeGoals.isEmpty() && completedGoals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("У вас пока нет целей. Нажмите +, чтобы добавить первую!")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (activeGoals.isNotEmpty()) {
                    item {
                        Text(
                            "Активные цели",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(activeGoals, key = { it.goal.id }) { goalItem ->
                        GoalCard(
                            goalWithProgress = goalItem,
                            onEditClicked = { viewModel.onEditGoalClicked(goalItem.goal) },
                            onDeleteClicked = { viewModel.onDeleteGoalClicked(goalItem.goal) }
                        )
                    }
                }

                if (completedGoals.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Завершенные цели",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(completedGoals, key = { it.goal.id }) { goalItem ->
                        GoalCard(
                            goalWithProgress = goalItem,
                            onEditClicked = { viewModel.onEditGoalClicked(goalItem.goal) },
                            onDeleteClicked = { viewModel.onDeleteGoalClicked(goalItem.goal) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoalCard(
    goalWithProgress: GoalWithProgress,
    onEditClicked: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("ru", "RU"))

    val goal = goalWithProgress.goal
    val rawCurrentAmount = goalWithProgress.currentAmount
    val isCompleted = rawCurrentAmount >= goal.targetAmount

    val displayAmount = if (isCompleted) goal.targetAmount else rawCurrentAmount
    val progress = if (isCompleted) 1f else (rawCurrentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Опции")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Редактировать") },
                            onClick = {
                                onEditClicked()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Удалить") },
                            onClick = {
                                onDeleteClicked()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null) }
                        )
                    }
                }
            }
            goal.description?.let {
                if (it.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currencyFormat.format(displayAmount),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currencyFormat.format(goal.targetAmount),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}