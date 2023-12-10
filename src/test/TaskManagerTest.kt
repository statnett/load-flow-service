import com.github.statnett.loadflowservice.FullBufferException
import com.github.statnett.loadflowservice.Task
import com.github.statnett.loadflowservice.TaskManager
import com.github.statnett.loadflowservice.TaskStatus
import junit.framework.TestCase.*
import kotlin.test.Test
import kotlin.test.assertFailsWith

class TaskManagerTest {
    @Test
    fun `test add load flow task`() {
        val task1 = Task()
        val task2 = Task()
        val task3 = Task()
        task2.status = TaskStatus.RUNNING
        task3.status = TaskStatus.FINISHED
        val tm = TaskManager()
        tm.register(task1)
        tm.register(task2)
        tm.register(task3)

        assertEquals(1, tm.numRunning())
        assertEquals(1, tm.queue.numRunning())
        assertEquals(3, tm.size())

        assertEquals(task1, tm.queue.get(task1.id))
        assertEquals(task2, tm.queue.get(task2.id))
        assertFalse(tm.queue.finished(task1.id))
        assertFalse(tm.queue.finished(task2.id))
        assertTrue(tm.queue.finished(task3.id))

        tm.queue.remove(task1.id)
        assertNull(tm.queue.get(task1.id))
        assertEquals(2, tm.queue.size())
    }

    @Test
    fun `test tasks not inserted when max number of tasks are running`() {
        val maxRunning = 5
        val tm = TaskManager(maxRunning)
        repeat (maxRunning) { tm.register(runningTask()) }
        assertEquals(maxRunning, tm.numRunning())
        assertEquals(maxRunning, tm.size())

        // When inserting one more, it should raise FullBufferError
        assertFailsWith<FullBufferException> { tm.register(runningTask()) }

        // Verify that the task was not inserted
        assertEquals(maxRunning, tm.size())
    }
}

fun runningTask(): Task {
    val task = Task()
    task.status = TaskStatus.RUNNING
    return task
}