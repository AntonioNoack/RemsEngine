package me.anno.studio.dependencies

import java.util.concurrent.LinkedTransferQueue

// todo when rendering, use the dependency manager
class DependencyManager {

    val keptResources = HashSet<Resource>()

    val steps = LinkedTransferQueue<ResourceSet>()

    var currentStep = ResourceSet()
    var lastStep = ResourceSet()

    fun freeSuperfluousResources() {
        // free resources, which are no longer required
        synchronized(this){
            val freed = HashSet<Resource>()
            keptResources.forEach { resource ->
                if (resource !in currentStep && steps.none { step -> resource in step }) {
                    free(resource)
                    freed += resource
                }
            }
            keptResources.removeAll(freed)
        }
    }

    fun freeResourcesTemporarily() {
        // free resources, which will be needed later, but not now
        synchronized(this){
            val freed = HashSet<Resource>()
            keptResources.forEach { resource ->
                if (resource !in currentStep) {
                    free(resource)
                    freed += resource
                }
            }
            keptResources.removeAll(freed)
        }
    }

    // to be executed at the end
    fun freeRemaining(){
        synchronized(this){
            keptResources.forEach { free(it) }
            keptResources.clear()
        }
    }

    fun free(resource: Resource) {
        val cache = resource.cache
        cache.free(resource.key)
    }

    fun nextStep(){
        currentStep = steps.poll()
    }

    fun appendStep(){
        val step = ResourceSet()
        lastStep = step
        steps += step
    }

    fun addRequirement(resource: Resource){
        lastStep.add(resource)
    }

}