package com.blink.monitor

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blink.monitor.databinding.ActivityWifiBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class WifiActivity: AppCompatActivity() {

    lateinit var binding: ActivityWifiBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWifiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        binding.tvSsid.text = getSSID()
    }

    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // 权限已授予
                binding.tvSsid.text = getSSID()
                connectWifi2(getSSID(), "li123456@")
            } else {
                // 权限被拒绝
                Toast.makeText(this, "权限被拒绝", Toast.LENGTH_SHORT).show()
            }
        }
    /**
     * 获取当前连接WIFI的SSID
     */
    fun getSSID(): String {
        val wm = getSystemService(WIFI_SERVICE) as WifiManager
        val wInfo = wm.connectionInfo
        if (wInfo != null) {
            val s = wInfo.ssid
            if (s.length > 2 && s[0] == '"' && s[s.length - 1] == '"') {
                return s.substring(1, s.length - 1)
            }
        }
        return ""
    }


    @RequiresApi(Build.VERSION_CODES.S)
    fun connectWifi2(ssid: String, password: String) {
        val suggestion =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                WifiNetworkSuggestion.Builder()
                    .setSsid(ssid)
                    .setIsAppInteractionRequired(true) // 提示用户连接
                    .setWpa2Passphrase(password)
                    .build()
            } else {
                TODO("VERSION.SDK_INT < Q")
            }

        val suggestions: MutableList<WifiNetworkSuggestion> = ArrayList()
        suggestions.add(suggestion)

        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val state =  wifiManager.addNetworkSuggestions(suggestions)
        Log.d("wifi", "state0:$state")
        val wifiNetworkSpecifier =  WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()

        val request =  NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(wifiNetworkSpecifier)
            .build();

        val mConnectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        mConnectivityManager.requestNetwork(request, object: ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d("wifi", "state2: onAvailable")
            }
        })



        lifecycleScope.launch {
            delay(10_000)
            Log.d("wifi", "state1:${isWiFiConnected(this@WifiActivity)}")
        }

    }

    fun isWiFiConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0+ 使用 NetworkCapabilities 检查网络状态
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork != null) {
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                return capabilities != null &&
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            }
        } else {
            // Android < 6.0 使用 NetworkInfo 检查网络状态
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null &&
                    networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI
        }
        return false
    }

    private fun connectToWiFi(ssid: String, password: String) {
        val wifiManager = getSystemService(WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) {
            wifiManager.isWifiEnabled = true
        }

        val wifiConfig = createWifiConfig(ssid, password)

        val netId = wifiManager.addNetwork(wifiConfig)
        if (netId != -1) {
            wifiManager.disconnect()
            val isConnected = wifiManager.enableNetwork(netId, true)
            wifiManager.reconnect()

            if (isConnected) {
                Log.d("WifiConnectActivity", "Successfully connected to $ssid")
            } else {
                Log.e("WifiConnectActivity", "Failed to connect to $ssid")
            }
        } else {
            Log.e("WifiConnectActivity", "Failed to add network configuration for $ssid")
        }
    }

    private fun createWifiConfig(ssid: String, password: String): WifiConfiguration {
        val config = WifiConfiguration().apply {
            this.SSID = String.format("\"%s\"", ssid)
            this.preSharedKey = String.format("\"%s\"", password)
            this.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
            this.hiddenSSID = true;
            this.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            this.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            this.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            this.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            this.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            this.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            this.status = WifiConfiguration.Status.ENABLED;

        }
        return config
    }

}