package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.models.UserCreateRequest
import com.sangita.grantha.backend.api.models.UserUpdateRequest
import com.sangita.grantha.backend.api.models.AssignRoleRequest
import com.sangita.grantha.backend.api.services.UserManagementService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.userManagementRoutes(userService: UserManagementService) {
    route("/v1/admin/users") {
        get {
            val users = userService.listUsers()
            call.respond(users)
        }
        
        get("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "userId")
                ?: return@get call.respondText("Missing user ID", status = HttpStatusCode.BadRequest)
            val user = userService.getUser(id)
            if (user == null) {
                call.respondText("User not found", status = HttpStatusCode.NotFound)
            } else {
                call.respond(user)
            }
        }
        
        post {
            val request = call.receive<UserCreateRequest>()
            val created = userService.createUser(request)
            call.respond(HttpStatusCode.Created, created)
        }
        
        put("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "userId")
                ?: return@put call.respondText("Missing user ID", status = HttpStatusCode.BadRequest)
            val request = call.receive<UserUpdateRequest>()
            val updated = userService.updateUser(id, request)
            if (updated == null) {
                call.respondText("User not found", status = HttpStatusCode.NotFound)
            } else {
                call.respond(updated)
            }
        }
        
        delete("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "userId")
                ?: return@delete call.respondText("Missing user ID", status = HttpStatusCode.BadRequest)
            val deleted = userService.deleteUser(id)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respondText("User not found", status = HttpStatusCode.NotFound)
            }
        }
        
        // Role assignment routes
        post("/{id}/roles") {
            val id = parseUuidParam(call.parameters["id"], "userId")
                ?: return@post call.respondText("Missing user ID", status = HttpStatusCode.BadRequest)
            val request = call.receive<AssignRoleRequest>()
            userService.assignRole(id, request.roleCode)
            call.respond(HttpStatusCode.NoContent)
        }
        
        delete("/{id}/roles/{roleCode}") {
            val id = parseUuidParam(call.parameters["id"], "userId")
                ?: return@delete call.respondText("Missing user ID", status = HttpStatusCode.BadRequest)
            val roleCode = call.parameters["roleCode"]
                ?: return@delete call.respondText("Missing role code", status = HttpStatusCode.BadRequest)
            val removed = userService.removeRole(id, roleCode)
            if (removed) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respondText("Role assignment not found", status = HttpStatusCode.NotFound)
            }
        }
        
        get("/{id}/roles") {
            val id = parseUuidParam(call.parameters["id"], "userId")
                ?: return@get call.respondText("Missing user ID", status = HttpStatusCode.BadRequest)
            val roles = userService.getUserRoles(id)
            call.respond(roles)
        }
    }
}

