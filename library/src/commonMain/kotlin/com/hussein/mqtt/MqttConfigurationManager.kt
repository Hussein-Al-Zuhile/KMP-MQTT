package com.hussein.mqtt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

abstract class MqttConfigurationManager<Client : BaseMQTTClient>(
    ipFlow: Flow<String>,
    portFlow: Flow<Int>,
    private val initiator: (ip: String, port: Int) -> Client,
) {

    private val _currentClient: MutableStateFlow<Client?> = MutableStateFlow(null)
    val currentClientStateFlow: StateFlow<Client?> = _currentClient.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            combine(ipFlow, portFlow) { ip, port -> ip to port }
                .collect { (ip, port) ->
                    restartClient(ip, port)
                }
        }
    }

    private fun restartClient(ip: String, port: Int) {
        scope.launch {
            val oldClient = _currentClient.value
            oldClient?.disconnect()
            val newClient = initiator(ip, port)
            newClient.connectOrTryReconnect()
            _currentClient.value = newClient
        }
    }

}
