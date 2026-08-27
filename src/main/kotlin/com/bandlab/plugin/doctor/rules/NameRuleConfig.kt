package com.bandlab.plugin.doctor.rules

import com.bandlab.plugin.doctor.extension.MatchableValue
import java.io.Serializable

class NameRuleConfig : Serializable {

    internal var nameMatchers = mutableSetOf<MatchableValue>()

    @JvmOverloads
    fun nameContains(value: String, ignoreCase: Boolean = false) {
        nameMatchers += MatchableValue.Contains(value, ignoreCase)
    }

    @JvmOverloads
    fun nameDoesNotContain(value: String, ignoreCase: Boolean = false) {
        nameMatchers += MatchableValue.DoesNotContain(value, ignoreCase)
    }
}