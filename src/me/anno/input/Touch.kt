package me.anno.input

class Touch(val touchId: Int, var x: Float, var y: Float): Comparable<Touch> {

    val t0 = System.nanoTime()
    val x0 = x
    val y0 = y
    var dx = 0f
    var dy = 0f
    var lastX = x
    var lastY = y

    override fun compareTo(other: Touch): Int = touchId - other.touchId

    fun update(){
        lastX = x
        lastY = y
    }

    fun update(x: Float, y: Float){
        dx = x - this.x
        dy = y - this.y
        this.x = x
        this.y = y
    }

    companion object {

        val maxTouches = 16
        val touches = ArrayList<Touch>()

        fun onTouchDown(touchId: Int, x: Float, y: Float){
            val touch = Touch(touchId, x, y)
            if(touches.size < maxTouches){
                touches.add(touch)
            } else {
                // find the minimum id
                val min = touches.withIndex().minBy { it.value.touchId }!!
                val minIndex = min.index
                onTouchUp(min.value) // up, even if it isn't...
                touches[minIndex] = touch
            }
            touches.sort()
            onTouchDown(touch)
        }

        fun onTouchDown(touch: Touch){

        }

        fun onTouchMove(touchId: Int, x: Float, y: Float){
            val touch = touches.firstOrNull { it.touchId == touchId } ?: return
            touch.update(x, y)
        }

        fun onTouchUp(touchId: Int, x: Float, y: Float){
            val index = touches.binarySearch { it.touchId - touchId }
            if(index < 0) return
            val touch = touches[index]
            // move the last element forward
            touches.removeAt(index)
            touch.update(x, y)
            onTouchUp(touch)
        }

        fun onTouchUp(touch: Touch){

        }

    }

}