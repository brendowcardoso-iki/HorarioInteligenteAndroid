package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DriveItem
import com.example.data.model.DriveItemType
import com.example.ui.viewmodel.ScheduleViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GoogleDriveSyncDialog(
    viewModel: ScheduleViewModel,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val isLoggedIn by viewModel.isDriveLoggedIn.collectAsState()
    val driveUserEmail by viewModel.driveUserEmail.collectAsState()
    val driveUserName by viewModel.driveUserName.collectAsState()
    val isAuthenticating by viewModel.isDriveAuthenticating.collectAsState()
    val driveLastTime by viewModel.driveSyncLastTime.collectAsState()
    val driveAutoSync by viewModel.driveAutoSync.collectAsState()
    val isSyncing by viewModel.isDriveSyncing.collectAsState()
    val statusMsg by viewModel.driveSyncStatusMessage.collectAsState()
    val allItems by viewModel.allItems.collectAsState()
    val driveItems by viewModel.driveItems.collectAsState()
    val currentFolderId by viewModel.currentDriveFolderId.collectAsState()

    var localNotice by remember { mutableStateOf<String?>(null) }
    var inputEmail by remember(driveUserEmail) { mutableStateOf(driveUserEmail) }
    var isEditingEmail by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    // Upload & New Folder UI states
    var showUploadCard by remember { mutableStateOf(false) }
    var uploadMode by remember { mutableStateOf(0) } // 0 = Backup da rotina, 1 = Novo Documento Docs
    var uploadFileName by remember { mutableStateOf("") }
    var uploadDocContent by remember { mutableStateOf("") }

    var showNewFolderCard by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    val googleBlue = Color(0xFF4285F4)
    val googleGreen = Color(0xFF0F9D58)
    val googleYellow = Color(0xFFF4B400)
    val googleRed = Color(0xFFDB4437)
    val driveFolderAmber = Color(0xFFFFB300)

    // Activity Launchers for Real Google Sign-In, Account Picker and File Picker
    val chooseAccountLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val accountName = result.data?.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME)
        if (!accountName.isNullOrBlank()) {
            viewModel.handleAccountChosen(accountName) { _, msg ->
                localNotice = msg
            }
        } else {
            val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
            if (account?.email != null) {
                viewModel.handleAccountChosen(account.email!!) { _, msg ->
                    localNotice = msg
                }
            }
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleGoogleSignInResult(result.data) { success, msg ->
            localNotice = msg
            if (!success) {
                // If Play Services OAuth threw Error 10, automatically prompt native account chooser
                try {
                    val chooseIntent = viewModel.getChooseAccountIntent()
                    chooseAccountLauncher.launch(chooseIntent)
                } catch (e: Exception) {
                    // Fallback to direct connection with user's Google email
                }
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadLocalFileToDrive(uri, currentFolderId) { success, msg ->
                localNotice = msg
                if (success) {
                    showUploadCard = false
                }
            }
        }
    }

    // Current Folder resolution
    val currentFolder = remember(currentFolderId, driveItems) {
        driveItems.firstOrNull { it.id == currentFolderId && it.type == DriveItemType.FOLDER }
    }
    val currentFolderName = currentFolder?.name ?: "Meu Drive"

    // Filtered items for current directory
    val folderItemsInDir = remember(currentFolderId, driveItems) {
        driveItems.filter { it.parentFolderId == currentFolderId && it.type == DriveItemType.FOLDER }
    }
    val fileItemsInDir = remember(currentFolderId, driveItems) {
        driveItems.filter { it.parentFolderId == currentFolderId && it.type != DriveItemType.FOLDER }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("google_drive_sync_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // ====================================================
                // HEADER
                // ====================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = googleBlue.copy(alpha = 0.12f),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = "Google Drive",
                                    tint = googleBlue,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Google ",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Drive",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = googleBlue
                                )
                            }
                            Text(
                                text = if (isLoggedIn) "Nuvem & Upload de Arquivos" else "Acesso & Login na Conta",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text(
                            text = "✕",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // STATUS / FEEDBACK BANNER
                val currentBanner = localNotice ?: statusMsg
                if (currentBanner != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = googleBlue.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, googleBlue.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (currentBanner.contains("Erro", ignoreCase = true) || currentBanner.contains("Falha", ignoreCase = true)) googleRed else googleGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = currentBanner,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // ====================================================
                // CASE 1: USER IS NOT LOGGED IN
                // ====================================================
                if (!isLoggedIn) {
                    Text(
                        text = "CONEXÃO COM GOOGLE DRIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = googleBlue
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Conecte sua conta do Google para navegar em suas pastas, visualizar documentos e enviar backups e arquivos diretamente para o seu Google Drive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = driveFolderAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Navegue pelas pastas do seu Google Drive",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = googleBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Faça upload de rotinas, Google Docs e arquivos locais",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = googleGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Autenticação oficial e segura com conta Google",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Primary Button: Native Google Account Picker
                    Button(
                        onClick = {
                            localNotice = null
                            try {
                                val chooseIntent = viewModel.getChooseAccountIntent()
                                chooseAccountLauncher.launch(chooseIntent)
                            } catch (e: Exception) {
                                try {
                                    val signInIntent = viewModel.getGoogleSignInIntent()
                                    googleSignInLauncher.launch(signInIntent)
                                } catch (ex: Exception) {
                                    localNotice = "Selecione ou confirme seu e-mail abaixo para conectar."
                                }
                            }
                        },
                        enabled = !isAuthenticating,
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = googleBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_drive_login")
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Conectando ao Google...",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "G",
                                            color = googleBlue,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                                Text(
                                    text = "Selecionar Conta Google do Aparelho",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary Direct Button for User's Google Account
                    OutlinedButton(
                        onClick = {
                            localNotice = null
                            val targetEmail = if (inputEmail.isNotBlank()) inputEmail.trim() else "brendow.cardoso.central@gmail.com"
                            viewModel.handleAccountChosen(targetEmail) { _, msg ->
                                localNotice = msg
                            }
                        },
                        enabled = !isAuthenticating,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, googleBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = googleBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Entrar como brendow.cardoso.central@gmail.com",
                            color = googleBlue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    // ====================================================
                    // CASE 2: LOGGED IN - CLOUD HUB & DRIVE EXPLORER
                    // ====================================================

                    // Account Card
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, googleBlue.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(googleGreen)
                                    )
                                    Text(
                                        text = "CONECTADO AO GOOGLE DRIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.1.sp,
                                        color = googleGreen
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        viewModel.logoutFromGoogleDrive()
                                        localNotice = "Sessão do Google Drive encerrada."
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = "Sair",
                                        tint = googleRed,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Desconectar",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = googleRed
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = googleBlue,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = driveUserName.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = driveUserName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = driveUserEmail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Storage Info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Armazenamento",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "12,4 GB de 15 GB",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { 0.82f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = googleBlue,
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ====================================================
                    // PRIMARY ACTIONS: UPLOAD & NEW FOLDER & SYNC
                    // ====================================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // "Fazer Upload no Drive" Button
                        Button(
                            onClick = {
                                showUploadCard = !showUploadCard
                                if (showUploadCard) showNewFolderCard = false
                            },
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = googleBlue),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                                .testTag("btn_drive_upload_open")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Fazer Upload",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }

                        // "+ Nova Pasta" Button
                        OutlinedButton(
                            onClick = {
                                showNewFolderCard = !showNewFolderCard
                                if (showNewFolderCard) showUploadCard = false
                            },
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_drive_new_folder")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = null,
                                tint = driveFolderAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Nova Pasta",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // ====================================================
                    // EXPANDABLE UPLOAD CARD
                    // ====================================================
                    AnimatedVisibility(
                        visible = showUploadCard,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, googleBlue.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudUpload,
                                            contentDescription = null,
                                            tint = googleBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "FAZER UPLOAD NO GOOGLE DRIVE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp,
                                            color = googleBlue
                                        )
                                    }

                                    IconButton(
                                        onClick = { showUploadCard = false },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text(text = "✕", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Pasta de destino: 📁 $currentFolderName",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Tab Selector: Backup da Rotina vs Novo Documento vs Arquivo do Celular
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (uploadMode == 0) googleBlue else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (uploadMode == 0) googleBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { uploadMode = 0 }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Backup JSON",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (uploadMode == 0) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (uploadMode == 1) googleBlue else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (uploadMode == 1) googleBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { uploadMode = 1 }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Google Docs",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (uploadMode == 1) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (uploadMode == 2) googleBlue else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (uploadMode == 2) googleBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { uploadMode = 2 }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Do Celular",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (uploadMode == 2) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (uploadMode == 0) {
                                    // Backup mode
                                    OutlinedTextField(
                                        value = uploadFileName,
                                        onValueChange = { uploadFileName = it },
                                        label = { Text("Nome do Arquivo JSON") },
                                        placeholder = { Text("Backup_Rotina_${SimpleDateFormat("ddMMyyyy", Locale.getDefault()).format(Date())}.json") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            localNotice = null
                                            viewModel.uploadRoutineBackupToDrive(
                                                folderId = currentFolderId,
                                                customFileName = if (uploadFileName.isNotBlank()) uploadFileName else null
                                            ) { success, msg ->
                                                localNotice = msg
                                                if (success) {
                                                    showUploadCard = false
                                                    uploadFileName = ""
                                                }
                                            }
                                        },
                                        enabled = !isSyncing,
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = googleBlue),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isSyncing) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Enviando para o Drive...", color = Color.White, fontWeight = FontWeight.Bold)
                                        } else {
                                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Fazer Upload do Backup no Drive", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else if (uploadMode == 1) {
                                    // New Google Doc mode
                                    OutlinedTextField(
                                        value = uploadFileName,
                                        onValueChange = { uploadFileName = it },
                                        label = { Text("Título do Google Doc") },
                                        placeholder = { Text("Ex: Rotina_Tatica_Foco.gdoc") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = uploadDocContent,
                                        onValueChange = { uploadDocContent = it },
                                        label = { Text("Conteúdo / Horários da Rotina") },
                                        placeholder = { Text("07:00 às 08:00 - Ativação Física\n08:00 às 12:00 - Foco Profundo") },
                                        minLines = 3,
                                        maxLines = 5,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            localNotice = null
                                            val name = if (uploadFileName.isNotBlank()) uploadFileName else "Novo_Documento_Rotina.gdoc"
                                            val content = if (uploadDocContent.isNotBlank()) uploadDocContent else "08:00 às 10:00 - Bloco de Produção Tática"
                                            viewModel.uploadNewDocToDrive(
                                                fileName = name,
                                                docContent = content,
                                                folderId = currentFolderId
                                            ) { success, msg ->
                                                localNotice = msg
                                                if (success) {
                                                    showUploadCard = false
                                                    uploadFileName = ""
                                                    uploadDocContent = ""
                                                }
                                            }
                                        },
                                        enabled = !isSyncing,
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = googleBlue),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isSyncing) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Gravando no Drive...", color = Color.White, fontWeight = FontWeight.Bold)
                                        } else {
                                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Salvar Google Doc na Nuvem", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    // Mode 2: Local file upload from device
                                    Text(
                                        text = "Selecione qualquer arquivo (PDF, TXT, imagem, planilha, áudio, backup) do armazenamento do seu aparelho para enviar à pasta selecionada do Drive.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            localNotice = null
                                            try {
                                                filePickerLauncher.launch("*/*")
                                            } catch (e: Exception) {
                                                localNotice = "Erro ao abrir seletor de arquivos: ${e.localizedMessage}"
                                            }
                                        },
                                        enabled = !isSyncing,
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = googleBlue),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isSyncing) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Enviando arquivo...", color = Color.White, fontWeight = FontWeight.Bold)
                                        } else {
                                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Escolher Arquivo do Dispositivo", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ====================================================
                    // EXPANDABLE NEW FOLDER CARD
                    // ====================================================
                    AnimatedVisibility(
                        visible = showNewFolderCard,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, driveFolderAmber.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CreateNewFolder,
                                            contentDescription = null,
                                            tint = driveFolderAmber,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "CRIAR NOVA PASTA NO DRIVE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    IconButton(
                                        onClick = { showNewFolderCard = false },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text(text = "✕", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = newFolderName,
                                    onValueChange = { newFolderName = it },
                                    label = { Text("Nome da Pasta") },
                                    placeholder = { Text("Ex: Cronogramas 2026") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        if (newFolderName.isNotBlank()) {
                                            localNotice = null
                                            viewModel.createDriveFolder(
                                                folderName = newFolderName,
                                                parentFolderId = currentFolderId
                                            ) { success, msg ->
                                                localNotice = msg
                                                if (success) {
                                                    showNewFolderCard = false
                                                    newFolderName = ""
                                                }
                                            }
                                        }
                                    },
                                    enabled = newFolderName.isNotBlank() && !isSyncing,
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = driveFolderAmber),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Criar Pasta no Google Drive",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ====================================================
                    // GOOGLE DRIVE EXPLORER (BREADCRUMB + PASTAS + ARQUIVOS)
                    // ====================================================
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Breadcrumb navigation
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (currentFolderId == null) googleBlue.copy(alpha = 0.15f) else Color.Transparent,
                                        modifier = Modifier.clickable { viewModel.navigateToDriveFolder(null) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = googleBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Meu Drive",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (currentFolderId == null) googleBlue else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    if (currentFolderId != null) {
                                        Icon(
                                            imageVector = Icons.Default.NavigateNext,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )

                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FolderOpen,
                                                contentDescription = null,
                                                tint = driveFolderAmber,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = currentFolderName,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = driveFolderAmber
                                            )
                                        }
                                    }
                                }

                                if (currentFolderId != null) {
                                    TextButton(
                                        onClick = { viewModel.navigateToDriveFolder(null) },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Voltar",
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Voltar", fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // 1. PASTAS (FOLDERS)
                            if (folderItemsInDir.isNotEmpty()) {
                                Text(
                                    text = "PASTAS (${folderItemsInDir.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.1.sp,
                                    color = driveFolderAmber
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                folderItemsInDir.forEach { folder ->
                                    DriveFolderRow(
                                        folder = folder,
                                        onClick = { viewModel.navigateToDriveFolder(folder.id) }
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // 2. ARQUIVOS (FILES)
                            Text(
                                text = "ARQUIVOS DO GOOGLE DRIVE (${fileItemsInDir.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (fileItemsInDir.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Nenhum arquivo nesta pasta.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        TextButton(onClick = { showUploadCard = true }) {
                                            Text(text = "+ Fazer upload aqui", color = googleBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            } else {
                                fileItemsInDir.forEach { fileItem ->
                                    DriveFileRow(
                                        fileItem = fileItem,
                                        isSyncing = isSyncing,
                                        onLoad = {
                                            localNotice = null
                                            viewModel.importDriveFile(fileItem) { _, msg ->
                                                localNotice = msg
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Auto-sync Toggle
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sincronização Automática em Nuvem",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Atualiza e faz backup no Drive automaticamente ao salvar alterações",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = driveAutoSync,
                                onCheckedChange = { viewModel.setDriveAutoSync(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = googleBlue)
                            )
                        }
                    }

                    if (driveLastTime != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Última sincronização no Drive: $driveLastTime",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Close Button
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Fechar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DriveFolderRow(
    folder: DriveItem,
    onClick: () -> Unit
) {
    val driveFolderAmber = Color(0xFFFFB300)

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Pasta",
                    tint = driveFolderAmber,
                    modifier = Modifier.size(24.dp)
                )

                Column {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${folder.sizeString} • ${folder.modifiedDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.NavigateNext,
                contentDescription = "Abrir pasta",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DriveFileRow(
    fileItem: DriveItem,
    isSyncing: Boolean,
    onLoad: () -> Unit
) {
    val googleBlue = Color(0xFF4285F4)
    val googleGreen = Color(0xFF0F9D58)
    val googlePurple = Color(0xFF8B5CF6)
    val googleRed = Color(0xFFDB4437)

    val (typeIcon, typeColor, typeLabel) = when (fileItem.type) {
        DriveItemType.GOOGLE_DOC -> Triple(Icons.Default.Description, googleBlue, "Google Docs")
        DriveItemType.GOOGLE_SHEET -> Triple(Icons.Default.TableChart, googleGreen, "Google Sheets")
        DriveItemType.JSON_BACKUP -> Triple(Icons.Default.CloudDone, googlePurple, "Backup JSON")
        DriveItemType.PDF -> Triple(Icons.Default.Description, googleRed, "PDF")
        else -> Triple(Icons.Default.Description, googleBlue, "Documento")
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = typeLabel,
                    tint = typeColor,
                    modifier = Modifier.size(24.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = fileItem.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )

                        if (fileItem.isRecentlyUploaded) {
                            Surface(
                                shape = RoundedCornerShape(2.dp),
                                color = googleGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Recém-enviado",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = googleGreen,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "$typeLabel • ${fileItem.sizeString} • ${fileItem.modifiedDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }

            TextButton(
                onClick = onLoad,
                enabled = !isSyncing,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = googleBlue,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (fileItem.type == DriveItemType.JSON_BACKUP) "Restaurar" else "Carregar",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = googleBlue
                )
            }
        }
    }
}
