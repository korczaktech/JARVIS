package com.korczak.morok
import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.korczak.morok.core.CommandResult
import com.korczak.morok.core.CommandRouter
import com.korczak.morok.core.CommandSource
import com.korczak.morok.service.MorokForegroundService
class MainActivity : ComponentActivity() {
    private val router = CommandRouter()
    private var status by mutableStateOf("Morok pronto.")
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        status = if (results.values.all { it }) "Permissões concedidas." else "Algumas permissões não foram concedidas."
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBasePermissions()
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("MOROK", style = MaterialTheme.typography.headlineLarge)
                        Text(status)
                        Button(onClick = {
                            status = when (val result = router.route("status", CommandSource.BUTTON)) {
                                is CommandResult.Success -> result.message
                                is CommandResult.RequiresConfirmation -> "Confirme a ação."
                                is CommandResult.NeedsPermission -> "Permissão necessária: ${result.permission}"
                                is CommandResult.Failure -> result.message
                            }
                        }) { Text("Testar núcleo") }
                        OutlinedButton(onClick = { startAssistantService() }) { Text("Iniciar serviço") }
                    }
                }
            }
        }
    }
    private fun requestBasePermissions() {
        val permissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }
    private fun startAssistantService() {
        ContextCompat.startForegroundService(this, Intent(this, MorokForegroundService::class.java))
        status = "Serviço do Morok iniciado."
    }
}
