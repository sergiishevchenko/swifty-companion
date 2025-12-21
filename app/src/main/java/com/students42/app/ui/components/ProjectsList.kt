package com.students42.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.students42.app.data.models.ProjectModel

enum class ProjectFilter {
    ALL, PISCINE, COMMON_CORE, ADVANCED_CORE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsList(projects: List<ProjectModel>) {
    var selectedFilter by remember { mutableStateOf(ProjectFilter.ALL) }

    val filteredProjects = when (selectedFilter) {
        ProjectFilter.ALL -> projects
        ProjectFilter.PISCINE -> projects.filter { it.isPiscine }
        ProjectFilter.COMMON_CORE -> projects.filter { it.isCommonCore }
        ProjectFilter.ADVANCED_CORE -> projects.filter { it.isAdvancedCore }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp, top = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Projects",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == ProjectFilter.ALL,
                onClick = { selectedFilter = ProjectFilter.ALL },
                label = { Text("All") },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            FilterChip(
                selected = selectedFilter == ProjectFilter.PISCINE,
                onClick = { selectedFilter = ProjectFilter.PISCINE },
                label = { Text("Piscine") },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            FilterChip(
                selected = selectedFilter == ProjectFilter.COMMON_CORE,
                onClick = { selectedFilter = ProjectFilter.COMMON_CORE },
                label = { Text("Common") },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            FilterChip(
                selected = selectedFilter == ProjectFilter.ADVANCED_CORE,
                onClick = { selectedFilter = ProjectFilter.ADVANCED_CORE },
                label = { Text("Advanced") },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }

        filteredProjects.forEach { project ->
            ProjectCard(project = project)
        }
    }
}

@Composable
private fun ProjectCard(project: ProjectModel) {
    val cardColor: Color
    val icon: androidx.compose.ui.graphics.vector.ImageVector
    val iconColor: Color
    val statusText: String
    
    when {
        project.isCompleted -> {
            cardColor = Color(0xFF4CAF50)
            icon = Icons.Default.CheckCircle
            iconColor = Color.White
            statusText = "Completed"
        }
        project.isFailed -> {
            cardColor = Color(0xFFF44336)
            icon = Icons.Default.Close
            iconColor = Color.White
            statusText = "Failed"
        }
        else -> {
            cardColor = MaterialTheme.colorScheme.surface
            icon = Icons.Default.CheckCircle
            iconColor = MaterialTheme.colorScheme.primary
            statusText = project.status ?: "Unknown"
        }
    }

    val textColor = if (project.isCompleted || project.isFailed) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (project.isCompleted || project.isFailed) 6.dp else 4.dp
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = if (project.isCompleted || project.isFailed) 0.3f else 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = statusText,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = project.name ?: "Unknown Project",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )
                project.finalMark?.let {
                    Text(
                        text = "Mark: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

