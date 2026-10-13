// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.plugin.doctor.extension

import java.io.Serializable

sealed interface MatchableValue : Serializable {

    val value: String

    fun isMatch(target: String): Boolean

    class AlwaysValid : MatchableValue {
        override val value: String = ""

        override fun isMatch(target: String): Boolean = true
    }

    class AlwaysInvalid : MatchableValue {
        override val value: String = ""

        override fun isMatch(target: String): Boolean = false
    }

    data class Match(
        override val value: String,
        val ignoreCase: Boolean = false,
    ) : MatchableValue {
        override fun isMatch(target: String): Boolean {
            return value.equals(target, ignoreCase = ignoreCase)
        }

        override fun toString(): String {
            return "Match(value='$value', ignoreCase=$ignoreCase)"
        }
    }

    data class Contains(
        override val value: String,
        val ignoreCase: Boolean = false,
    ) : MatchableValue {
        override fun isMatch(target: String): Boolean {
            return target.contains(value, ignoreCase = ignoreCase)
        }

        override fun toString(): String {
            return "Contains(value='$value', ignoreCase=$ignoreCase)"
        }
    }

    data class DoesNotContain(
        override val value: String,
        val ignoreCase: Boolean = false,
    ) : MatchableValue {
        override fun isMatch(target: String): Boolean {
            return !target.contains(value, ignoreCase = ignoreCase)
        }

        override fun toString(): String {
            return "DoesNotContain(value='$value', ignoreCase=$ignoreCase)"
        }
    }

    data class EndsWith(
        override val value: String,
        val ignoreCase: Boolean = false,
    ) : MatchableValue {
        override fun isMatch(target: String): Boolean {
            return if (ignoreCase) {
                target.lowercase().endsWith(value.lowercase())
            } else {
                target.endsWith(value)
            }
        }

        override fun toString(): String {
            return "EndsWith(value='$value', ignoreCase=$ignoreCase)"
        }
    }
}
