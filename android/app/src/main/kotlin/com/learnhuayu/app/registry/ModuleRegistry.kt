package com.learnhuayu.app.registry

import com.learnhuayu.core.model.LearningModule

class ModuleRegistry(modules: Set<LearningModule>) {

    private val modulesById: Map<String, LearningModule> = modules.associateBy { it.id }

    val modules: List<LearningModule> = modules.sortedWith(
        compareBy({ preferredIndex(it.id) }, { it.title }, { it.id }),
    )

    init {
        require(modulesById.size == modules.size) { "Duplicate learning module id" }
    }

    fun module(id: String): LearningModule? = modulesById[id]

    private fun preferredIndex(id: String): Int {
        val index = preferredOrder.indexOf(id)
        return if (index == -1) preferredOrder.size else index
    }

    private companion object {
        val preferredOrder = listOf("tones", "vocabulary", "listening", "speech", "fundamentals")
    }
}
