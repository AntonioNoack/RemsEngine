package me.anno.tests.lua

import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.input.Key
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.lua.QuickInputScriptComponent
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class QuickInputScriptTest {

    class TestComponent : Component() {
        var result = ""
    }

    private val getTester = "" +
            "tester = EntityQuery:getComponent(entity, FindClass('TestComponent'))\n"

    @BeforeEach
    fun registerTypes() {
        Engine.cancelShutdown()
        registerCustomClass(Entity::class)
        registerCustomClass(TestComponent::class)
        registerCustomClass(QuickInputScriptComponent::class)
    }

    @Test
    fun testKeyUp() {
        assertEquals("KEY_F11", testScript {
            it.keyUpScript = getTester +
                    "tester:setResult(key)"
            it.onKeyUp(Key.KEY_F11)
        })
    }

    @Test
    fun testKeyDown() {
        assertEquals("KEY_F12", testScript {
            it.keyDownScript = getTester +
                    "tester:setResult(key)"
            it.onKeyDown(Key.KEY_F12)
        })
    }

    @Test
    fun testKeyTyped() {
        assertEquals("KEY_F12", testScript {
            it.keyTypedScript = getTester +
                    "tester:setResult(key)"
            it.onKeyTyped(Key.KEY_F12)
        })
    }

    @Test
    fun testCharTyped() {
        assertEquals("256", testScript {
            it.charTypedScript = getTester +
                    "tester:setResult(char)"
            it.onCharTyped(256)
        })
    }

    @Test
    fun testMouseMoved() {
        assertEquals("1, 2, 3, 4", testScript {
            it.mouseMoveScript = getTester +
                    "tester:setResult(x .. ', ' .. y .. ', ' .. dx .. ', ' .. dy)"
            it.onMouseMoved(1f, 2f, 3f, 4f)
        })
    }

    @Test
    fun testMouseClick() {
        assertEquals("BUTTON_LEFT, false", testScript {
            it.mouseClickScript = getTester +
                    "tester:setResult(tostring(button) .. ', ' .. tostring(long))"
            it.onMouseClicked(Key.BUTTON_LEFT, false)
        })
    }

    @Test
    fun testMouseWheel() {
        assertEquals("1, 2, 3, 4, false", testScript {
            it.mouseWheelScript = getTester +
                    "tester:setResult(x .. ', ' .. y .. ', ' .. dx .. ', ' .. dy .. ', ' .. tostring(byMouse))"
            it.onMouseWheel(1f, 2f, 3f, 4f, false)
        })
    }

    @Test
    fun testAction() {
        assertEquals("1, 2, 3, 4, action", testScript {
            it.actionScript = getTester +
                    "tester:setResult(x .. ', ' .. y .. ', ' .. dx .. ', ' .. dy .. ', ' .. tostring(action))"
            it.onGotAction(1f, 2f, 3f, 4f, "action")
        })
    }

    private fun testScript(init: (QuickInputScriptComponent) -> Unit): String {
        val tester = TestComponent()
        val instance = QuickInputScriptComponent()
        Entity()
            .add(instance)
            .add(tester)
        init(instance)
        return tester.result
    }
}