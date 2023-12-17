package me.anno.tests.engine.prefab

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.ECSTreeView
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * a few tests to verify that changing the order of Entities within the ECSTreeView is possible
 * */
class TreeViewDragNDrop {

    init {
        ECSRegistry.initPrefabs()
        ECSRegistry.initMeshes()
    }

    fun tested(): ECSTreeView {
        return ECSTreeView(style)
    }

    fun <V> assertContentEquals(expected: List<V>, actual: List<V>) {
        if (expected.size != actual.size || expected != actual) {
            println("Expected: [\n  ${expected.joinToString(",\n  ")}]")
            println("Actual: [\n  ${actual.joinToString(",\n  ")}]")
        }
        assertEquals(expected.size, actual.size)
        val different = expected.withIndex()
        for (diff in different) {
            assertEquals(diff.value, actual[diff.index])
        }
    }

    fun getSortingName(set: CSet): String {
        return set.path.getNameIds().joinToString("/", "", "/${set.name}")
    }

    fun checkPrefab(prefab: Prefab, adds: List<CAdd>, sets: List<CSet>) {
        assertContentEquals(adds, prefab.adds.entries
            .sortedBy { it.key.depth }
            .map { it.value }.flatten()
        )
        assertContentEquals(
            sets.sortedBy(::getSortingName),
            prefab.sets.map { k1, k2, v -> CSet(k1, k2, v) }.sortedBy(::getSortingName)
        )
    }

    @Test
    fun changeOrderAB() {
        val fileA = Entity("A").ref
        val fileB = Entity("B").ref
        val prefab = Prefab("Entity")
        prefab.add(CAdd(Path.ROOT_PATH, 'e', "Entity", "A", fileA), 0)
        prefab.add(CAdd(Path.ROOT_PATH, 'e', "Entity", "B", fileB), 1)
        prefab[Path(Path.ROOT_PATH, "A", 0, 'e'), "name"] = "EntityA"
        prefab[Path(Path.ROOT_PATH, "B", 1, 'e'), "name"] = "EntityB"
        checkPrefab(
            prefab, listOf(
                CAdd(Path.ROOT_PATH, 'e', "Entity", "A", fileA),
                CAdd(Path.ROOT_PATH, 'e', "Entity", "B", fileB)
            ), listOf(
                CSet(Path(Path.ROOT_PATH, "A", 0, 'e'), "name", "EntityA"),
                CSet(Path(Path.ROOT_PATH, "B", 1, 'e'), "name", "EntityB"),
            )
        )
        val hovered = prefab.getSampleInstance() as Entity
        val original = hovered.children[0]
        tested().paste(hovered, original, 1f, "?")
        checkPrefab(
            prefab, listOf(
                CAdd(Path.ROOT_PATH, 'e', "Entity", "B", fileB),
                CAdd(Path.ROOT_PATH, 'e', "Entity", "A", fileA),
            ), listOf(
                CSet(Path(Path.ROOT_PATH, "B", 0, 'e'), "name", "EntityB"),
                CSet(Path(Path.ROOT_PATH, "A", 1, 'e'), "name", "EntityA"),
            )
        )
    }

    @Test
    fun appendAOntoB() {
        val fileA = Entity("A").ref
        val fileB = Entity("B").ref
        val prefab = Prefab("Entity")
        prefab.add(CAdd(Path.ROOT_PATH, 'e', "Entity", "A", fileA), 0)
        prefab.add(CAdd(Path.ROOT_PATH, 'e', "Entity", "B", fileB), 1)
        prefab[Path(Path.ROOT_PATH, "A", 0, 'e'), "name"] = "EntityA"
        prefab[Path(Path.ROOT_PATH, "B", 1, 'e'), "name"] = "EntityB"
        checkPrefab(
            prefab, listOf(
                CAdd(Path.ROOT_PATH, 'e', "Entity", "A", fileA),
                CAdd(Path.ROOT_PATH, 'e', "Entity", "B", fileB)
            ), listOf(
                CSet(Path(Path.ROOT_PATH, "A", 0, 'e'), "name", "EntityA"),
                CSet(Path(Path.ROOT_PATH, "B", 1, 'e'), "name", "EntityB"),
            )
        )
        val sample = prefab.getSampleInstance() as Entity
        val hovered = sample.children[1]
        val original = sample.children[0]
        tested().paste(hovered, original, 0.5f, "?")
        val pathB = Path(Path.ROOT_PATH, "B", 0, 'e')
        val pathA = Path(pathB, "A", 0, 'e')
        checkPrefab(
            prefab, listOf(
                CAdd(Path.ROOT_PATH, 'e', "Entity", "B", fileB),
                CAdd(pathB, 'e', "Entity", "A", fileA),
            ), listOf(
                CSet(pathB, "name", "EntityB"),
                CSet(pathA, "name", "EntityA"),
            )
        )
    }

    @Test
    fun appendBOntoA() {
        val fileA = Entity("A").ref
        val fileB = Entity("B").ref
        val prefab = Prefab("Entity")
        prefab.add(CAdd(Path.ROOT_PATH, 'e', "Entity", "A", fileA), 0)
        prefab.add(CAdd(Path.ROOT_PATH, 'e', "Entity", "B", fileB), 1)
        prefab[Path(Path.ROOT_PATH, "A", 0, 'e'), "name"] = "EntityA"
        prefab[Path(Path.ROOT_PATH, "B", 1, 'e'), "name"] = "EntityB"
        checkPrefab(
            prefab, listOf(
                CAdd(Path.ROOT_PATH, 'e', "Entity", "A", fileA),
                CAdd(Path.ROOT_PATH, 'e', "Entity", "B", fileB)
            ), listOf(
                CSet(Path(Path.ROOT_PATH, "A", 0, 'e'), "name", "EntityA"),
                CSet(Path(Path.ROOT_PATH, "B", 1, 'e'), "name", "EntityB"),
            )
        )
        val sample = prefab.getSampleInstance() as Entity
        tested().paste(
            hovered = sample.children[0],
            original = sample.children[1],
            0.5f, "?"
        )
        val pathA = Path(Path.ROOT_PATH, "A", 0, 'e')
        val pathB = Path(pathA, "B", 0, 'e')
        checkPrefab(
            prefab, listOf(
                CAdd(Path.ROOT_PATH, 'e', "Entity", "A", fileA),
                CAdd(pathA, 'e', "Entity", "B", fileB),
            ), listOf(
                CSet(pathA, "name", "EntityA"),
                CSet(pathB, "name", "EntityB")
            )
        )
    }

    // todo tests for copying
    @Test
    fun appendBOntoB() {
        val fileA = Entity("A").ref
        val fileB = Entity("B").ref
        val prefab = Prefab("Entity")
        prefab.add(CAdd(Path.ROOT_PATH, 'e', "Entity", "A", fileA), 0)
        prefab.add(CAdd(Path.ROOT_PATH, 'e', "Entity", "B", fileB), 1)
        prefab[Path(Path.ROOT_PATH, "A", 0, 'e'), "name"] = "EntityA"
        prefab[Path(Path.ROOT_PATH, "B", 1, 'e'), "name"] = "EntityB"
        checkPrefab(
            prefab, listOf(
                CAdd(Path.ROOT_PATH, 'e', "Entity", "A", fileA),
                CAdd(Path.ROOT_PATH, 'e', "Entity", "B", fileB)
            ), listOf(
                CSet(Path(Path.ROOT_PATH, "A", 0, 'e'), "name", "EntityA"),
                CSet(Path(Path.ROOT_PATH, "B", 1, 'e'), "name", "EntityB"),
            )
        )
        val sample = prefab.getSampleInstance() as Entity
        tested().paste(
            hovered = sample.children[1],
            original = sample.children[1],
            0.5f, "?"
        )
        val pathA = Path(Path.ROOT_PATH, "A", 0, 'e')
        val pathB = Path(Path.ROOT_PATH, "B", 1, 'e')
        val pathBB = Path(pathB, "B", 0, 'e')
        checkPrefab(
            prefab, listOf(
                CAdd(Path.ROOT_PATH, 'e', "Entity", "A", fileA),
                CAdd(Path.ROOT_PATH, 'e', "Entity", "B", fileB),
                CAdd(pathB, 'e', "Entity", "B", fileB),
            ), listOf(
                CSet(pathA, "name", "EntityA"),
                CSet(pathB, "name", "EntityB"),
                CSet(pathBB, "name", "EntityB"),
            )
        )
    }

    @Test
    fun appendRootOntoB() {
        val fileA = Entity("A").ref
        val fileB = Entity("B").ref
        val prefab = Prefab("Entity")
        prefab.add(CAdd(Path.ROOT_PATH, 'e', "Entity", "A", fileA), 0)
        prefab.add(CAdd(Path.ROOT_PATH, 'e', "Entity", "B", fileB), 1)
        prefab[Path.ROOT_PATH, "name"] = "EntityE"
        prefab[Path(Path.ROOT_PATH, "A", 0, 'e'), "name"] = "EntityA"
        prefab[Path(Path.ROOT_PATH, "B", 1, 'e'), "name"] = "EntityB"
        checkPrefab(
            prefab, listOf(
                CAdd(Path.ROOT_PATH, 'e', "Entity", "A", fileA),
                CAdd(Path.ROOT_PATH, 'e', "Entity", "B", fileB)
            ), listOf(
                CSet(Path.ROOT_PATH, "name", "EntityE"),
                CSet(Path(Path.ROOT_PATH, "A", 0, 'e'), "name", "EntityA"),
                CSet(Path(Path.ROOT_PATH, "B", 1, 'e'), "name", "EntityB"),
            )
        )
        val sample = prefab.getSampleInstance() as Entity
        tested().paste(
            hovered = sample.children[1],
            original = sample,
            0.5f, "?"
        )
        println(prefab.getSampleInstance())
        val pathA = Path(Path.ROOT_PATH, "A", 0, 'e')
        val pathB = Path(Path.ROOT_PATH, "B", 1, 'e')
        val rootName = prefab.adds[pathB]!!.first().nameId
        assertEquals(rootName[0], '#')
        val pathBE = Path(pathB, rootName, 0, 'e')
        checkPrefab(
            prefab, listOf(
                CAdd(Path.ROOT_PATH, 'e', "Entity", "A", fileA),
                CAdd(Path.ROOT_PATH, 'e', "Entity", "B", fileB),
                CAdd(pathB, 'e', "Entity", rootName),
                CAdd(pathBE, 'e', "Entity", "A", fileA),
                CAdd(pathBE, 'e', "Entity", "B", fileB),
            ), listOf(
                CSet(Path.ROOT_PATH, "name", "EntityE"),
                CSet(pathA, "name", "EntityA"),
                CSet(pathB, "name", "EntityB"),
                CSet(pathBE, "name", "EntityE"),
                CSet(Path(pathBE, "A", 0, 'e'), "name", "EntityA"),
                CSet(Path(pathBE, "B", 0, 'e'), "name", "EntityB"),
            )
        )
    }
}