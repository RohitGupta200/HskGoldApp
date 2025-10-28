package org.cap.gold.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import org.cap.gold.auth.AuthService
import org.cap.gold.model.User
import org.cap.gold.profile.ProfileService
import org.koin.compose.koinInject
import kotlinx.coroutines.launch
import org.cap.gold.ui.components.LocalStatusDialogState
import org.cap.gold.data.remote.ProductApiService
import org.cap.gold.ui.components.AboutUsImage

@Composable
fun AccountScreen(
    user: User,
    onLogout: () -> Unit
) {
    val navigator = LocalNavigator.current
    val statusDialog = LocalStatusDialogState.current
    val scope = rememberCoroutineScope()
    val isAdmin = remember(user.role) { user.role != 0 }
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(scrollState)
    ) {
        // Title Row (tab layout has no back, so only centered title)
        Text("Account", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(16.dp))

        // Personal Information
        Text("Personal Information", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        InlineChangeRow(
            label = "Name",
            value = user.displayName?:"",
            onChangeClick = { navigator?.push(ChangeNameVoyagerScreen(currentName = user.displayName?:"")) }
        )

        Spacer(Modifier.height(20.dp))

        // Contact Details
        Text("Contact Details", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        InlineChangeRow(
            label = "Phone",
            value = user.phoneNumber,
            onChangeClick = { navigator?.push(ChangePhoneVoyagerScreen(currentPhone = user.phoneNumber)) }
        )
        if(!(user.role==0))
            InlineChangeRow(
                label = "Shop Name",
                value = user.shopName ?: "",
                onChangeClick = { navigator?.push(ChangeEmailVoyagerScreen(currentEmail = user.shopName ?: "")) }
            )

        Spacer(Modifier.height(20.dp))

        // About Us Section
        val api: ProductApiService = koinInject()
        var aboutUs by remember { mutableStateOf("") }
        LaunchedEffect(Unit) {
            try { aboutUs = api.getAboutUs() } catch (_: Throwable) { aboutUs = "" }
        }
        Text("About H.S Kothari Jewellers", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        if (user.role==0) {
            OutlinedTextField(
                value = aboutUs,
                onValueChange = { aboutUs = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Add text here") },
                minLines = 1,
                maxLines = 10
            )
        } else {
            val isBlank = aboutUs.isBlank()
            Text(
                text = if (isBlank) " " else aboutUs,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isBlank) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f) else MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(12.dp))
        // About image (platform-specific)
        AboutUsImage(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        )
        
        if (user.role==0) {
            Spacer(Modifier.height(12.dp))
            var saving by remember { mutableStateOf(false) }
            PrimaryPillButton(text = if (saving) "Saving..." else "Save About Us") {
                if (saving) return@PrimaryPillButton
                saving = true
                scope.launch {
                    val ok = try { api.setAboutUs(aboutUs) } catch (_: Throwable) { false }
                    if (ok) {
                        // Show success and refresh content (optional)
                        scope.launch {
                            statusDialog.show(success = true, message = "About Us updated successfully")
                            delay(2000)
                            statusDialog.hide()
                        }
                    }
                    saving = false
                }
            }
        }

        // Change Password action (secondary pill)
        Spacer(Modifier.height(6.dp))
        SecondaryPillButton(text = "Change password") {
            navigator?.push(ChangePasswordVoyagerScreen())
        }

        // Logout primary pill at bottom
        Spacer(Modifier.height(6.dp))
        PrimaryPillButton(text = "Logout", onClick = onLogout)

        // Delete Account button
        Spacer(Modifier.height(15.dp))
        var showDeleteDialog by remember { mutableStateOf(false) }
        TextButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Delete account", color = Color.Red, style = MaterialTheme.typography.bodyLarge)
        }

        // Delete Account Dialog
        if (showDeleteDialog) {
            val auth: AuthService = koinInject()
            DeleteAccountDialog(
                phoneNumber = user.phoneNumber,
                onDismiss = { showDeleteDialog = false },
                onConfirm = { password ->
                    scope.launch {
                        val result = auth.deleteAccount(user.phoneNumber, password)
                        if (result is org.cap.gold.auth.model.AuthResult.Success) {
                            statusDialog.show(success = true, message = "Account deleted successfully")
                            delay(1500)
                            statusDialog.hide()
                            onLogout()
                            showDeleteDialog = false
                        } else if (result is org.cap.gold.auth.model.AuthResult.Error) {
                            statusDialog.show(success = false, message = result.message)
                            delay(2500)
                            statusDialog.hide()
                        }

                    }
                }
            )
        }
    }
}

// Inline row: Label, value, and CHANGE action on the right
@Composable
private fun InlineChangeRow(label: String, value: String, onChangeClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                value.ifBlank { "" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onChangeClick) {
                Text("CHANGE", color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FilledInput(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    isPassword: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(isPassword) }
    val bg = Color(0xffffff)
    Surface(color = bg) {
        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                visualTransformation = if (!passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    if(isPassword) {
                        val icon =
                            if (!passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(icon, contentDescription = "Toggle password visibility")
                        }
                    }
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth()
            )
            if (value.isBlank() && placeholder.isNotBlank()) {
                Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun PrimaryPillButton(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B2F7F), contentColor = Color.White)
    ) { Text(text) }
}

@Composable
private fun SecondaryPillButton(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = shape,
        modifier = Modifier.fillMaxWidth().height(48.dp).clickable { onClick() }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// Voyager Screens and Composables for changes
class ChangePasswordVoyagerScreen : Screen { override val key: ScreenKey = "change_password"; @Composable override fun Content() { ChangePasswordScreen() } }
class ChangePhoneVoyagerScreen(private val currentPhone: String) : Screen { override val key: ScreenKey = "change_phone"; @Composable override fun Content() { ChangePhoneScreen(currentPhone) } }
class ChangeEmailVoyagerScreen(private val currentEmail: String) : Screen { override val key: ScreenKey = "change_email"; @Composable override fun Content() { ChangeEmailScreen(currentEmail) } }
class ChangeNameVoyagerScreen(private val currentName: String) : Screen { override val key: ScreenKey = "change_name"; @Composable override fun Content() { ChangeNameScreen(currentName) } }

@Composable
private fun ChangePasswordScreen(profile: ProfileService = koinInject(), auth: AuthService = koinInject()) {
    val navigator = LocalNavigator.current
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val statusDialog = LocalStatusDialogState.current

    Scaffold(
        topBar = { TopBar(title = "Change password", onBack = { navigator?.pop() }) },
        bottomBar = {
            Box(Modifier.fillMaxWidth().padding(16.dp)) {
                PrimaryPillButton(text = if (loading) "Updating..." else "Update password") {
                    error = null
                    if (new.length < 6) { error = "Password must be at least 6 characters"; return@PrimaryPillButton }
                    if (new != confirm) { error = "New passwords do not match"; return@PrimaryPillButton }
                    loading = true
                    scope.launch {
                        try {
                            profile.changePassword(currentPassword = current, newPassword = new)
                            auth.checkAuthState()
                            CoroutineScope(Dispatchers.IO).launch {
                                statusDialog.show(success = true, message = "Password updated successfully")
                                delay(2500)
                                statusDialog.hide()
                            }

                            navigator?.pop()
                        } catch (e: Exception) {
                            error = e.message ?: "Failed to change password"
                        } finally { loading = false }
                    }
                }
            }
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LabeledFilledField(label = "Enter current password", value = current, onChange = { current = it }, password = true)
            LabeledFilledField(label = "Enter new password", value = new, onChange = { new = it }, password = true)
            LabeledFilledField(label = "Re-enter new password", value = confirm, onChange = { confirm = it }, password = true)
            if (error != null) ErrorChip(error!!)
        }
    }
}

@Composable
private fun ChangePhoneScreen(currentPhone: String, profile: ProfileService = koinInject(), auth: AuthService = koinInject()) {
    val navigator = LocalNavigator.current
    var phone by remember { mutableStateOf(currentPhone) }
    var error by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val statusDialog = LocalStatusDialogState.current

    Scaffold(
        topBar = { TopBar(title = "Change Phone", onBack = { navigator?.pop() }) },
        bottomBar = {
            Box(Modifier.fillMaxWidth().padding(16.dp)) {
                PrimaryPillButton(text = if (loading) "Updating..." else "Update phone") {
                    error = null
                    if (phone.isBlank()) { error = "Enter phone"; return@PrimaryPillButton }
                    loading = true
                    scope.launch {
                        try {
                            profile.changePhone(phone,password)
                            auth.checkAuthState()
                            CoroutineScope(Dispatchers.IO).launch {
                                statusDialog.show(success = true, message = "Phone updated successfully")
                                delay(2500)
                                statusDialog.hide()
                            }
                            navigator?.pop()
                        }
                        catch (e: Exception) { error = e.message ?: "Failed to change phone" }
                        finally { loading = false }
                    }
                }
            }
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LabeledFilledField(label = "Enter your password", value = password, onChange = {password = it}, password = true, enabled = true)
            LabeledFilledField(label = "Enter new phone", value = phone, onChange = { phone = it }, keyboardType = KeyboardType.Phone)
            if (error != null) ErrorChip(error!!)
        }
    }
}

@Composable
private fun ChangeEmailScreen(currentEmail: String, profile: ProfileService = koinInject(), auth: AuthService = koinInject()) {
    val navigator = LocalNavigator.current
    var email by remember { mutableStateOf(currentEmail) }
    var error by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val statusDialog = LocalStatusDialogState.current

    Scaffold(
        topBar = { TopBar(title = "Change Shop Name", onBack = { navigator?.pop() }) },
        bottomBar = {
            Box(Modifier.fillMaxWidth().padding(16.dp)) {
                PrimaryPillButton(text = if (loading) "Updating..." else "Update Shop Name") {
                    error = null
                    loading = true
                    scope.launch {
                        try {
                            profile.changeEmail(email,password)
                            auth.checkAuthState()
                            CoroutineScope(Dispatchers.IO).launch {
                                statusDialog.show(success = true, message = "Shop updated successfully")
                                delay(2500)
                                statusDialog.hide()
                            }
                            navigator?.pop()
                        }
                        catch (e: Exception) { error = e.message ?: "Failed to change Shop Name" }
                        finally { loading = false }
                    }
                }
            }
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LabeledFilledField(label = "Enter your password", value = password, onChange = {password = it}, password = true, enabled = true)
            LabeledFilledField(label = "Enter new Shop Name", value = email, onChange = { email = it }, keyboardType = KeyboardType.Email)
            if (error != null) ErrorChip(error!!)
        }
    }
}

@Composable
private fun ChangeNameScreen(currentName: String, profile: ProfileService = koinInject(), auth: AuthService = koinInject()) {
    val navigator = LocalNavigator.current
    var name by remember { mutableStateOf(currentName) }
    var error by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val statusDialog = LocalStatusDialogState.current

    Scaffold(
        topBar = { TopBar(title = "Change Name", onBack = { navigator?.pop() }) },
        bottomBar = {
            Box(Modifier.fillMaxWidth().padding(16.dp)) {
                PrimaryPillButton(text = if (loading) "Saving..." else "Save") {
                    error = null
                    if (name.isBlank()) { error = "Enter name"; return@PrimaryPillButton }
                    loading = true
                    scope.launch {
                        try {
                            profile.changeName(name,password)
                            auth.checkAuthState()
                            CoroutineScope(Dispatchers.IO).launch {
                                statusDialog.show(success = true, message = "Name updated successfully")
                                delay(2500)
                                statusDialog.hide()
                            }
                            navigator?.pop()
                        }
                        catch (e: Exception) { error = e.message ?: "Failed to change name" }
                        finally { loading = false }
                    }
                }
            }
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LabeledFilledField(label = "Enter your password", value = password, onChange = {password = it}, password = true, enabled = true)
            LabeledFilledField(label = "Name", value = name, onChange = { name = it })
            if (error != null) ErrorChip(error!!)
        }
    }
}

@Composable
private fun LabeledFilledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    enabled: Boolean = true
) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(6.dp))
    FilledInput(
        value = value,
        placeholder = "",
        onValueChange = onChange,
        enabled = enabled,
        isPassword = password
    )
}

@Composable
private fun ErrorChip(text: String) {
    val bg = Color(0xFFFFE9E9)
    val shape = RoundedCornerShape(8.dp)
    Surface(color = bg, shape = shape) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⚠", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(8.dp))
            Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Surface(shadowElevation = 0.dp) {
        CenterAlignedTopAppBar(
            title = { Text(title) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

    }
}

@Composable
private fun DeleteAccountDialog(
    phoneNumber: String,
    onDismiss: () -> Unit,
    onConfirm: (password: String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Delete account",
                style = MaterialTheme.typography.titleLarge,
                color = Color.Red
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Are you sure you want to delete your account? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Phone: $phoneNumber",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        error = null
                    },
                    label = { Text("Enter your password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(icon, contentDescription = "Toggle password visibility")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null
                )
                if (error != null) {
                    Text(
                        error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.isBlank()) {
                        error = "Password is required"
                    } else {
                        onConfirm(password)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                )
            ) {
                Text("Delete account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
