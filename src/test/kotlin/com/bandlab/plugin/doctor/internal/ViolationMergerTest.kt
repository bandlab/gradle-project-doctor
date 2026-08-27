package com.bandlab.plugin.doctor.internal

import com.bandlab.plugin.doctor.ErrorLevel
import com.bandlab.plugin.doctor.internal.models.MessageGroup
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ViolationMergerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `createMergedViolations merges groups with same errorLevel`() {
        // given
        val error = ErrorLevel.Error("error")
        val file1 = tempFolder.newFile("a.json").apply {
            writeText(
                MessageGroup.toJsonArray(
                    listOf(MessageGroup(error, listOf("e1"))),
                ).toString(),
            )
        }
        val file2 = tempFolder.newFile("b.json").apply {
            writeText(
                MessageGroup.toJsonArray(
                    listOf(MessageGroup(error, listOf("e2"))),
                ).toString(),
            )
        }
        val merger = ViolationMerger()

        // when
        val result = merger.createMergedViolations(listOf(file1, file2), baselineFile = null)

        // then
        assertThat(result).hasSize(1)
        assertThat(result.single().errorLevel).isEqualTo(error)
        assertThat(result.single().messages).containsExactly("e1", "e2")
    }

    @Test
    fun `createMergedViolations keeps different errorLevels separate`() {
        // given
        val file1 = tempFolder.newFile("a.json").apply {
            writeText(
                MessageGroup.toJsonArray(
                    listOf(MessageGroup(ErrorLevel.Error("e"), listOf("e1"))),
                ).toString(),
            )
        }
        val file2 = tempFolder.newFile("b.json").apply {
            writeText(
                MessageGroup.toJsonArray(
                    listOf(MessageGroup(ErrorLevel.Warning("w"), listOf("w1"))),
                ).toString(),
            )
        }
        val merger = ViolationMerger()

        // when
        val result = merger.createMergedViolations(listOf(file1, file2), baselineFile = null)

        // then
        assertThat(result).hasSize(2)
    }

    @Test
    fun `createMergedViolations filters baseline violations`() {
        // given
        val error = ErrorLevel.Error("e")
        val report = tempFolder.newFile("report.json").apply {
            writeText(
                MessageGroup.toJsonArray(
                    listOf(MessageGroup(error, listOf("e1", "e2"))),
                ).toString(),
            )
        }
        val baseline = tempFolder.newFile("baseline.json").apply {
            writeText(
                MessageGroup.toJsonArray(
                    listOf(MessageGroup(error, listOf("e1"))),
                ).toString(),
            )
        }
        val merger = ViolationMerger()

        // when
        val result = merger.createMergedViolations(listOf(report), baselineFile = baseline)

        // then
        assertThat(result).hasSize(1)
        assertThat(result.single().messages).containsExactly("e2")
    }

    @Test
    fun `createMergedViolations returns empty when all are baselined`() {
        // given
        val error = ErrorLevel.Error("e")
        val report = tempFolder.newFile("report.json").apply {
            writeText(
                MessageGroup.toJsonArray(
                    listOf(MessageGroup(error, listOf("e1"))),
                ).toString(),
            )
        }
        val baseline = tempFolder.newFile("baseline.json").apply {
            writeText(
                MessageGroup.toJsonArray(
                    listOf(MessageGroup(error, listOf("e1"))),
                ).toString(),
            )
        }
        val merger = ViolationMerger()

        // when
        val result = merger.createMergedViolations(listOf(report), baselineFile = baseline)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `createMergedViolations ignores non-existent files`() {
        // given
        val missing = tempFolder.newFile("missing.json").apply { delete() }
        val merger = ViolationMerger()

        // when
        val result = merger.createMergedViolations(listOf(missing), baselineFile = null)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `createMergedViolations handles multiple messages in same group`() {
        // given
        val warning = ErrorLevel.Warning("w")
        val file = tempFolder.newFile("a.json").apply {
            writeText(
                MessageGroup.toJsonArray(
                    listOf(MessageGroup(warning, listOf("w1", "w2", "w3"))),
                ).toString(),
            )
        }
        val merger = ViolationMerger()

        // when
        val result = merger.createMergedViolations(listOf(file), baselineFile = null)

        // then
        assertThat(result.single().messages).containsExactly("w1", "w2", "w3")
    }
}
