package com.bandlab.plugin.doctor.internal.models

import com.bandlab.plugin.doctor.ErrorLevel
import org.json.JSONObject

internal fun ErrorLevel.Companion.toJson(level: ErrorLevel): JSONObject {
    val errorLevel = when (level) {
        is ErrorLevel.Error -> "Error"
        is ErrorLevel.Warning -> "Warning"
    }

    return JSONObject().apply {
        put("level", errorLevel)
        put("msg", level.msg)
    }
}

internal fun ErrorLevel.Companion.fromJson(json: JSONObject): ErrorLevel {
    val errorLevel = json.getString("level")
    val msg = json.optString("msg")

    return when (errorLevel) {
        "Error" -> ErrorLevel.Error(msg)
        "Warning" -> ErrorLevel.Warning(msg)
        else -> throw IllegalArgumentException("Invalid error level: $errorLevel")
    }
}