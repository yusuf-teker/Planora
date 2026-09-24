package com.yusufteker.planora.feature.home.presentation.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.yusufteker.planora.core.theme.PlanoraColors
import planora.core.generated.resources.Res
import planora.core.generated.resources.action_add_event
import planora.core.generated.resources.action_add_task
import planora.core.generated.resources.task_ic

@Composable
fun HomeFabMenu(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCreateTask: () -> Unit,
    onCreateEvent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(horizontalAlignment = Alignment.End, modifier = modifier) {

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.action_add_task),
                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    SmallFloatingActionButton(
                        onClick = {
                            onExpandedChange(false)
                            onCreateTask()
                        },
                        containerColor = PlanoraColors.TaskColor,
                        contentColor = androidx.compose.ui.graphics.Color.White,
                        shape = androidx.compose.foundation.shape.CircleShape
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.task_ic),
                            contentDescription = stringResource(Res.string.action_add_task)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.action_add_event),
                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    SmallFloatingActionButton(
                        onClick = {
                            onExpandedChange(false)
                            onCreateEvent()
                        },
                        containerColor = PlanoraColors.EventColor,
                        contentColor = androidx.compose.ui.graphics.Color.White,
                        shape = androidx.compose.foundation.shape.CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = stringResource(Res.string.action_add_event)
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                onExpandedChange(!isExpanded)
            },
            containerColor = MaterialTheme.colorScheme.primary,
            shape = androidx.compose.foundation.shape.CircleShape,
        ) {
            Icon(
                imageVector = if (isExpanded) {
                    Icons.Default.Close
                } else {
                    Icons.Default.Add
                },
                contentDescription = null
            )
        }
    }
}