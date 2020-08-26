package me.anno.input.touch

import jwinpointer.JWinPointerReader
import jwinpointer.JWinPointerReader.PointerEventListener

object JWinTouch {

    fun init(windowTitle: String){
        val jWinPointerReader = JWinPointerReader(windowTitle)
        jWinPointerReader.addPointerEventListener(object : PointerEventListener {
            override fun pointerXYEvent(
                deviceType: Int,
                pointerID: Int,
                eventType: Int,
                inverted: Boolean,
                x: Int,
                y: Int,
                pressure: Int
            ) {
                // todo process touch events
                // System.out.println("DeviceType: "+deviceType+", PointerID: "+pointerID+", EventType: "+eventType+", Inverted: "+inverted+", x: "+x+", y: "+y+", pressure: "+pressure);
                if(Math.random() < 0.01) println("Ptr-XY: $deviceType $pointerID, $eventType, $inverted")
            }

            override fun pointerButtonEvent(
                deviceType: Int,
                pointerID: Int,
                eventType: Int,
                inverted: Boolean,
                buttonIndex: Int
            ) {
                println("Click: $deviceType, $pointerID, $eventType, $inverted, $buttonIndex")
            }

            override fun pointerEvent(deviceType: Int, pointerID: Int, eventType: Int, inverted: Boolean) {
                println("Ptr: $deviceType, $pointerID, $eventType, $inverted")
            }
        })
    }
}