package com.hussein.mqtt

import de.kempmobil.ktor.mqtt.Disconnected
import de.kempmobil.ktor.mqtt.MqttClient
import de.kempmobil.ktor.mqtt.PublishRequest
import de.kempmobil.ktor.mqtt.QoS
import de.kempmobil.ktor.mqtt.SubscriptionOptions
import de.kempmobil.ktor.mqtt.TopicFilter
import de.kempmobil.ktor.mqtt.packet.Connack
import de.kempmobil.ktor.mqtt.packet.Suback
import de.kempmobil.ktor.mqtt.packet.Unsuback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

abstract class BaseMQTTClient(
    val ip: String,
    val port: Int,
    clientIdentifier: String = Random.nextInt().toString(),
) {

    open val client = MqttClient(ip, port) {
        clientId = clientIdentifier
    }

    val messagesSharedFlow = client.publishedPackets

    val connectionStateFlow = client.connectionState.stateIn(
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
        started = SharingStarted.Eagerly,
        initialValue = Disconnected
    )

    private val _subscribedTopics: MutableStateFlow<List<MQTTTopic<*>>> = MutableStateFlow(emptyList())
    val subscribedTopics = _subscribedTopics.asStateFlow()


    suspend fun connect(): Result<Connack> = client.connect().onSuccess { connack ->
        logger.debug { "Connected to MQTT broker $ip:$port with $connack" }
    }.onFailure {
        logger.debug { "Failed to connect to MQTT broker $ip:$port" }
    }

    fun connectOrTryReconnect(
        initialDelay: Duration = 1.seconds,
        maxDelay: Duration = 30.seconds,
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    ) {
        scope.launch {
            connectionStateFlow.collect { connectionState ->
                var currentDelay = initialDelay

                while (!connectionState.isConnected) {
                    connect().onSuccess {
                        ensureActive()
                        break
                    }.onFailure {
                        ensureActive()
                        currentDelay = (currentDelay * 2).coerceAtMost(maxDelay)
                    }
                    delay(currentDelay)
                }
            }
        }
    }


    suspend fun disconnect(): Unit {
        client.disconnect()
    }

    suspend fun subscribe(topic: MQTTTopic<*>): Result<Suback> =
        client.subscribe(listOf(TopicFilter(topic.topicValue, SubscriptionOptions.DEFAULT)))
            .onSuccess {
                logger.debug { "Subscribed to ${topic.topicString}" }
                _subscribedTopics.value += topic
            }
            .onFailure {
                logger.debug(it) { "Failed to subscribe to ${topic.topicString}" }
            }

    suspend fun subscribeTopics(topics: List<MQTTTopic<*>>): List<Result<Suback>> = coroutineScope {
        topics.map { topic ->
            async {
                subscribe(topic)
            }
        }.awaitAll()
    }

    suspend fun unsubscribe(topic: MQTTTopic<*>): Result<Unsuback> =
        client.unsubscribe(listOf(topic.topicValue))
            .apply {
                onSuccess {
                    logger.debug { "Unsubscribed from ${topic.topicString}" }
                    _subscribedTopics.value -= topic
                }
            }

    suspend inline fun <reified PayloadModel> publish(
        topic: MQTTTopic<PayloadModel>,
        payload: PayloadModel,
        desiredQoS: QoS = QoS.AT_LEAST_ONCE,
        isRetainMessage: Boolean = false
    ) =
        client.publish(
            PublishRequest(
                topic = topic.topicValue,
                payload = payload.encodeToJsonString().encodeToByteString(),
                desiredQoS = desiredQoS,
                isRetainMessage = isRetainMessage,
            )
        )

    companion object {
        inline fun <reified T> T.encodeToJsonString() = jsonConverter.encodeToString<T>(this)

        inline fun <reified T> String.decodeFromJsonString() = jsonConverter.decodeFromString<T>(this)

        val jsonConverter = Json {
            explicitNulls = false
            isLenient = true
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }


}
