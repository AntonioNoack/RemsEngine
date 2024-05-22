package me.anno.tests.engine

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.engine.ECSRegistry
import me.anno.io.saveable.Saveable
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

object TestLazyRegistry {
    @Test
    fun testPrefab() {
        ECSRegistry.init()
        assertIs<Prefab>(Saveable.create("Prefab"))
        assertIs<CSet>(Saveable.create("CSet"))
        assertIs<CAdd>(Saveable.create("CAdd"))
    }
}