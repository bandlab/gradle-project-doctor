package com.bandlab.plugin.doctor.extension

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MatchableValueTest {

    // region Match

    @Test
    fun `Match equals with ignoreCase false is case sensitive`() {
        // given
        val matcher = MatchableValue.Match("Foo", ignoreCase = false)

        // when - then
        assertThat(matcher.isMatch("Foo")).isTrue()
        assertThat(matcher.isMatch("foo")).isFalse()
        assertThat(matcher.isMatch("FooBar")).isFalse()
    }

    @Test
    fun `Match equals with ignoreCase true is case insensitive`() {
        // given
        val matcher = MatchableValue.Match("Foo", ignoreCase = true)

        // when - then
        assertThat(matcher.isMatch("foo")).isTrue()
        assertThat(matcher.isMatch("FOO")).isTrue()
        assertThat(matcher.isMatch("FoO")).isTrue()
    }

    // endregion

    // region Contains

    @Test
    fun `Contains matches substring`() {
        // given
        val matcher = MatchableValue.Contains("screen", ignoreCase = false)

        // when - then
        assertThat(matcher.isMatch(":image:zoom:screen")).isTrue()
        assertThat(matcher.isMatch(":common:utils")).isFalse()
    }

    @Test
    fun `Contains with ignoreCase handles case`() {
        // given
        val matcher = MatchableValue.Contains("Screen", ignoreCase = true)

        // when - then
        assertThat(matcher.isMatch(":image:zoom:screen")).isTrue()
        assertThat(matcher.isMatch(":IMAGE:ZOOM:SCREEN")).isTrue()
    }

    // endregion

    // region DoesNotContain

    @Test
    fun `DoesNotContain returns true when substring absent`() {
        // given
        val matcher = MatchableValue.DoesNotContain("screen", ignoreCase = false)

        // when - then
        assertThat(matcher.isMatch(":common:utils")).isTrue()
        assertThat(matcher.isMatch(":image:zoom:screen")).isFalse()
    }

    // endregion

    // region EndsWith

    @Test
    fun `EndsWith matches suffix case sensitive`() {
        // given
        val matcher = MatchableValue.EndsWith(":screen", ignoreCase = false)

        // when - then
        assertThat(matcher.isMatch(":image:zoom:screen")).isTrue()
        assertThat(matcher.isMatch(":image:zoom:Screen")).isFalse()
        assertThat(matcher.isMatch(":image:zoom:screen:extra")).isFalse()
    }

    @Test
    fun `EndsWith with ignoreCase true matches case insensitively`() {
        // given
        val matcher = MatchableValue.EndsWith(":SCREEN", ignoreCase = true)

        // when - then
        assertThat(matcher.isMatch(":image:zoom:screen")).isTrue()
        assertThat(matcher.isMatch(":IMAGE:ZOOM:SCREEN")).isTrue()
    }

    // endregion

    // region AlwaysValid / AlwaysInvalid

    @Test
    fun `AlwaysValid matches any target`() {
        // given
        val matcher = MatchableValue.AlwaysValid()

        // when - then
        assertThat(matcher.isMatch("anything")).isTrue()
        assertThat(matcher.isMatch("")).isTrue()
        assertThat(matcher.isMatch(":a:b")).isTrue()
    }

    @Test
    fun `AlwaysInvalid matches nothing`() {
        // given
        val matcher = MatchableValue.AlwaysInvalid()

        // when - then
        assertThat(matcher.isMatch("anything")).isFalse()
        assertThat(matcher.isMatch("")).isFalse()
    }

    // endregion
}
