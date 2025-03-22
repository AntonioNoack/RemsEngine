package me.anno.tests.ui.dragging

import me.anno.engine.EngineActions
import me.anno.engine.EngineBase
import me.anno.input.ActionManager
import me.anno.input.ActionManager.keyDragDelay
import me.anno.input.Key
import me.anno.language.translation.NameDesc
import me.anno.tests.FlakyTest
import me.anno.tests.ui.UITests
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.FileInput
import me.anno.utils.OS.documents
import me.anno.utils.OS.home
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertNotNull
import me.anno.utils.assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.math.ceil

class DraggingTest : UITests() {
    @Test
    @FlakyTest("Only works when running separately, why ever")
    @Execution(ExecutionMode.SAME_THREAD)
    fun testCopyValueByDragging() {

        // register everything we need
        EngineActions.register()
        ActionManager.init()
        EngineBase.dragged = null

        val root = home
        val input1 = FileInput(NameDesc("Source"), style, root, emptyList())
        val input2 = FileInput(NameDesc("Destination"), style, root, emptyList())

        val ui = PanelListY(style)
        ui.add(input1)
        ui.add(input2)
        prepareUI(ui)
        updateUI()

        val file1 = documents
        input1.setValue(file1, false)
        assertEquals(file1.toLocalPath(), input1.base2.value)
        assertNull(EngineBase.dragged)

        // start dragging:
        //  - hold mouse still for a moment
        //  - then move it over to the other panel
        moveMouseTo(input1)
        click(Key.BUTTON_LEFT)
        hold(Key.BUTTON_LEFT)
        updateUI()

        val numSteps = ceil(keyDragDelay / 0.1).toInt()
        for (i in 0..numSteps) { // why is one step more required?
            callMouseMove()
            skipTime(0.1)
            updateUI()
        }

        moveMouseTo(input2)
        callMouseMove()
        updateUI()

        val dragged = assertNotNull(EngineBase.dragged)
        assertEquals(file1, dragged.getOriginal())
        assertNotEquals(file1, input2.value)

        release(Key.BUTTON_LEFT)

        // check that value was properly dropped
        assertEquals(file1, input2.value)
    }
}