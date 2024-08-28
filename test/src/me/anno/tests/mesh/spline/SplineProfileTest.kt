package me.anno.tests.mesh.spline

import me.anno.ecs.components.mesh.spline.SplineProfile
import me.anno.utils.Color.black
import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector2f
import org.junit.jupiter.api.Test

// todo implement & test splitting profiles with UVs
object SplineProfileTest {

    val openProfile = SplineProfile(
        listOf(
            Vector2f(-7f, -0.49f),
            Vector2f(-3.3f, +0.5f),
            Vector2f(+3.3f, +0.5f),
            Vector2f(+7f, -0.49f),
        ), null,
        IntArrayList(
            intArrayOf(
                0x77dd77 or black,
                0x555555 or black,
                0x555555 or black,
                0x77dd77 or black,
            )
        ),
        false
    )

    val closedProfile = SplineProfile(
        openProfile.positions, openProfile.uvs, openProfile.colors,
        true
    )

    @Test
    fun testSplitOpen() {
        val expectedLeft = SplineProfile(
            listOf(
                Vector2f(-7f, -0.49f),
                Vector2f(-3.3f, +0.5f),
                Vector2f(+0f, +0.5f),
            ), null,
            IntArrayList(
                intArrayOf(
                    0x77dd77 or black,
                    0x555555 or black,
                    0x555555 or black,
                )
            ),
            false
        )
        val expectedRight = SplineProfile(
            listOf(
                Vector2f(-0f, +0.5f),
                Vector2f(+3.3f, +0.5f),
                Vector2f(+7f, -0.49f),
            ), null,
            IntArrayList(
                intArrayOf(
                    0x555555 or black,
                    0x555555 or black,
                    0x77dd77 or black,
                )
            ),
            false
        )
        val (left, right) = openProfile.split()
        assertEquals(expectedLeft, left)
        assertEquals(expectedRight, right)
    }

    @Test
    fun testSplitClosed() {
        val expectedLeft = SplineProfile(
            listOf(
                Vector2f(+0f, -0.49f),
                Vector2f(-7f, -0.49f),
                Vector2f(-3.3f, +0.5f),
                Vector2f(+0f, +0.5f),
            ), null,
            IntArrayList(
                intArrayOf(
                    0x77dd77 or black,
                    0x77dd77 or black,
                    0x555555 or black,
                    0x555555 or black,
                )
            ),
            false
        )
        val expectedRight = SplineProfile(
            listOf(
                Vector2f(+0f, -0.49f),
                Vector2f(-0f, +0.5f),
                Vector2f(+3.3f, +0.5f),
                Vector2f(+7f, -0.49f),
            ), null,
            IntArrayList(
                intArrayOf(
                    0x77dd77 or black,
                    0x555555 or black,
                    0x555555 or black,
                    0x77dd77 or black,
                )
            ),
            false
        )
        val (left, right) = closedProfile.split()
        assertEquals(expectedLeft, left)
        assertEquals(expectedRight, right)
    }
}