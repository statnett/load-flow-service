package com.github.statnett.loadflowservice

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

fun statusUrl(id: String): String {
    return "/status/$id"
}

fun resultUrl(id: String): String {
    return "/result/$id"
}

@Serializable
data class TaskInfo(
    val statusUrl: String,
    val resultUrl: String,
    val id: String,
)

private val logger = KotlinLogging.logger { }

fun createTask(
    tm: TaskManager,
    calculate: () -> ComputationResult,
): TaskInfo {
    val task = Task()
    tm.register(task)
    CoroutineScope(Dispatchers.Default).launch {
        logger.info { "Running task ${task.id} on thread ${Thread.currentThread().name}" }
        try {
            task.status = TaskStatus.RUNNING
            task.result = calculate()
            task.status = TaskStatus.FINISHED
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            task.status = TaskStatus.FAILED
            task.exception = e
        } finally {
            tm.scheduleTaskDeletion(task.id)
        }
    }
    return TaskInfo(
        statusUrl = statusUrl(task.id),
        resultUrl = resultUrl(task.id),
        id = task.id,
    )
}
