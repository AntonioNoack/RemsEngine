package me.anno.tests.engine

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.engine.ECSRegistry
import me.anno.io.saveable.Saveable
import me.anno.utils.assertions.assertIs
import org.junit.jupiter.api.Test

object TestLazyRegistry {
    @Test
    fun testPrefab() {
        ECSRegistry.init()
        assertIs(Prefab::class, Saveable.create("Prefab"))
        assertIs(CSet::class, Saveable.create("CSet"))
        assertIs(CAdd::class, Saveable.create("CAdd"))
    }
}