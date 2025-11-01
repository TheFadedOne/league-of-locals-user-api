package com.leagueoflocals.leagueoflocals_user_api.client

import com.leagueoflocals.leagueoflocals_user_api.service.Auth0TokenService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class Auth0ManagementClient(

    private val tokenService: Auth0TokenService,

    @Value("\${auth0.domain}") private val domain: String,
    @Value("\${auth0.client-id}") private val clientId: String,
    @Value("\${auth0.client-secret}") private val clientSecret: String,
    webClientBuilder: WebClient.Builder,
    private val scheme: String = "https"
) {
    private val webClient = webClientBuilder.build();

    fun createUser(email: String, password: String, username: String): Auth0User {
        val token = tokenService.getManagementApiToken()
        val usersUrl = "$scheme://$domain/api/v2/users"

        val request = Auth0CreateUserRequest(
            email = email,
            password = password,
            connection = "Username-Password-Authentication",
            nickname = username
        )

        return webClient.post()
            .uri(usersUrl)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono<Auth0User>()
            .block() ?: throw IllegalStateException("Failed to create user in Auth0")
    }

    fun deleteUser(auth0UserId: String): Void? {
        val token = tokenService.getManagementApiToken()
        val deleteUserUrl = "$scheme://$domain/api/v2/users/$auth0UserId"

        return webClient.delete()
            .uri(deleteUserUrl)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .bodyToMono<Void>()
            .block()
    }
}

private data class Auth0CreateUserRequest(val email: String, val password: String, val connection: String, val nickname: String)
data class Auth0User(val user_id: String, val email: String, val nickname: String)