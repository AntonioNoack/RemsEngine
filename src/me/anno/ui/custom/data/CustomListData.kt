package me.anno.ui.custom.data

import me.anno.config.DefaultConfig.style
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.ui.base.Panel
import me.anno.ui.custom.CustomList
import me.anno.ui.custom.CustomListX
import me.anno.ui.custom.CustomListY

class CustomListData(): CustomData() {

    constructor(list: CustomList): this(){
        weight = (list as Panel).weight
        list.dataChildren.forEach {
            children += it.toData()
        }
    }

    val children = ArrayList<CustomData>()

    override fun toPanel(isX: Boolean): Panel {
        val list: CustomList =
            if(isX) CustomListX(style)
            else CustomListY(style)
        list as Panel
        children.forEach {
            list.addChild(it.toPanel(!isX))
        }
        list.weight = weight
        return list
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        children.forEach {
            writer.writeObject(this, "child", it)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "child" -> children.add(value as CustomData)
            else -> super.readObject(name, value)
        }
    }

    override fun getClassName() = "CustomListData"

}