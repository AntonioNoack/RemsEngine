package me.anno.ui.custom.data

import me.anno.config.DefaultConfig.style
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.ui.base.Panel
import me.anno.ui.custom.CustomContainer
import me.anno.ui.custom.CustomList
import me.anno.ui.custom.TypeLibrary

class CustomPanelData(): CustomData() {

    constructor(panel: Panel): this(){
        type = panel.javaClass.simpleName
        weight = panel.weight
        if(panel is CustomList){
            content = panel.toData()
        }
    }

    var type = ""
    var content: CustomData? = null

    override fun toPanel(isX: Boolean): Panel {
        val child = when(type){
            "CustomListX" -> content!!.toPanel(isX)
            "CustomListY" -> content!!.toPanel(isX)
            else -> TypeLibrary.types.getValue(type).constructor.invoke()
        }
        val container = CustomContainer(child, style)
        container.weight = weight
        return container
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("type", type)
        writer.writeObject(this, "content", content)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "content" -> content = value as CustomData
            else -> super.readObject(name, value)
        }
    }

    override fun readString(name: String, value: String) {
        when(name){
            "type" -> type = value
            else -> super.readString(name, value)
        }
    }

    override fun getClassName() = "CustomPanelData"

}