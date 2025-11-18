package com.hussein.mqtt.utils

import com.hussein.mqtt.BaseMQTTClient
import com.hussein.mqtt.BaseMQTTClient.Companion.decodeFromJsonString
import com.hussein.mqtt.MQTTTopic
import com.hussein.mqtt.isMatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.io.bytestring.decodeToString

inline fun <reified MessageModel> BaseMQTTClient.subscribeAndListenWhenConnected(
    topic: MQTTTopic<MessageModel>,
    unsubscribeOnCompletion: Boolean = false,
): Flow<MessageModel> = channelFlow<MessageModel> {

    connectionStateFlow.collect { state ->
        if (state.isConnected) {

            subscribe(topic)

            messagesSharedFlow
                .filter { topic isMatch it.topic }
                .collect {
                    send(
                        it.payload.decodeToString().decodeFromJsonString()
                    )
                }
        }
    }
}.onCompletion {
    if (unsubscribeOnCompletion) {
        CoroutineScope(Dispatchers.IO).launch {
            unsubscribe(topic)
        }
    }
}
