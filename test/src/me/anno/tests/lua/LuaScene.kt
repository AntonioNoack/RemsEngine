package me.anno.tests.lua

import me.anno.ecs.Entity
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.lua.QuickScriptComponent

/**
 * Shows how Lua scripting can be used.
 * */
fun main() {

    ECSRegistry.init()

    val qs = QuickScriptComponent()
    qs.createScript = """
        -- creating and calling Lua from inside Lua xD
        -- like JavaScript eval(), highly unsafe xD
        q = R:QuickScriptComponent()
        q:setCreateScript('print("Hello World!")')
        q:onCreate()
        -- expanding the sample scene
        cubeMesh = OS:getDocuments():getChild('icosphere.obj') -- just a file
        cubeMeshI = MeshCache:get(cubeMesh,false) -- mesh instance; API for MeshCache instance might change
        dx = cubeMeshI:getBounds():getDeltaX()
        dz = cubeMeshI:getBounds():getDeltaZ()
        for z=0,5 do
            group = R:Entity('Group ' .. tostring(z), entity)
            group:getTransform():setLocalPosition(R:Vector3d(0,0,z*dz))
            for x=0,5 do
                child = R:Entity('Instance ' .. tostring(x) .. ', ' .. tostring(z), group)
                -- constructors with arguments not yet supported
                child:getTransform():setLocalPosition(R:Vector3d(x*dx,0,0))
                child:add(R:MeshComponent(cubeMesh))
            end
        end
        
        -- sample from LuaJ on how to get access to Java:
        -- JFrame = luajava.bindClass("javax.swing.JFrame")
        -- frame = luajava.newInstance("javax.swing.JFrame", "Texts");
        -- frame:setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        -- frame:setSize(300,400)
        -- frame:setVisible(true)
            """.trimIndent()

    testSceneWithUI("LuaScene", Entity(qs))
}