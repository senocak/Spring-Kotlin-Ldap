package com.github.senocak.ldap

import com.github.senocak.ldap.domain.CreateRequest
import com.github.senocak.ldap.domain.ExceptionResponse
import com.github.senocak.ldap.domain.LoginRequest
import com.github.senocak.ldap.domain.UserDto
import com.github.senocak.ldap.domain.UserModel
import com.github.senocak.ldap.domain.UserModelRepository
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.HttpStatus
import org.springframework.ldap.core.AttributesMapper
import org.springframework.ldap.core.CollectingAuthenticationErrorCallback
import org.springframework.ldap.core.DirContextAdapter
import org.springframework.ldap.core.DirContextOperations
import org.springframework.ldap.core.LdapClient
import org.springframework.ldap.core.LdapTemplate
import org.springframework.ldap.core.support.AbstractContextMapper
import org.springframework.ldap.core.support.DefaultIncrementalAttributesMapper
import org.springframework.ldap.filter.AndFilter
import org.springframework.ldap.filter.EqualsFilter
import org.springframework.ldap.query.ContainerCriteria
import org.springframework.ldap.query.LdapQueryBuilder
import org.springframework.ldap.support.LdapNameBuilder
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.UUID
import javax.naming.AuthenticationException
import javax.naming.Name
import javax.naming.NameAlreadyBoundException
import javax.naming.directory.Attributes

fun main(args: Array<String>) {
	runApplication<LdapApplication>(*args)
}

@SpringBootApplication
@RestController
@RequestMapping("/api")
@RestControllerAdvice
class LdapApplication(
	private val ldapTemplate: LdapTemplate,
	private val ldapClient: LdapClient,
	private val userModelRepository: UserModelRepository
) {
	@GetMapping
	fun getAll(
		@RequestParam(required = false) uid: String?,
		@RequestParam(required = false) cn: String?, // first name
		@RequestParam(required = false) sn: String?, // last name
		@RequestParam(required = false) givenName: String?,
		@RequestParam(required = false) displayName: String?,
		@RequestParam(required = false) email: String?
	): List<UserDto> {
		var criteria: ContainerCriteria = LdapQueryBuilder.query().where("objectclass").`is`("inetOrgPerson")
		uid?.let { criteria = criteria.and(LdapQueryBuilder.query().where("uid").`is`(it)) }
		cn?.let { criteria = criteria.and(LdapQueryBuilder.query().where("cn").whitespaceWildcardsLike(it)) }
		sn?.let { criteria = criteria.and(LdapQueryBuilder.query().where("sn").whitespaceWildcardsLike(it)) }
		givenName?.let { criteria = criteria.and(LdapQueryBuilder.query().where("givenName").whitespaceWildcardsLike(it)) }
		displayName?.let { criteria = criteria.and(LdapQueryBuilder.query().where("displayname").`is`(it)) }
		email?.let { criteria = criteria.and(LdapQueryBuilder.query().where("mail").whitespaceWildcardsLike(it)) }
		val findAll = userModelRepository.findAll(criteria)//.map { it: UserModel -> it.userPassword = "***"; it}
		return ldapClient.search().query(criteria).toList(object: AbstractContextMapper<UserDto>() {
			override fun doMapFromContext(ctx: DirContextOperations): UserDto {
				val dn: Name = ctx.dn
				return UserDto(
						dn = dn.toString(),
						uid = ctx.getStringAttribute("uid"),
						firstName = ctx.getStringAttribute("cn"),
						lastName = ctx.getStringAttribute("sn"),
						givenName = ctx.getStringAttribute("givenname"),
						displayName = ctx.getStringAttribute("displayname"),
						userPassword = ctx.attributes.get("userpassword").get().toString(),
						group = ctx.getStringAttribute("ou"),
						emails = ctx.getObjectAttributes("mail")?.map { m -> "$m" }
						//it.emails = getMultiValuedAttributesWithDefaultIncrementAttributesMapper(dn.toString(), "mail")
					)
			}
		})
	}

	@GetMapping("/{userId}")
	fun getUserInfo(@PathVariable userId: String): UserDto? {
		// base: LdapUtils.emptyLdapName()
		val usersFound = ldapTemplate.search("ou=Users", "uid=$userId", object: AbstractContextMapper<UserDto>() {
			override fun doMapFromContext(ctx: DirContextOperations): UserDto {
				val dn: Name = ctx.dn
				return UserDto(
					dn = "$dn",
					uid = ctx.getStringAttribute("uid"),
					firstName = ctx.getStringAttribute("cn"),
					lastName = ctx.getStringAttribute("sn"),
					givenName = ctx.getStringAttribute("givenname"),
					displayName = ctx.getStringAttribute("displayname"),
					userPassword = ctx.attributes.get("userpassword").get().toString(),
					group = "Users",
					emails = getMultiValuedAttributesWithDefaultIncrementAttributesMapper(dn.toString(), "mail")
				)
			}
		})
		queryWithAttributeMapper(userId)
		return usersFound?.get(0)
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	fun create(@RequestBody createRequest: CreateRequest) {
		val dn: Name = LdapNameBuilder
			.newInstance()
			.add("ou", "users")
			.add("cn", createRequest.firstName)
			.build()
		val context: DirContextAdapter = DirContextAdapter(dn)
			.also { c: DirContextAdapter ->
				c.setAttributeValues("objectclass", arrayOf("top", "person", "organizationalPerson", "inetOrgPerson"))
				c.setAttributeValue("uid", UUID.randomUUID().toString())
				c.setAttributeValue("cn", createRequest.firstName)
				c.setAttributeValue("sn", createRequest.lastName)
				c.setAttributeValue("displayName", createRequest.username)
				c.setAttributeValue("userPassword", createRequest.password)
				c.setAttributeValue("givenName", createRequest.givenName)
			}
		ldapTemplate.bind(context)
	}

	@PostMapping("/login")
	fun login(@RequestBody loginRequest: LoginRequest) {
		// https://docs.spring.io/spring-ldap/docs/1.3.2.RELEASE/reference/html/user-authentication.html
		val authErrorCallback = CollectingAuthenticationErrorCallback()
		val authenticate: Boolean = ldapTemplate.authenticate("ou=Users", "(uid=${loginRequest.uid})",
			loginRequest.password, authErrorCallback)
		if (!authenticate || authErrorCallback.error != null)
			throw AuthenticationException(authErrorCallback.error.message)

	}

	private fun getMultiValuedAttributesWithDefaultIncrementAttributesMapper(dn: String, attr: String): List<String> {
		val attributes: Attributes = DefaultIncrementalAttributesMapper.lookupAttributes(ldapTemplate, dn, attr)
		return (0 until attributes.get(attr).size()).map { attributes.get(attr)[it].toString() }
	}

	private fun queryWithAttributeMapper(userId: String): UserModel {
		val encode: String = AndFilter()
			.and(EqualsFilter("objectClass", "inetOrgPerson"))
			.and(EqualsFilter("uid", userId))
			.encode() // "uid=$userId"
		val usersFound: MutableList<UserModel> = ldapTemplate.search("ou=Users", encode,6,
			arrayOf("uid", "cn", "sn", "userPassword", "displayName", "givenName", "mail"),
			AttributesMapper { attributes ->
				UserModel()
					.also { it ->
						it.uid = "${attributes.get("uid").get()}"
						it.firstName = "${attributes.get("cn").get()}"
						it.lastName = "${attributes.get("sn").get()}"
						it.userPassword = "***" //attributes.get("userPassword").get().toString()
						it.displayName = "${attributes.get("displayname").get()}"
						it.givenName = "${attributes.get("givenname").get()}"
						it.group = "Users"
						it.emails = attributes.get("mail")?.all?.toList()?.map { m: Any -> "$m" }
					}
			}
		)
		return usersFound[0]
	}

	@ExceptionHandler
	@ResponseStatus(HttpStatus.CONFLICT)
	fun handleNameAlreadyBoundException(ex: NameAlreadyBoundException): ExceptionResponse =
		ExceptionResponse(message = ex.message ?: "An error occurred", error = HttpStatus.CONFLICT.reasonPhrase)

	@ExceptionHandler
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	fun handleAuthenticationException(ex: AuthenticationException): ExceptionResponse =
		ExceptionResponse(message = ex.message ?: "An error occurred", error = HttpStatus.UNAUTHORIZED.reasonPhrase)
}
