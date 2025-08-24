package org.cap.gold.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.cap.gold.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListItem(
    user: User,
    onRoleChange: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRoleMenu by remember { mutableStateOf(false) }
    
    Card(
        onClick = { showRoleMenu = true },
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                when (user.role) {
                    0 -> Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    1 -> Icon(
                        Icons.Default.Star,
                        contentDescription = "Approved User",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    2 -> Icon(
                        Icons.Default.Star,
                        contentDescription = "Unapproved User",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    else -> Icon(
                        Icons.Default.Person,
                        contentDescription = "User",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // User info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.displayName ?: "User ${user.phoneNumber.takeLast(4)}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
                
                if (user.email != null) {
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = user.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Role indicator and menu
            Box {
                // Current role chip
                val (roleText, roleColor) = when (user.role) {
                    0 -> "Admin" to MaterialTheme.colorScheme.primary
                    1 -> "Approved" to MaterialTheme.colorScheme.secondary
                    2 -> "Unapproved" to MaterialTheme.colorScheme.tertiary
                    else -> "User" to MaterialTheme.colorScheme.outline
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = roleText,
                        color = roleColor,
                        style = MaterialTheme.typography.labelMedium
                    )
                    IconButton(
                        onClick = { showRoleMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Change role",
                            tint = roleColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // Role selection dropdown
                DropdownMenu(
                    expanded = showRoleMenu,
                    onDismissRequest = { showRoleMenu = false }
                ) {
                    listOf(
                        "Admin" to 0,
                        "Approved" to 1,
                        "Unapproved" to 2,
                        "User" to 3
                    ).forEach { (roleName, roleValue) ->
                        DropdownMenuItem(
                            text = { Text(roleName) },
                            onClick = {
                                onRoleChange(user.id, roleValue)
                                showRoleMenu = false
                            },
                            leadingIcon = {
                                if (user.role == roleValue) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    // Empty icon for alignment
                                    Spacer(modifier = Modifier.size(24.dp))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
