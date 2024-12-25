package com.github.senocak.ldap.domain

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import javax.naming.Name
import javax.naming.ldap.LdapName
import org.springframework.boot.jackson.JsonComponent
import org.springframework.ldap.odm.annotations.Attribute
import org.springframework.ldap.odm.annotations.DnAttribute
import org.springframework.ldap.odm.annotations.Entry
import org.springframework.ldap.odm.annotations.Id

@Entry(objectClasses = ["inetOrgPerson"], base = "ou=users")
class UserModel {
    // https://stackoverflow.com/questions/64129238/spring-data-ldap-not-persisting-user-but-ldap-template-works
    @Id
    var dn: Name? = null

    @Attribute(name = "uid")
    var uid: String? = null

    @Attribute(name = "cn")
    var firstName: String? = null

    @Attribute(name = "sn")
    var lastName: String? = null

    @Attribute(name = "userPassword")
    var userPassword: String? = null

    @Attribute(name = "displayName")
    var displayName: String? = null

    @Attribute(name = "givenName")
    var givenName: String? = null

    @DnAttribute(value = "ou")
    var group: String? = null

    @Attribute(name = "mail")
    var emails: List<String>? = null

    override fun toString(): String =
        "UserModel(dn=$dn, uid=$uid, firstName=$firstName, lastName=$lastName," +
                "userPassword=$userPassword, displayName=$displayName, group=$group," +
                "emails=$emails)"
}

@JsonComponent
class LdapNameSerializer : JsonSerializer<LdapName>() {
    override fun serialize(name: LdapName, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeString(name.toString())
    }
}
