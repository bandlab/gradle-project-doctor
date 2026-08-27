package com.bandlab.plugin.doctor.internal.utils

/**
 * Prints strings inside a nicely fitted pill box.
 */
internal class PillBoxPrinter(
    private val textOutput: TextOutput,
    private val title: String,
    private val messageLength: Int = 120,
) {

    fun writePrescription(vararg messages: String) {
        if (messages.isNotEmpty()) {
            textOutput.println(createTitle(messageLength))
        }

        messages.forEach { item ->
            textOutput.println(padMessage(item))
            textOutput.println(createEnding(messageLength))
        }
    }

    private fun createTitle(lineLength: Int): String {
        return "\n" + " $title ".padStart(lineLength / 2 + 10, '=').padEnd(lineLength + 4, '=')
    }

    private fun createEnding(lineLength: Int): String {
        return "".padEnd(lineLength + 4, '=')
    }

    private fun padMessage(message: String): String {
        return message.split('\n').flatMap { line ->
            val chunked = line.chunked(messageLength)
            if (chunked.isEmpty()) {
                return@flatMap listOf("| ${"".padEnd(messageLength)} |")
            } else {
                return@flatMap chunked.map { "| ${it.ansiAwarePadEnd(messageLength)} |" }
            }
        }.joinToString("\n")
    }

    private fun String.ansiAwarePadEnd(length: Int): String {
        val isContainsAnsi = contains("\u001B")
        return padEnd(length + if (isContainsAnsi) 9 else 0)
    }

    fun createPill(message: String): String {
        val lines = padMessage(message)
        val pilledLines = listOf(createTitle(messageLength)) + lines + createEnding(messageLength)
        val builder = StringBuilder()

        for (line in pilledLines) {
            builder.append(line).append('\n')
        }
        return builder.toString()
    }
}