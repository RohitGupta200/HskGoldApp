package org.cap.gold.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.cap.gold.model.User

@Composable
fun AdminUserListItem(
    user: User,
    onRoleChange: (userId: String, newRole: Int) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.email ?: user.id, style = MaterialTheme.typography.titleMedium)
                Text(text = "Role: ${roleLabel(user.role)}", style = MaterialTheme.typography.bodyMedium)
            }
            Row {
                TextButton(onClick = { menuExpanded = true }) { Text("Change Role") }
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Admin") }, onClick = {
                        menuExpanded = false
                        if (user.role != 0) onRoleChange(user.id, 0)
                    })
                    DropdownMenuItem(text = { Text("Approved User") }, onClick = {
                        menuExpanded = false
                        if (user.role != 1) onRoleChange(user.id, 1)
                    })
                    DropdownMenuItem(text = { Text("Unapproved User") }, onClick = {
                        menuExpanded = false
                        if (user.role != 2) onRoleChange(user.id, 2)
                    })
                }
            }
        }
    }
}

private fun roleLabel(role: Int): String = when (role) {
    0 -> "Admin"
    1 -> "Approved"
    else -> "Unapproved"
}
