package com.leagueoflocals.leagueoflocals_user_api.client

import com.leagueoflocals.leagueoflocals_user_api.service.Auth0TokenService
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.given
import org.springframework.web.reactive.function.client.WebClient

@ExtendWith(MockitoExtension::class)
class Auth0ManagementClientTest {

    @Mock
    private lateinit var mockTokenService: Auth0TokenService

    private lateinit var mockWebServer: MockWebServer
    private lateinit var managementClient: Auth0ManagementClient

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val domain = "${mockWebServer.url("/").host}:${mockWebServer.url("/").port}"
        val scheme = "http"

        managementClient = Auth0ManagementClient(
            tokenService = mockTokenService,
            domain = domain,
            scheme = scheme,
            clientId = "client-id",
            clientSecret = "client-secret",
            webClientBuilder = WebClient.builder().baseUrl("$scheme://$domain")
        )
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `createUser should successfully create and return a new user`() {

        val fakeToken = "test-access-token"
        given(mockTokenService.getManagementApiToken()).willReturn(fakeToken)

        val mockAuth0UserResponse = """
            {
                "user_id": "auth0|newuser123",
                "email": "test@example.com",
                "nickname": "newrunner"
            }
        """.trimIndent()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(201) // 201 Created
                .setHeader("Content-Type", "application/json")
                .setBody(mockAuth0UserResponse)
        )

        val result = managementClient.createUser(
            email = "test@example.com",
            password = "password123!",
            username = "newrunner"
        )

        assertThat(result.user_id).isEqualTo("auth0|newuser123")
        assertThat(result.nickname).isEqualTo("newrunner")

        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.path).isEqualTo("/api/v2/users")
        assertThat(recordedRequest.method).isEqualTo("POST")
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer $fakeToken")
        assertThat(recordedRequest.body.readUtf8()).contains("\"nickname\":\"newrunner\"")
    }

    @Test
    fun `deleteUser should successfully delete existing user from Auth0 database` () {
        val fakeToken = "test-access-token"
        given(mockTokenService.getManagementApiToken()).willReturn(fakeToken)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(204) // 201 Created
                .setHeader("Content-Type", "application/json")
        )

        managementClient.deleteUser("auth0UserId")

        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.path).isEqualTo("/api/v2/users/auth0UserId")
        assertThat(recordedRequest.method).isEqualTo("DELETE")
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer $fakeToken")
    }
}