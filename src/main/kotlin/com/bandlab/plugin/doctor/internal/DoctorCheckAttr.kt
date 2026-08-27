package com.bandlab.plugin.doctor.internal

import org.gradle.api.Named
import org.gradle.api.attributes.Attribute

internal interface DoctorCheckAttr : Named {
    companion object {
        val ATTR = Attribute.of("com.bandlab.project.doctor.check", String::class.java)
    }
}
