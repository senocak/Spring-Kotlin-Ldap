package com.github.senocak.ldap

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.ldap.core.AttributesMapper
import org.springframework.ldap.core.DirContextOperations
import org.springframework.ldap.core.LdapTemplate
import org.springframework.ldap.core.support.AbstractContextMapper
import org.springframework.ldap.core.support.DefaultIncrementalAttributesMapper
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
@RestController
@RequestMapping("/api")
class LdapApplication(
	private val ldapTemplate: LdapTemplate,
	private val userModelRepository: UserModelRepository
) {
	@GetMapping("/")
	fun getAll(): MutableList<UserModel> = userModelRepository.findAll()

	@GetMapping("/{userId}")
	fun getUserInfo(@PathVariable userId: String): UserModel? = queryWithContextMapper(userId = userId)

	private fun getMultiValuedAttributesWithDefaultIncrementAttributesMapper(dn: String, attr: String): List<String> {
		val attributes = DefaultIncrementalAttributesMapper.lookupAttributes(ldapTemplate, dn, attr)
		val results = ArrayList<String>()
		for (i in 0 until attributes.get(attr).size()) {
			results.add(attributes.get(attr)[i].toString())
		}
		return results
	}

	private fun queryWithAttributeMapper(userId: String): UserModel? {
		val usersFound = ldapTemplate.search("ou=Users", "uid=$userId", AttributesMapper { attributes ->
			//val emails = getMultiValuedAttributesWithDefaultIncrementAttributesMapper("cn=$cn,ou=Users", "mail")
			UserModel()
				.also {
					//it.dn = attributes.get("uid").get().toString()
					it.uid = attributes.get("uid").get().toString()
					it.firstName = attributes.get("cn").get().toString()
					it.lastName = attributes.get("sn").get().toString()
					it.userPassword = attributes.get("userPassword").get().toString()
					it.displayName = attributes.get("displayname").get().toString()
					it.group = attributes.get("ou").get().toString()
					it.emails = attributes.get("mail").all.toList() as List<String>
				}
		})
		return if (usersFound.isNotEmpty()) usersFound[0] else null
	}

	fun queryWithContextMapper(userId: String): UserModel? {
		val usersFound = ldapTemplate.search("ou=Users", "uid=$userId", object: AbstractContextMapper<UserModel>() {
			override fun doMapFromContext(ctx: DirContextOperations): UserModel {
				val dn = ctx.dn
				val emails = getMultiValuedAttributesWithDefaultIncrementAttributesMapper(dn.toString(), "mail")
				println("emails: $emails")
				return UserModel()
					.also {
						it.dn = dn
						it.uid = ctx.getStringAttribute("uid")
						it.firstName = ctx.getStringAttribute("cn")
						it.lastName = ctx.getStringAttribute("sn")
						it.userPassword = ctx.attributes.get("userPassword").get().toString()
						it.displayName = ctx.getStringAttribute("displayname")
						it.group = ctx.getStringAttribute("ou")
						it.emails = ctx.getStringAttributes("mail").toList()
					}
			}
		})
		return if (usersFound.isNotEmpty()) usersFound[0] else null
	}

	//fun create(firstName: String?, username: String?, lastName: String?, password: String?) {
	//	val dn: Name = LdapNameBuilder
	//		.newInstance()
	//		.add("ou", "users")
	//		.add("cn", firstName)
	//		.build()
	//	val context: DirContextAdapter = DirContextAdapter(dn)
	//	val objectClasses = arrayOf("top", "person", "organizationalPerson", "inetOrgPerson")
	//	context.setAttributeValues("objectclass", objectClasses)
	//	context.setAttributeValue("sn", lastName)
	//	context.setAttributeValue("displayName", username)
	//	context.setAttributeValue("userPassword", password)
	//	ldapTemplate.bind(context)
	//}

	//private class LdapUserDetailsMapper : AttributesMapper<LdapUserDetails?> {
	//    @Throws(NamingException::class)
	//    override fun mapFromAttributes(attributes: Attributes): LdapUserDetails {
	//        val essence: LdapUserDetailsImpl.Essence = Essence()
	//        try {
	//            essence.setUsername(attributes.get(userCommanName).get() as String)
	//            essence.setDn(dnPattern + "," + searchBase)
	//            essence.setPassword(userPassword)
	//        } catch (e: Exception) {
	//            log.info("Inside Catch Block of LdapUserDetailsMapper")
	//            e.printStackTrace()
	//        }
	//        essence.setAuthorities(emptyList())
	//        return essence.createUserDetails()
	//    }
	//}


	//@Throws(UsernameNotFoundException::class)
	//fun loadUserByUsername(username: String): UserDetails? {
	//    val filter = AndFilter()
	//    filter.and(EqualsFilter("objectClass", "person")).and(EqualsFilter(userId, username))
	//    val users: List<LdapUserDetails> = ldapTemplate.search(searchBase, filter.encode(), LdapUserDetailsMapper())
	//    if (users.isEmpty()) {
	//        throw UsernameNotFoundException("User not found with username: $username")
	//    }
	//
	//    val user: UserDetails = users[0] as UserDetails
	//    return User(username, user.getPassword(), emptyList())
	//}
}

fun main(args: Array<String>) {
	runApplication<LdapApplication>(*args)
}