package org.cap.gold.controllers

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.cap.gold.repositories.AboutUsRepository

class AboutUsController(
    private val aboutUsRepository: AboutUsRepository
) {
    @Serializable
    data class AboutUsPayload(val content: String)

    fun Route.aboutUsRoutes() {
        route("/aboutus") {
            // Get current About Us content
            get {
                val content = aboutUsRepository.getContent()
                call.respond(AboutUsPayload(content))
            }
            // Set About Us content (replace)
            post {
                // Support either JSON {"content":"..."} or raw text/plain body
                val contentType = call.request.contentType()
                val content = if (contentType.match(ContentType.Application.Json)) {
                    val payload = call.receive<AboutUsPayload>()
                    payload.content
                } else {
                    call.receiveText()
                }
                aboutUsRepository.setContent(content)
                call.respond(HttpStatusCode.OK, AboutUsPayload(content))
            }
        }
    }
}
