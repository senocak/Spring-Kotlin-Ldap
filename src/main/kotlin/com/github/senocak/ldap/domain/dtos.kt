package com.github.senocak.ldap.domain

data class UserDto(
    var dn: String? = null,
    var uid: String? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var userPassword: String? = null,
    var displayName: String? = null,
    var givenName: String? = null,
    var group: String? = null,
    var emails: List<String>? = null
)

data class CreateRequest(
    var firstName: String? = null,
    var username: String? = null,
    var lastName: String? = null,
    var password: String? = null,
    var givenName: String? = null,
)

data class LoginRequest(
    var uid: String? = null,
    var password: String? = null,
)

data class ExceptionResponse(
    var message: String? = null,
    var error: String? = null,
)
