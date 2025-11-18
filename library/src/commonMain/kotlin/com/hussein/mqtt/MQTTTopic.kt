package com.hussein.mqtt

import de.kempmobil.ktor.mqtt.Topic
import de.kempmobil.ktor.mqtt.util.toTopic

interface MQTTTopic<Payload> {
    val topicString: String
}

val MQTTTopic<*>.topicValue: Topic
    get() = topicString.toTopic()

infix fun MQTTTopic<*>.isMatch(topic: Topic): Boolean =
    topicString.isMatch(topic.name)

infix fun MQTTTopic<*>.isMatch(topic: MQTTTopic<*>) =
    topicString.isMatch(topic.topicString)

infix fun Topic.isMatch(otherTopic: Topic): Boolean {
    return name.isMatch(otherTopic.name)
}

private fun String.isMatch(otherTopicString: String): Boolean {
    val levels = split("/")
    val otherLevels = otherTopicString.split("/")

    for (i in levels.indices) {
        val currentLevel = levels[i]

        // '#' wildcard — matches everything after this point
        if (currentLevel == "#") return true

        // topic shorter than filter → no match
        if (i >= otherLevels.size) return false

        val otherLevel = otherLevels[i]

        // '+' wildcard — matches any single level
        if (currentLevel == "+") continue

        // exact match required otherwise
        if (currentLevel != otherLevel) return false
    }

    // must match exactly in length unless filter ends with '#'
    return otherLevels.size == levels.size
}
