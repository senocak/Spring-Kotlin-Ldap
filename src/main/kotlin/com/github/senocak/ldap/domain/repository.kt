package com.github.senocak.ldap.domain

import org.springframework.data.ldap.repository.LdapRepository

interface UserModelRepository: LdapRepository<UserModel>