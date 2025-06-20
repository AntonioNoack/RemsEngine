package me.anno.engine

class ScheduledTask(name: String, val time: Long, runnable: () -> Unit) :
    NamedTask(name, runnable), Comparable<ScheduledTask> {

    override fun compareTo(other: ScheduledTask): Int {
        return time.compareTo(other.time)
    }
}