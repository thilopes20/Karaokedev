package com.karaokedev.app

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.karaokedev.app.audio.KaraokeEngine
import com.karaokedev.app.audio.PRESETS
import com.karaokedev.app.lyrics.LrcParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                KaraokeScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaraokeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasMicPermission = granted }

    var presetIndex by remember { mutableIntStateOf(0) }
    var musicUri by remember { mutableStateOf<Uri?>(null) }
    var lyrics by remember { mutableStateOf<List<LrcParser.LyricLine>>(emptyList()) }
    var singing by remember { mutableStateOf(false) }
    var currentLine by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Escolha uma música e uma letra .lrc") }

    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var engine by remember { mutableStateOf<KaraokeEngine?>(null) }

    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            musicUri = uri
            status = "Música selecionada"
        }
    }
    val lyricsPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val text = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.use { it.readText() }
            lyrics = LrcParser.parse(text ?: "")
            status = if (lyrics.isEmpty()) "Letra vazia ou formato inválido"
            else "Letra carregada (${lyrics.size} linhas)"
        }
    }

    fun stopSinging() {
        singing = false
        player?.stop()
        player?.release()
        player = null
        val file = engine?.stop()
        engine = null
        currentLine = null
        status = if (file != null) "Gravação salva: ${file.name}" else "Parado"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("KaraokeDev", style = MaterialTheme.typography.headlineMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PRESETS.forEachIndexed { index, preset ->
                FilterChip(
                    selected = presetIndex == index,
                    onClick = { presetIndex = index },
                    label = { Text(preset.name) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { musicPicker.launch("audio/*") }) { Text("Música") }
            Button(onClick = { lyricsPicker.launch("*/*") }) { Text("Letra .lrc") }
        }

        when {
            !hasMicPermission -> Button(
                onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            ) { Text("Permitir microfone") }

            singing -> Button(onClick = { stopSinging() }) { Text("Parar") }

            else -> Button(
                enabled = musicUri != null,
                onClick = {
                    val uri = musicUri ?: return@Button
                    val preset = PRESETS[presetIndex]
                    val eng = KaraokeEngine(context, preset)
                    if (!eng.start()) {
                        status = "Falha ao iniciar o microfone"
                        return@Button
                    }
                    engine = eng
                    val mp = MediaPlayer()
                    try {
                        mp.setDataSource(context, uri)
                        mp.prepare()
                        mp.start()
                        player = mp
                        singing = true
                        status = "Cantando com preset ${preset.name}"
                        scope.launch {
                            while (singing) {
                                delay(100)
                                val playing = runCatching { mp.isPlaying }.getOrDefault(false)
                                if (!playing) break
                                currentLine = LrcParser.currentLine(
                                    lyrics, mp.currentPosition.toLong()
                                )
                            }
                        }
                    } catch (e: Exception) {
                        status = "Erro ao tocar música: ${e.message}"
                        eng.stop()
                        engine = null
                    }
                }
            ) { Text("Cantar") }
        }

        Box(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentLine ?: "…",
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
        }

        Text(status, style = MaterialTheme.typography.bodySmall)
    }
}
