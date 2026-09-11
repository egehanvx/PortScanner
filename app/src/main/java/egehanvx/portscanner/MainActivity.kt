package egehanvx.portscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import egehanvx.portscanner.ui.theme.PortScannerTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PortScannerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    PortScannerScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun PortScannerScreen(
    modifier: Modifier = Modifier
) {
    var target by remember {
        mutableStateOf("")
    }

    var startPort by remember {
        mutableStateOf("")
    }

    var endPort by remember {
        mutableStateOf("")
    }

    var results by remember {
        mutableStateOf<List<ScanResult>>(emptyList())
    }

    var isScanning by remember {
        mutableStateOf(false)
    }

    var wasCancelled by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var scannedCount by remember {
        mutableStateOf(0)
    }

    var totalPorts by remember {
        mutableStateOf(0)
    }

    var scanDuration by remember {
        mutableStateOf<Long?>(null)
    }

    var lastTarget by remember {
        mutableStateOf("")
    }

    val scope = rememberCoroutineScope()

    var scanJob by remember {
        mutableStateOf<Job?>(null)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Top
    ) {

        Text(
            text = "Port Scanner"
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        OutlinedTextField(
            value = target,
            onValueChange = {
                target = it
                errorMessage = ""
                wasCancelled = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Hedef IP / Hostname")
            },
            placeholder = {
                Text("Örn: 192.168.1.1")
            },
            singleLine = true,
            enabled = !isScanning
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        OutlinedTextField(
            value = startPort,
            onValueChange = {
                startPort = it
                errorMessage = ""
                wasCancelled = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Başlangıç portu")
            },
            placeholder = {
                Text("Örn: 1")
            },
            singleLine = true,
            enabled = !isScanning
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        OutlinedTextField(
            value = endPort,
            onValueChange = {
                endPort = it
                errorMessage = ""
                wasCancelled = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Bitiş portu")
            },
            placeholder = {
                Text("Örn: 1024")
            },
            singleLine = true,
            enabled = !isScanning
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Button(
            onClick = {

                val cleanTarget = target.trim()
                val start = startPort.toIntOrNull()
                val end = endPort.toIntOrNull()

                errorMessage = ""
                wasCancelled = false
                results = emptyList()
                scannedCount = 0
                totalPorts = 0
                scanDuration = null
                lastTarget = ""

                if (cleanTarget.isEmpty()) {
                    errorMessage =
                        "Lütfen bir hedef IP veya hostname girin."
                    return@Button
                }

                if (start == null || end == null) {
                    errorMessage =
                        "Port numaraları sayı olmalıdır."
                    return@Button
                }

                if (start !in 1..65535 || end !in 1..65535) {
                    errorMessage =
                        "Portlar 1 ile 65535 arasında olmalıdır."
                    return@Button
                }

                if (start > end) {
                    errorMessage =
                        "Başlangıç portu bitiş portundan büyük olamaz."
                    return@Button
                }

                totalPorts = end - start + 1
                lastTarget = cleanTarget
                isScanning = true

                scanJob = scope.launch {

                    val startTime = System.currentTimeMillis()

                    try {

                        PortScanner().scanRange(
                            host = cleanTarget,
                            startPort = start,
                            endPort = end,

                            onResult = { result, scanned, total ->

                                scannedCount = scanned
                                totalPorts = total

                                if (result.isOpen) {
                                    results = results + result
                                }
                            }
                        )

                        scanDuration =
                            System.currentTimeMillis() - startTime

                        isScanning = false
                        scanJob = null

                    } catch (e: CancellationException) {

                        scanDuration =
                            System.currentTimeMillis() - startTime

                        wasCancelled = true
                        isScanning = false
                        scanJob = null

                    } catch (e: UnknownHostException) {

                        errorMessage =
                            "Hedef bulunamadı. IP adresini veya hostname'i kontrol edin."

                        isScanning = false
                        scanJob = null

                    } catch (e: NoRouteToHostException) {

                        errorMessage =
                            "Hedefe ulaşılamıyor. Ağ bağlantısını ve hedefi kontrol edin."

                        isScanning = false
                        scanJob = null

                    } catch (e: ConnectException) {

                        errorMessage =
                            "Hedefe TCP bağlantısı kurulamadı."

                        isScanning = false
                        scanJob = null

                    } catch (e: Exception) {

                        errorMessage =
                            "Tarama sırasında beklenmeyen bir hata oluştu."

                        isScanning = false
                        scanJob = null
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isScanning
        ) {
            Text("Taramayı Başlat")
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Button(
            onClick = {
                scanJob?.cancel()
                scanJob = null
                isScanning = false
                wasCancelled = true
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isScanning
        ) {
            Text("Taramayı Durdur")
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        if (errorMessage.isNotEmpty()) {

            Text(
                text = errorMessage
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }

        if (wasCancelled) {

            Text(
                text = "Tarama kullanıcı tarafından durduruldu."
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }

        if (isScanning) {

            Text(
                text = "Tarama devam ediyor..."
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            if (totalPorts > 0) {

                val progress =
                    scannedCount.toFloat() / totalPorts.toFloat()

                LinearProgressIndicator(
                    progress = {
                        progress.coerceIn(0f, 1f)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "$scannedCount / $totalPorts"
                )

                Text(
                    text = "%${(progress * 100).toInt()}"
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )
        }

        if (!isScanning && lastTarget.isNotEmpty() && totalPorts > 0) {

            Text(
                text = "Tarama Özeti"
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Hedef: $lastTarget"
            )

            Text(
                text = "Taranan port: $scannedCount / $totalPorts"
            )

            Text(
                text = "Açık port: ${results.size}"
            )

            scanDuration?.let { duration ->

                Text(
                    text = "Süre: ${formatDuration(duration)}"
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )
        }

        if (results.isNotEmpty()) {

            Text(
                text = "Açık Portlar:"
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {

                items(results.sortedBy { it.port }) { result ->

                    val serviceText =
                        result.service ?: "Bilinmiyor"

                    Text(
                        text =
                            "${result.port} - OPEN - Muhtemel servis: $serviceText",
                        modifier = Modifier.padding(
                            vertical = 4.dp
                        )
                    )
                }
            }
        }
    }
}

fun formatDuration(milliseconds: Long): String {

    if (milliseconds < 1000) {
        return "${milliseconds} ms"
    }

    val seconds = milliseconds / 1000
    val remainingMilliseconds = milliseconds % 1000

    return "${seconds}.${remainingMilliseconds / 100} sn"
}

