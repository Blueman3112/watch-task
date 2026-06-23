package com.example.test0512.mobile.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

class NsdServerManager(private val context: Context) {
    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var serviceName: String = "WatchTaskSync"

    companion object {
        const val SERVICE_TYPE = "_watchtasksync._tcp."
        private const val TAG = "NsdServerManager"
    }

    fun registerService(port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = this@NsdServerManager.serviceName
            serviceType = SERVICE_TYPE
            this.port = port
        }

        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                serviceName = NsdServiceInfo.serviceName
                Log.d(TAG, "Service registered: $serviceName on port $port")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service registration failed: $errorCode")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                Log.d(TAG, "Service unregistered: ${arg0.serviceName}")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service unregistration failed: $errorCode")
            }
        }

        nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun tearDown() {
        try {
            registrationListener?.let { nsdManager?.unregisterService(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error during teardown", e)
        }
        registrationListener = null
    }
}
