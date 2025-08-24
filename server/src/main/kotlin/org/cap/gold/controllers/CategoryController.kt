package org.cap.gold.controllers

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.cap.gold.repositories.CategoryRepository
import java.util.UUID
import kotlinx.serialization.Serializable

class CategoryController(
    private val categoryRepository: CategoryRepository
) {
    fun Route.categoryRoutes() {
        // DTOs
        @Serializable
        data class CreateCategoryRequest(val name: String)

        route("/category") {
            // GET /category/all
            get("/all") {
                val items = categoryRepository.getAll()
                call.respond(items)
            }

            // POST /category/create
            post("/create") {
                val req = call.receive<CreateCategoryRequest>()
                val existing = categoryRepository.findByName(req.name.trim())
                if (existing != null) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Category exists"))
                    return@post
                }
                val created = categoryRepository.create(req.name.trim())
                call.respond(HttpStatusCode.Created, created)
            }

            // DELETE /category/delete?id=<uuid>
            delete("/delete") {
                val idParam = call.request.queryParameters["id"]
                val id = try {
                    UUID.fromString(idParam)
                } catch (e: Exception) {
                    null
                }
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid or missing id"))
                    return@delete
                }
                val ok = categoryRepository.delete(id)
                if (ok) {
                    call.respond(HttpStatusCode.OK, mapOf("deleted" to true))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Category not found"))
                }
            }
        }
    }
}
