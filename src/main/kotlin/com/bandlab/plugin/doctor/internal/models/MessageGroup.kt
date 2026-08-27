package com.bandlab.plugin.doctor.internal.models

import com.bandlab.plugin.doctor.ErrorLevel
import org.json.JSONArray
import org.json.JSONObject

/**
 * Contains a list of violation message and the error level of the message.
 *
 * This data that will be serialized to JSON on each modules and then deserialized on the root project.
 */
internal data class MessageGroup(
    val errorLevel: ErrorLevel,
    val messages: List<String>,
) {

    companion object {

        fun toJsonArray(messages: List<MessageGroup>): JSONArray {
            return JSONArray(messages.map { toJson(it) })
        }

        fun fromJsonArray(jsonArray: JSONArray): List<MessageGroup> {
            return jsonArray.toList().map { fromJson(JSONObject(it as Map<*, *>)) }
        }

        fun toJson(messageGroup: MessageGroup): JSONObject {
            val json = JSONObject()
            json.put("errorLevel", ErrorLevel.toJson(messageGroup.errorLevel))
            json.put("messages", JSONArray(messageGroup.messages))
            return json
        }

        fun fromJson(json: JSONObject): MessageGroup {
            val messages = json.getJSONArray("messages").toList().map { it.toString() }
            val errorObject = ErrorLevel.fromJson(json.getJSONObject("errorLevel"))
            return MessageGroup(errorObject, messages)
        }
    }
}