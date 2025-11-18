# KMP-MQTT

A library for MQTT easy plug and play usage, based on ktor-mqtt library. This library provides a simple, Kotlin Multiplatform-ready client for interacting with MQTT brokers.

## Installation

Add the dependency to your `build.gradle.kts` file:

```kotlin
implementation("io.github.hussein-al-zuhile:kmp-mqtt:1.0.0")
```

## Usage

### 1. Create your MQTT Client

First, you need to create a class that inherits from `BaseMQTTClient`.

```kotlin
import com.hussein.mqtt.BaseMQTTClient

class MyMQTTClient(
    ip: String,
    port: Int,
    clientIdentifier: String = "my-unique-client-id"
) : BaseMQTTClient(ip, port, clientIdentifier) {
    // You can add custom logic here if needed
}
```

### 2. Define Topics and Message Models

Define your topics and the data models for the messages. The library uses Kotlinx Serialization for JSON conversion.

```kotlin
import com.hussein.mqtt.MQTTTopic
import kotlinx.serialization.Serializable

@Serializable
data class MyMessage(val content: String)

val myTopic = MQTTTopic<MyMessage>("my/awesome/topic")
```

### 3. Connect to the Broker

Instantiate your client and connect to the MQTT broker.

```kotlin
val client = MyMQTTClient("broker.emqx.io", 1883)

// To connect once
val connectionResult = client.connect()

// Or, to automatically handle reconnects
client.connectOrTryReconnect()
```

### 4. Subscribe and Listen to Topics

You can easily subscribe to a topic and get a `Flow` of messages. The library will handle JSON decoding for you.

```kotlin
import com.hussein.mqtt.utils.subscribeAndListenWhenConnected
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

// ... inside a CoroutineScope

client.subscribeAndListenWhenConnected(myTopic)
    .onEach { message: MyMessage ->
        println("Received message: ${message.content}")
    }
    .launchIn(CoroutineScope(Dispatchers.Default))
```

### 5. Publish Messages

Publishing a message is just as simple. The library handles the JSON encoding.

```kotlin
import de.kempmobil.ktor.mqtt.QoS

// ... inside a suspend function or CoroutineScope

val messageToSend = MyMessage(content = "Hello, MQTT!")

client.publish(
    topic = myTopic,
    payload = messageToSend,
    desiredQoS = QoS.AT_LEAST_ONCE
)
```

### 6. Disconnect

To disconnect from the broker:

```kotlin
client.disconnect()
```

### 7. Dynamic Configuration with `MqttConfigurationManager`

If you need to dynamically change the MQTT broker's IP address or port, you can use `MqttConfigurationManager`. This is useful when the connection details are not known at compile time or can change during the application's lifecycle.

First, create a class that inherits from `MqttConfigurationManager`:

```kotlin
import com.hussein.mqtt.MqttConfigurationManager
import kotlinx.coroutines.flow.Flow

class MyMqttConfigManager(
    ipFlow: Flow<String>,
    portFlow: Flow<Int>
) : MqttConfigurationManager<MyMQTTClient>(
    ipFlow = ipFlow,
    portFlow = portFlow,
    initiator = { ip, port -> MyMQTTClient(ip, port) }
) 
```

Then, you can use it like this:

```kotlin
import kotlinx.coroutines.flow.MutableStateFlow

// Create flows for your dynamic configuration
val ipFlow = MutableStateFlow("broker.emqx.io")
val portFlow = MutableStateFlow(1883)

// Initialize the manager
val configManager = MyMqttConfigManager(ipFlow, portFlow)

// You can then collect the client and use it
configManager.currentClientStateFlow.collect { client ->
    if (client != null) {
        // You can now use the client to subscribe, publish, etc.
    }
}

// To change the configuration, just emit a new value to the flows
ipFlow.value = "new.broker.address"
portFlow.value = 8883
```
