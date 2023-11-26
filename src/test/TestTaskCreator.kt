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
    }
}