import com.github.statnett.loadflowservice.TaskManager
import com.github.statnett.loadflowservice.TaskStatus
import com.github.statnett.loadflowservice.createTask
import com.github.statnett.loadflowservice.solve
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory
import com.powsybl.loadflow.LoadFlowParameters
import junit.framework.TestCase.assertTrue
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestTaskCreator {
    @Test
    fun `test problem solved on background thread`() {
        val tm = TaskManager()
        val network = IeeeCdfNetworkFactory.create14()
        val params = LoadFlowParameters()
        val taskInfo = createTask(tm) {
            solve(network, params)
        }

        // Sleep a little bit so the task can finish
        TimeUnit.SECONDS.sleep(1L)
        val taskStatusResponse = tm.status(taskInfo.id)
        assertEquals(TaskStatus.FINISHED.name, taskStatusResponse.status)
        assertTrue(taskInfo.statusUrl.endsWith(taskInfo.id))
        assertTrue(taskInfo.resultUrl.endsWith(taskInfo.id))
        val task = tm.queue.get(taskInfo.id)
        assertNotNull(task)
        assertTrue(task.scheduledForDeletion)
    }

    @Test
    fun `test scheduled for deletion when exception occur`() {
        val tm = TaskManager()
        val msg = "Something unexpected happened!"
        val taskInfo = createTask(tm) {
            throw RuntimeException(msg)
        }

        // Small sleep to be absolutely sure that the thread was finished
        TimeUnit.MILLISECONDS.sleep(50L)
        val task = tm.queue.get(taskInfo.id)
        assertNotNull(task)
        assertTrue(task.scheduledForDeletion)
        assertNotNull(task.exception)
        assertTrue(tm.status(taskInfo.id).message.contains(msg))
    }
}