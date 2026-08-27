package com.bandlab.plugin.doctor.internal.models

import com.bandlab.plugin.doctor.ErrorLevel
import com.google.common.truth.Truth.assertThat
import org.json.JSONArray
import org.junit.Test

class MessageGroupTest {

    @Test
    fun `toJson and fromJson roundtrip preserves errorLevel and messages`() {
        // given
        val original = MessageGroup(
            errorLevel = ErrorLevel.Error("boom"),
            messages = listOf("msg1", "msg2"),
        )

        // when
        val json = MessageGroup.toJson(original)
        val restored = MessageGroup.fromJson(json)

        // then
        assertThat(restored.errorLevel).isEqualTo(original.errorLevel)
        assertThat(restored.messages).isEqualTo(original.messages)
    }

    @Test
    fun `toJsonArray and fromJsonArray roundtrip preserves list`() {
        // given
        val groups = listOf(
            MessageGroup(ErrorLevel.Error("e"), listOf("e1")),
            MessageGroup(ErrorLevel.Warning("w"), listOf("w1", "w2")),
        )

        // when
        val array = MessageGroup.toJsonArray(groups)
        val restored = MessageGroup.fromJsonArray(array)

        // then
        assertThat(restored).isEqualTo(groups)
    }

    @Test
    fun `toJsonArray creates array with correct size`() {
        // given
        val groups = listOf(
            MessageGroup(ErrorLevel.Error("e"), listOf("a")),
            MessageGroup(ErrorLevel.Warning("w"), listOf("b")),
        )

        // when
        val array = MessageGroup.toJsonArray(groups)

        // then
        assertThat(array.length()).isEqualTo(2)
    }

    @Test
    fun `fromJson handles empty messages`() {
        // given
        val group = MessageGroup(ErrorLevel.Warning("w"), emptyList())
        val json = MessageGroup.toJson(group)

        // when
        val restored = MessageGroup.fromJson(json)

        // then
        assertThat(restored.messages).isEmpty()
    }

    @Test
    fun `errorLevel serialization preserves msg`() {
        // given
        val error = ErrorLevel.Error("custom error")
        val warning = ErrorLevel.Warning("custom warn")
        val jsonError = MessageGroup.toJson(MessageGroup(error, listOf("x")))
        val jsonWarning = MessageGroup.toJson(MessageGroup(warning, listOf("y")))

        // when
        val restoredError = MessageGroup.fromJson(jsonError)
        val restoredWarning = MessageGroup.fromJson(jsonWarning)

        // then
        assertThat((restoredError.errorLevel as ErrorLevel.Error).msg).isEqualTo("custom error")
        assertThat((restoredWarning.errorLevel as ErrorLevel.Warning).msg).isEqualTo("custom warn")
    }
}
