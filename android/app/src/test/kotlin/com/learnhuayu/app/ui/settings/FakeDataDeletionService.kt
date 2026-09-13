package com.learnhuayu.app.ui.settings

import com.learnhuayu.core.data.deletion.DataDeletionService

/**
 * Test double for one-tap deletion. [deleteCount] records calls so a test can prove the
 * confirmation gate, and [failure] makes [deleteAll] throw so the failure path can be
 * exercised without touching real storage.
 */
class FakeDataDeletionService(
    var failure: Exception? = null,
) : DataDeletionService {

    var deleteCount = 0
        private set

    var onDelete: suspend () -> Unit = {}

    override suspend fun deleteAll() {
        deleteCount++
        failure?.let { error -> throw error }
        onDelete()
    }
}
