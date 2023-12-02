package com.github.statnett.loadflowservice

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private val logger = KotlinLogging.logger { }

enum class TaskStatus {
    CREATED, RUNNING, FINISHED, FAILED
}

class Task {
    var status = TaskStatus.CREATED
    var result: ComputationResult? = null
    val id = UUID.randomUUID().toString()
    var exception: Exception? = null
    var scheduledForDeletion = false
    private val createdAt = System.currentTimeMillis()

    fun ageSeconds(): Int {
        return ((System.currentTimeMillis() - createdAt) / 1000).toInt()
    }
}

class TaskQueue {
    private val tasks = mutableListOf<Task>()

    fun get(id: String): Task? {
        return tasks.firstOrNull { task -> task.id == id }
    }

    fun size(): Int {
        return tasks.size
    }

    fun register(task: Task) {
        tasks.add(task)
    }

    fun remove(id: String) {
        tasks.removeIf { t -> t.id == id }
    }

    fun finished(id: String): Boolean {
        val task = get(id) ?: return false
        return task.status == TaskStatus.FINISHED
    }

    fun clearOlderThan(ageSeconds: Int) {
        tasks.removeIf { t -> (t.ageSeconds() > ageSeconds) and (t.status != TaskStatus.RUNNING) }
    }

    fun numRunning(): Int {
        return tasks.count { t -> t.status == TaskStatus.RUNNING }
    }

    fun setDeleteStatus(id: String) {
        val task = get(id) ?: return
        task.scheduledForDeletion = true
    }
}

class TaskDoesNotExistException(message: String) : Exception(message)
class FullBufferException(message: String) : Exception(message)

@Serializable
data class TaskStatusResponse(val status: String, val message: String)

class TaskManager(private val maxRunningTasks: Int = 100, private val retention: Int = 10 * 60) {
    internal val queue = TaskQueue()

    fun status(id: String): TaskStatusResponse {
        val task = queue.get(id) ?: throw TaskDoesNotExistException("No task with id $id")
        val message = if (task.exception != null) "${task.exception}" else ""
        return TaskStatusResponse(
            task.status.name,
            message
        )
    }

    fun size(): Int {
        return queue.size()
    }

    fun numRunning(): Int {
        return queue.numRunning()
    }

    private fun clearOld() {
        queue.clearOlderThan(retention)
    }

    private fun raiseOnFull() {
        if (numRunning() >= maxRunningTasks) {
            throw FullBufferException("Could not add task because the buffer is full")
        }
    }

    fun register(task: Task) {
        raiseOnFull()
        queue.register(task)
    }


    suspend fun respondWithResult(call: ApplicationCall, id: String) {
        if (status(id).status != TaskStatus.FINISHED.name) {
            call.respondText("Task not finished", status = HttpStatusCode.NotFound)
            return
        }

        val result = queue.get(id)
        if (result != null) {
            val res = Json.encodeToString(result.result)
            call.respondText(res, contentType = ContentType.Application.Json)
            queue.remove(id)
        }

        throw TaskDoesNotExistException("No task with id $id")
    }

    suspend fun scheduleTaskDeletion(id: String) {
        val timeUntilDelete = retention.toDuration(DurationUnit.SECONDS)
        logger.info { "Deleting task $id in $timeUntilDelete" }
        queue.setDeleteStatus(id)
        delay(timeUntilDelete)
        queue.remove(id)
        logger.info { "Deleted task $id" }
    }

}
