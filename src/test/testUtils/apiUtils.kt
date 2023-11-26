package testUtils

import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay


suspend fun retryOnError(delayMs: Long, maxAttempts: Int, task: suspend () -> HttpResponse): HttpResponse {
    for (i in 0..maxAttempts) {
        val response = task()

        if (response.status == HttpStatusCode.OK) {
            return response
        }
        delay(delayMs)
    }
    return task()
}