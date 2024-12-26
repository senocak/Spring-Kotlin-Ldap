package com.github.senocak.ldap

import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.senocak.ldap.domain.UserDto
import com.github.senocak.ldap.domain.UserModelRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.hamcrest.core.IsNull
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.assertj.MockMvcTester
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@Tag("integration")
@ActiveProfiles(value = ["integration-test"])
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class LdapApplicationIT {
    @Autowired private lateinit var testRestTemplate: TestRestTemplate
    @Autowired protected lateinit var mockMvc: MockMvc
    @Autowired protected lateinit var mockMvcTester: MockMvcTester
    @Autowired protected lateinit var userModelRepository: UserModelRepository

    companion object {
        private const val OPENLDAP_EXPOSED_PORT: Int = 389
        private const val LDAP_USERNAME: String = "cn=admin,dc=example,dc=org"
        private const val LDAP_ADMIN_PASSWORD: String = "admin"
        private const val LDAP_DIFF: String = "/ldap/ldap-mycompany-com.ldif"

        @Container
        private val openldapContainer: GenericContainer<*> = GenericContainer("osixia/openldap:1.5.0")
            .withNetworkAliases("openldap")
            .withEnv("LDAP_ORGANISATION", "MyCompany Inc.")
            .withEnv("LDAP_BASE_DN", "dc=example,dc=org")
            .withEnv("LDAP_USERNAME", LDAP_USERNAME)
            .withEnv("LDAP_ADMIN_PASSWORD", LDAP_ADMIN_PASSWORD)
            .withExposedPorts(OPENLDAP_EXPOSED_PORT)
            .withCreateContainerCmdModifier { cmd: CreateContainerCmd -> cmd.withName("openldap_${UUID.randomUUID()}") }
            .withFileSystemBind(
                System.getProperty("user.dir") + "/openldap/bootstrap.ldif",
                LDAP_DIFF,
                BindMode.READ_ONLY
            )
            //.withCopyFileToContainer(MountableFile.forClasspathResource("ldap-mycompany-com.ldif", 484),
            //    "/ldap/ldap-mycompany-com.ldif")
            //.withReuse(true)

        init {
            openldapContainer.start()
        }

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            openldapContainer.execInContainer("ldapadd", "-x", "-D", LDAP_USERNAME, "-w", LDAP_ADMIN_PASSWORD, "-H", "ldap://", "-f", LDAP_DIFF)
            // ldapadd -x -D cn=admin,dc=example,dc=org -w admin -H ldap:// -f ldap/ldap-mycompany-com.ldif
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            openldapContainer.stop()
        }

        @JvmStatic
        @DynamicPropertySource
        fun dynamicPropertySource(registry: DynamicPropertyRegistry) {
            registry.add("spring.ldap.urls") { "ldap://localhost:${openldapContainer.getMappedPort(OPENLDAP_EXPOSED_PORT)}" }
            registry.add("spring.ldap.base") { openldapContainer.envMap["LDAP_BASE_DN"] }
            registry.add("spring.ldap.username") { openldapContainer.envMap["LDAP_USERNAME"] }
            registry.add("spring.ldap.password") { openldapContainer.envMap["LDAP_ADMIN_PASSWORD"] }
        }
    }

    @Nested
    inner class GetAll {
        @Test
        fun given_whenGetAll_thenAssertResult() {
            val responseEntity: ResponseEntity<List<UserDto>> =
                testRestTemplate.exchange("/api",HttpMethod.GET, null, object : ParameterizedTypeReference<List<UserDto>>() {})

            assertTrue(responseEntity.statusCode.is2xxSuccessful)
            assertFalse(responseEntity.statusCode.isError)
            assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
            responseEntity.body?.let {
                assertThat(it).isNotNull()
                assertTrue(it.isNotEmpty())
                assertThat(it).hasSize(3)
                assertThat(it[0]).isInstanceOf(UserDto::class.java)
                assertEquals("cn=Amanda,ou=Users", it[0].dn)
                assertEquals("U0", it[0].uid)
                assertEquals("Amanda", it[0].firstName)
                assertEquals("Amanda", it[0].lastName)
                assertTrue(it[0].userPassword!!.isNotEmpty())
                assertEquals("Amanda", it[0].displayName)
                assertEquals("Amanda", it[0].givenName)
                assertNull(it[0].group)
                assertThat(it[0].emails).hasSize(2)
            }
        }
    }

    @Test
    fun givenUid_whenGetUserInfo_thenAssertResult() {
        // Given
        val requestBuilder: MockHttpServletRequestBuilder = MockMvcRequestBuilders.get("/api/U0")
        // testRestTemplate.getForEntity("/api/U0", UserDto::class.java)
        /*
        val result = mockMvcTester.get().uri("/api/{userId}", "U0")
        assertThat(result)
            .isNotNull()
            .hasStatusOk()
            .bodyJson()
            .asInstanceOf(InstanceOfAssertFactories.LIST)
        */

        // When
        val perform: ResultActions = mockMvc.perform(requestBuilder)
        // Then
        perform
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.dn", equalTo("cn=Amanda,ou=Users")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.uid", equalTo("U0")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.firstName", equalTo("Amanda")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.lastName", equalTo("Amanda")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.userPassword", IsNull.notNullValue()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.displayName", equalTo("Amanda")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.givenName", equalTo("Amanda")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.group", equalTo("Users")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.emails", hasSize<Any>(2)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.emails[0]", equalTo("amanda@example.org")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.emails[1]", equalTo("amanda@test.org")))
        /*
        {
          "dn" : "cn=Amanda,ou=Users",
          "uid" : "U0",
          "firstName" : "Amanda",
          "lastName" : "Amanda",
          "userPassword" : "[B@61375dff",
          "displayName" : "Amanda",
          "givenName" : "Amanda",
          "group" : "Users",
          "emails" : [ "amanda@example.org", "amanda@test.org" ]
        }
        */
    }
}