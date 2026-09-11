package egehanvx.portscanner

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.NoRouteToHostException
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class PortScanner {

    fun scanPort(
        host: String,
        port: Int,
        timeout: Int = 1000
    ): ScanResult {

        return try {
            Socket().use { socket ->

                socket.connect(
                    InetSocketAddress(host, port),
                    timeout
                )

                ScanResult(
                    port = port,
                    isOpen = true,
                    service = getServiceName(port)
                )
            }

        } catch (e: CancellationException) {
            throw e

        } catch (e: SocketTimeoutException) {

            ScanResult(
                port = port,
                isOpen = false,
                service = getServiceName(port)
            )

        } catch (e: ConnectException) {

            ScanResult(
                port = port,
                isOpen = false,
                service = getServiceName(port)
            )

        } catch (e: NoRouteToHostException) {

            ScanResult(
                port = port,
                isOpen = false,
                service = getServiceName(port)
            )

        } catch (e: UnknownHostException) {

            throw e

        } catch (e: Exception) {

            ScanResult(
                port = port,
                isOpen = false,
                service = getServiceName(port)
            )
        }
    }

    suspend fun scanRange(
        host: String,
        startPort: Int,
        endPort: Int,
        timeout: Int = 1000,
        maxConcurrent: Int = 20,
        onResult: (ScanResult, Int, Int) -> Unit
    ): List<ScanResult> = coroutineScope {

        val results = mutableListOf<ScanResult>()
        val semaphore = Semaphore(maxConcurrent)

        var scannedCount = 0

        val jobs = (startPort..endPort).map { port ->

            async(Dispatchers.IO) {

                currentCoroutineContext().ensureActive()

                semaphore.withPermit {

                    currentCoroutineContext().ensureActive()

                    val result = scanPort(
                        host = host,
                        port = port,
                        timeout = timeout
                    )

                    currentCoroutineContext().ensureActive()

                    synchronized(results) {
                        results.add(result)
                    }

                    synchronized(this@coroutineScope) {
                        scannedCount++

                        onResult(
                            result,
                            scannedCount,
                            endPort - startPort + 1
                        )
                    }

                    result
                }
            }
        }

        try {

            jobs.awaitAll()

            results.sortedBy {
                it.port
            }

        } catch (e: CancellationException) {

            jobs.forEach {
                it.cancel()
            }

            throw e
        }
    }

    private fun getServiceName(port: Int): String? {
        return when (port) {
            21 -> "FTP"
            22 -> "SSH"
            23 -> "Telnet"
            25 -> "SMTP"
            53 -> "DNS"
            80 -> "HTTP"
            110 -> "POP3"
            143 -> "IMAP"
            443 -> "HTTPS"
            3306 -> "MySQL"
            5432 -> "PostgreSQL"
            8080 -> "HTTP/Alt"
            else -> null
        }
    }
}