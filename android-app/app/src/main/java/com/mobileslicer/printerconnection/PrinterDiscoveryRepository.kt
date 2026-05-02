package com.mobileslicer.printerconnection

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

@Suppress("DEPRECATION")
internal class PrinterDiscoveryRepository(
    context: Context
) {
    private val appContext = context.applicationContext
    private val nsdManager = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager

    suspend fun discoverPrinters(timeoutMs: Long = 7_000L): PrinterConnectionChoicesResult = withContext(Dispatchers.IO) {
        val serviceTypes = listOf(
            "_octoprint._tcp." to "OctoPrint",
            "_moonraker._tcp." to "Moonraker",
            "_prusalink._tcp." to "PrusaLink",
            "_bambu._tcp." to "Bambu LAN",
            "_bambulab._tcp." to "Bambu LAN",
            "_bblp._tcp." to "Bambu LAN"
        )
        val choices = linkedMapOf<String, PrinterConnectionChoice>()
        val mdnsTimeoutMs = minOf(4_000L, (timeoutMs * 4L) / 7L)
        for ((serviceType, label) in serviceTypes) {
            discoverServiceType(serviceType, label, mdnsTimeoutMs / serviceTypes.size.coerceAtLeast(1))
                .forEach { choice -> choices.putIfAbsent(choice.value, choice) }
        }
        scanLocalPrinterApis((timeoutMs - mdnsTimeoutMs).coerceAtLeast(2_500L))
            .forEach { choice -> choices.putIfAbsent(choice.value, choice) }
        if (choices.isEmpty()) {
            PrinterConnectionChoicesResult(
                false,
                "No printers found",
                "No printer mDNS services or known local printer APIs were found on this network. You can still enter the printer IP manually."
            )
        } else {
            PrinterConnectionChoicesResult(
                true,
                "Select discovered printer",
                "Choose a discovered local printer host.",
                choices.values.toList()
            )
        }
    }

    private suspend fun discoverServiceType(
        serviceType: String,
        label: String,
        timeoutMs: Long
    ): List<PrinterConnectionChoice> = suspendCancellableCoroutine { continuation ->
        val results = linkedMapOf<String, PrinterConnectionChoice>()
        var stopped = false

        fun finish(listener: NsdManager.DiscoveryListener) {
            if (stopped) return
            stopped = true
            runCatching { nsdManager.stopServiceDiscovery(listener) }
            continuation.resume(results.values.toList())
        }

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) = Unit

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != serviceType) return
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        val hostAddress = serviceInfo.hostAddress() ?: return
                        val port = serviceInfo.port.takeIf { it > 0 }
                        val scheme = if (port == 443 || port == 8883 || port == 990) "https" else "http"
                        val value = "$scheme://$hostAddress${port?.let { ":$it" }.orEmpty()}"
                        val detail = buildList {
                            add(label)
                            add(serviceInfo.serviceType.trim('.'))
                            serviceInfo.attributeString("dev_id")?.let { add("Device ID: $it") }
                            serviceInfo.attributeString("serial")?.let { add("Serial: $it") }
                            serviceInfo.attributeString("sn")?.let { add("Serial: $it") }
                        }.joinToString(" • ")
                        results[value] = PrinterConnectionChoice(
                            label = serviceInfo.serviceName.ifBlank { "$label $hostAddress" },
                            value = value,
                            detail = detail
                        )
                    }
                })
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) = finish(this)
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        }

        continuation.invokeOnCancellation {
            if (!stopped) {
                stopped = true
                runCatching { nsdManager.stopServiceDiscovery(listener) }
            }
        }
        runCatching {
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
        }.onFailure {
            continuation.resume(emptyList())
            return@suspendCancellableCoroutine
        }
        Thread {
            Thread.sleep(timeoutMs.coerceAtLeast(500L))
            finish(listener)
        }.start()
    }

    private fun NsdServiceInfo.hostAddress(): String? =
        runCatching {
            val hostValue = host ?: return@runCatching null
            hostValue.hostAddress
        }.getOrNull()?.trim('[', ']')?.takeIf { it.isNotBlank() }

    private fun NsdServiceInfo.attributeString(name: String): String? =
        runCatching {
            attributes[name]?.toString(Charsets.UTF_8)
        }.getOrNull()?.takeIf { it.isNotBlank() }

    private suspend fun scanLocalPrinterApis(timeoutMs: Long): List<PrinterConnectionChoice> =
        withTimeoutOrNull(timeoutMs) {
            coroutineScope {
                val subnets = localIpv4SubnetPrefixes().take(2)
                if (subnets.isEmpty()) return@coroutineScope emptyList()
                val ports = listOf(10088, 7125, 80, 5000)
                val semaphore = Semaphore(48)
                val results = linkedMapOf<String, PrinterConnectionChoice>()
                val probes = subnets.flatMap { subnet ->
                    (1..254).flatMap { host ->
                        val address = "$subnet.$host"
                        ports.map { port ->
                            async(Dispatchers.IO) {
                                semaphore.withPermit {
                                    probePrinterApi(address, port)
                                }
                            }
                        }
                    }
                }
                probes.awaitAll().filterNotNull().forEach { choice ->
                    results.putIfAbsent(choice.value, choice)
                }
                results.values.toList()
            }
        }.orEmpty()

    private fun localIpv4SubnetPrefixes(): List<String> =
        runCatching {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .filter { it.isUp && !it.isLoopback && !it.isVirtual }
                .flatMap { networkInterface ->
                    networkInterface.interfaceAddresses.asSequence()
                        .mapNotNull { address ->
                            val inetAddress = address.address as? Inet4Address ?: return@mapNotNull null
                            if (inetAddress.isLoopbackAddress || !inetAddress.isSiteLocalAddress) return@mapNotNull null
                            val octets = inetAddress.hostAddress.orEmpty().split('.')
                            if (octets.size != 4) return@mapNotNull null
                            "${octets[0]}.${octets[1]}.${octets[2]}"
                        }
                }
                .distinct()
                .toList()
        }.getOrDefault(emptyList())

    private fun probePrinterApi(host: String, port: Int): PrinterConnectionChoice? {
        val baseUrl = "http://$host:$port"
        moonrakerProbe(baseUrl)?.let { return it }
        octoPrintProbe(baseUrl)?.let { return it }
        prusaLinkProbe(baseUrl)?.let { return it }
        return null
    }

    private fun moonrakerProbe(baseUrl: String): PrinterConnectionChoice? {
        val body = requestProbe("$baseUrl/server/info") ?: return null
        if (!body.contains("moonraker", ignoreCase = true) && !body.contains("klippy", ignoreCase = true)) return null
        return PrinterConnectionChoice(
            label = "Moonraker ${baseUrl.substringAfter("://")}",
            value = baseUrl,
            detail = "Moonraker • subnet scan"
        )
    }

    private fun octoPrintProbe(baseUrl: String): PrinterConnectionChoice? {
        val body = requestProbe("$baseUrl/api/version") ?: return null
        if (!body.contains("OctoPrint", ignoreCase = true)) return null
        return PrinterConnectionChoice(
            label = "OctoPrint ${baseUrl.substringAfter("://")}",
            value = baseUrl,
            detail = "OctoPrint • subnet scan"
        )
    }

    private fun prusaLinkProbe(baseUrl: String): PrinterConnectionChoice? {
        val body = requestProbe("$baseUrl/api/version") ?: return null
        if (!body.contains("Prusa", ignoreCase = true) && !body.contains("PrusaLink", ignoreCase = true)) return null
        return PrinterConnectionChoice(
            label = "PrusaLink ${baseUrl.substringAfter("://")}",
            value = baseUrl,
            detail = "PrusaLink • subnet scan"
        )
    }

    private fun requestProbe(url: String): String? =
        runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 350
                readTimeout = 550
                instanceFollowRedirects = false
                setRequestProperty("Accept", "application/json,text/plain,*/*")
            }
            try {
                val code = connection.responseCode
                if (code !in 200..299) return@runCatching null
                connection.inputStream.bufferedReader().use { it.readText().take(2048) }
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
}
