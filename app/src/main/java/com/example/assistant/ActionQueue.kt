package com.example.assistant

import com.example.apps.AppActionResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

enum class StepStatus {
    PENDING,
    RUNNING,
    VERIFIED,
    COMPLETED,
    FAILED
}

data class QueuedAction(
    val id: String,
    val description: String,
    var status: StepStatus = StepStatus.PENDING,
    val execute: suspend () -> AppActionResult
)

class ActionQueue {
    private val queue = mutableListOf<QueuedAction>()
    private var currentExecutionJob: Job? = null
    @Volatile
    private var isCancelled = false

    fun clear() {
        isCancelled = true
        queue.clear()
        currentExecutionJob?.cancel()
        currentExecutionJob = null
    }

    fun enqueue(description: String, execute: suspend () -> AppActionResult) {
        queue.add(
            QueuedAction(
                id = "Action_${System.currentTimeMillis()}_${queue.size}",
                description = description,
                execute = execute
            )
        )
    }

    suspend fun executeAll(
        onStepRunning: (String) -> Unit,
        onStepCompleted: (String, AppActionResult) -> Unit,
        onStepFailed: (String, String) -> Unit,
        onRequiresConfirmation: (AppActionResult) -> Unit
    ): Boolean = coroutineScope {
        isCancelled = false
        for (action in queue) {
            if (isCancelled) {
                action.status = StepStatus.FAILED
                return@coroutineScope false
            }

            action.status = StepStatus.RUNNING
            onStepRunning(action.description)

            try {
                val result = action.execute()
                if (result.requiresConfirmation) {
                    action.status = StepStatus.VERIFIED
                    onRequiresConfirmation(result)
                    return@coroutineScope true
                }

                if (result.success) {
                    action.status = if (result.verified) StepStatus.COMPLETED else StepStatus.VERIFIED
                    onStepCompleted(action.description, result)
                    delay(300) // Brief pause between chained UI transitions
                } else {
                    action.status = StepStatus.FAILED
                    onStepFailed(action.description, result.message)
                    return@coroutineScope false
                }
            } catch (ce: CancellationException) {
                action.status = StepStatus.FAILED
                return@coroutineScope false
            } catch (e: Exception) {
                action.status = StepStatus.FAILED
                onStepFailed(action.description, "বস, কাজ চলাকালীন সমস্যা হয়েছে: ${e.localizedMessage}")
                return@coroutineScope false
            }
        }
        queue.clear()
        true
    }
}
