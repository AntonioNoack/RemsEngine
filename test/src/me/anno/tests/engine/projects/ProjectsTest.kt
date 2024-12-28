package me.anno.tests.engine.projects

import me.anno.engine.projects.ProjectHeader
import me.anno.engine.projects.Projects.addToRecentProjects
import me.anno.engine.projects.Projects.getRecentProjects
import me.anno.engine.projects.Projects.removeFromRecentProjects
import me.anno.io.files.FileFileRef
import me.anno.utils.assertions.assertContains
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertNotContains
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test

class ProjectsTest {
    @Test
    fun testRecentProjects() {
        // preparing
        val tmp = FileFileRef.createTempFolder("test")
        assertTrue(tmp.exists)
        val p0 = ProjectHeader("abc", tmp.getChild("abc").apply { tryMkdirs() })
        val p1 = ProjectHeader("123", tmp.getChild("123").apply { tryMkdirs() })

        // adding
        addToRecentProjects(p0)
        addToRecentProjects(p1)
        val recent0 = getRecentProjects()
        assertTrue(recent0.size >= 2)
        assertContains(p0, recent0)
        assertContains(p1, recent0)

        // remove one specific element
        removeFromRecentProjects(p0.file)
        val recent1 = getRecentProjects()
        assertContains(p1, recent1)
        assertNotContains(p0, recent1)

        // add it back, and remove the other element
        addToRecentProjects(p0)
        removeFromRecentProjects(p1.file)
        val recent2 = getRecentProjects()
        assertContains(p0, recent2)
        assertNotContains(p1, recent2)

        // remove all projects
        val recent3 = getRecentProjects()
        for (project in recent3) {
            removeFromRecentProjects(project.file)
        }
        assertEquals(emptyList<ProjectsTest>(), getRecentProjects())

        // clean-up
        tmp.delete()
        assertFalse(tmp.exists)
    }
}