package org.recast4j.detour.extras.jumplink

import org.recast4j.recast.RecastBuilderResult

interface GroundSampler {
    fun sample(cfg: JumpLinkBuilderConfig, result: RecastBuilderResult, es: EdgeSampler)
}