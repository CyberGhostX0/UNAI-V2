package com.umbranexus.unaiv2.ui

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.umbranexus.unaiv2.ChatMessage
import com.umbranexus.unaiv2.UnaiNavigation
import com.umbranexus.unaiv2.UnaiViewModel
import com.umbranexus.unaiv2.R
import com.umbranexus.unaiv2.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnaiScreen(viewModel: UnaiViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentScreen by viewModel.currentScreen

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = Color.Black
            ) {
                Surface(
                    color = Color.Black,
                    modifier = Modifier.fillMaxSize()
                ) {
                    UnaiSideMenu(viewModel, onClose = { scope.launch { drawerState.close() } })
                }
            }
        }
    ) {
        Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
            when (screen) {
                UnaiNavigation.CHAT -> MainChatScreen(viewModel, onOpenDrawer = { scope.launch { drawerState.open() } })
                UnaiNavigation.SAVED_CHATS -> SavedChatsScreen(viewModel)
                UnaiNavigation.SAVED_IMAGES -> VisionPlaceholderScreen(viewModel)
                UnaiNavigation.MEMORY_BANK -> MemoryBankScreen(viewModel)
                UnaiNavigation.SETTINGS -> SettingsScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainChatScreen(viewModel: UnaiViewModel, onOpenDrawer: () -> Unit) {
    var inputText by remember { mutableStateOf("") }
    val messages = viewModel.messages
    val isBusy by viewModel.isBusy
    val isSpeaking by viewModel.isSpeaking

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        data?.get(0)?.let { spokenText ->
            viewModel.sendMessage(spokenText)
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .clickable { onOpenDrawer() }
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(width = 24.dp, height = 2.dp).background(UnaiGold))
                    Box(modifier = Modifier.size(width = 18.dp, height = 2.dp).background(UnaiGold))
                    Box(modifier = Modifier.size(width = 12.dp, height = 2.dp).background(UnaiGold))
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Surface(
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, UnaiGold.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Umbra Nexus AI",
                            color = UnaiGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, UnaiGold.copy(alpha = 0.4f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        IconButton(onClick = { 
                            viewModel.sendMessage("Perform a vision sweep. Identify current environment protocols.")
                            // This triggers the Vision/Image Core logic
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                        }
                        
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Initiate command...", color = Color.Gray, fontSize = 16.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = TextStyle(fontSize = 16.sp)
                        )

                        IconButton(onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening...")
                            }
                            speechLauncher.launch(intent)
                        }) {
                            Icon(Icons.Default.Mic, contentDescription = "Mic", tint = Color.Gray)
                        }

                        Image(
                            painter = painterResource(id = R.drawable.adevar_logo),
                            contentDescription = "Send",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable(enabled = !isBusy) {
                                    if (inputText.isNotBlank()) {
                                        viewModel.sendMessage(inputText)
                                        inputText = ""
                                    }
                                },
                            alpha = if (isBusy) 0.5f else 1f,
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (messages.isEmpty()) {
                VoiceSphere(isBusy = isBusy, isSpeaking = isSpeaking)
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        reverseLayout = true
                    ) {
                        items(messages.reversed()) { message ->
                            ChatBubble(message, onRemember = { viewModel.addToMemory(it) })
                        }
                    }
                }
                VoiceSphere(isBusy = isBusy, isSpeaking = isSpeaking, modifier = Modifier.size(100.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedChatsScreen(viewModel: UnaiViewModel) {
    val savedChats = viewModel.savedChats
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SAVED CHATS", color = UnaiGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(UnaiNavigation.CHAT) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = UnaiGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        if (savedChats.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No encrypted sessions found.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(savedChats) { chat ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable { viewModel.loadSavedChat(chat) },
                        color = UnaiDarkPurple.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, UnaiGold.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = chat.firstOrNull()?.text?.take(40) ?: "Empty Session",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${chat.size} messages",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: UnaiViewModel) {
    var conversationKey by remember { mutableStateOf(viewModel.conversationKey.value) }
    var groqKey by remember { mutableStateOf(viewModel.groqKey.value) }
    var useGroq by remember { mutableStateOf(viewModel.useGroq.value) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SYSTEM SETTINGS", color = UnaiGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(UnaiNavigation.CHAT) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = UnaiGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("AI UPLINK CONFIGURATION", color = UnaiGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Use Groq Llama-3 (High Speed)", color = Color.White, fontSize = 14.sp)
                Switch(
                    checked = useGroq,
                    onCheckedChange = { useGroq = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = UnaiGold,
                        checkedTrackColor = UnaiGold.copy(alpha = 0.5f)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            KeyField(label = "Groq API Key (gsk_...)", value = groqKey, onValueChange = { groqKey = it })
            Spacer(modifier = Modifier.height(16.dp))

            KeyField(label = "Umbra Nexus Key (un_...)", value = conversationKey, onValueChange = { conversationKey = it })
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { viewModel.updateApiKeys(conversationKey, groqKey, useGroq) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = UnaiGold)
            ) {
                Text("SAVE PROTOCOLS", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun KeyField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Gray) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = UnaiGold,
            unfocusedBorderColor = Color.Gray,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryBankScreen(viewModel: UnaiViewModel) {
    val memories = viewModel.memoryBank
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MEMORY BANK", color = UnaiGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(UnaiNavigation.CHAT) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = UnaiGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        if (memories.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No data fragments indexed.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(memories) { memory ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, UnaiGold.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = memory.content, color = Color.White, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "INDEX: ${memory.id.take(8)}",
                                    color = UnaiGold.copy(alpha = 0.6f),
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "PURGE",
                                    color = Color.Red.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { viewModel.removeMemory(memory) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceSphere(
    isBusy: Boolean,
    modifier: Modifier = Modifier,
    isSpeaking: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sphere")
    
    // Core pulse
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isBusy || isSpeaking) 1.2f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSpeaking) 800 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Dynamic rotation for energy rings
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    isSpeaking -> 2000
                    isBusy -> 4000
                    else -> 10000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Amplitude simulation for "speaking" effect
    val amplitude1 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = if (isSpeaking) 1.5f else 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSpeaking) 150 else 1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "amp1"
    )

    val amplitude2 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = if (isSpeaking) 1.3f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSpeaking) 250 else 1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "amp2"
    )

    Box(
        modifier = modifier
            .size(300.dp)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Glow
        Canvas(modifier = Modifier.fillMaxSize().blur(40.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (isSpeaking) UnaiGold.copy(alpha = 0.4f) else UnaiPurple.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension / 1.2f * pulseScale
                )
            )
        }

        // Energy Rings
        repeat(3) { index ->
            val ringScale = when(index) {
                0 -> amplitude1
                1 -> amplitude2
                else -> pulseScale
            }
            
            Canvas(modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = rotation * (index + 1) * (if (index % 2 == 0) 1 else -1)
                    scaleX = ringScale
                    scaleY = ringScale
                }
            ) {
                drawCircle(
                    color = when(index) {
                        0 -> UnaiGold.copy(alpha = 0.6f)
                        1 -> UnaiLightPurple.copy(alpha = 0.5f)
                        else -> UnaiPurple.copy(alpha = 0.4f)
                    },
                    radius = (size.minDimension / 2.2f),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(10f, 20f),
                            phase = rotation * 2
                        )
                    )
                )
            }
        }
        
        // Core Sphere
        Canvas(modifier = Modifier.size(180.dp * pulseScale)) {
            val radius = size.minDimension / 2
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (isSpeaking) UnaiGold else UnaiLightPurple,
                        if (isSpeaking) UnaiPurple else UnaiPurple
                    ),
                    center = Offset(size.width * 0.3f, size.height * 0.3f),
                    radius = radius * 1.5f
                )
            )
            
            // Inner highlights
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.5f), Color.Transparent),
                    center = Offset(size.width * 0.35f, size.height * 0.35f),
                    radius = radius * 0.6f
                )
            )
        }
    }
}

@Composable
fun UnaiSideMenu(viewModel: UnaiViewModel, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "U.N.A.I.",
            color = UnaiGold,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        MenuButton("NEW SESSION") {
            viewModel.clearSession()
            onClose()
        }
        MenuButton("SAVED CHATS") {
            viewModel.navigateTo(UnaiNavigation.SAVED_CHATS)
            onClose()
        }
        MenuButton("SAVED IMAGES") {
            viewModel.navigateTo(UnaiNavigation.SAVED_IMAGES)
            onClose()
        }
        MenuButton("MEMORY BANK") {
            viewModel.navigateTo(UnaiNavigation.MEMORY_BANK)
            onClose()
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Neural Routing (Tor)", color = Color.White, fontSize = 14.sp)
            val neuralRouting by viewModel.neuralRoutingEnabled
            Switch(
                checked = neuralRouting,
                onCheckedChange = { viewModel.setNeuralRouting(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = UnaiGold,
                    checkedTrackColor = UnaiGold.copy(alpha = 0.5f)
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        MenuButton("SETTINGS") {
            viewModel.navigateTo(UnaiNavigation.SETTINGS)
            onClose()
        }

        Spacer(modifier = Modifier.weight(1f))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, UnaiGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            color = Color.Transparent,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("UMBRA NEXUS CORE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.Green, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ONLINE", color = Color.Green, fontSize = 10.sp)
                    }
                }
                Text("All systems operational.", color = Color.Gray, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.checkSystemStatus() },
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, UnaiGold),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SYSTEM STATUS", color = UnaiGold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.adevar_logo),
                contentDescription = "Adevar Logo",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("A product of", color = Color.Gray, fontSize = 10.sp)
                Text("Adevar LLC", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MenuButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(50.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, UnaiGold),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text = text, color = Color.White, letterSpacing = 2.sp, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisionPlaceholderScreen(viewModel: UnaiViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SAVED IMAGES", color = UnaiGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(UnaiNavigation.CHAT) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = UnaiGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("Vision Core results pending. No imagery captured.", color = Color.Gray)
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, onRemember: (String) -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (message.isUser) UnaiGold.copy(alpha = 0.1f) else UnaiDarkPurple.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, UnaiGold.copy(alpha = 0.2f)),
            modifier = Modifier.clickable { if(!message.isUser) onRemember(message.text) }
        ) {
            Text(
                text = message.text,
                color = Color.White,
                modifier = Modifier.padding(12.dp),
                fontSize = 14.sp
            )
        }
    }
}
