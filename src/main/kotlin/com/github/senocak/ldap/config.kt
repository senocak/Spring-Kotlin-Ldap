package com.github.senocak.ldap

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.ldap.repository.config.EnableLdapRepositories
import org.springframework.ldap.core.LdapTemplate
import org.springframework.ldap.core.support.LdapContextSource

@Configuration
@EnableLdapRepositories(basePackages = ["com.github.senocak"])
class Config {
    @Value("\${spring.ldap.urls}")
    var ldapUrl: String? = null

    @Value("\${spring.ldap.base}")
    var base: String? = null

    @Value("\${spring.ldap.username}")
    var userDn: String? = null

    @Value("\${spring.ldap.password}")
    var password: String? = null

    @Bean
    fun ldapContextSource(): LdapContextSource = LdapContextSource()
        .also {
            it.setUrl(ldapUrl)
            it.setBase(base)
            it.userDn = userDn
            it.password = password
            it.afterPropertiesSet()
        }

    @Bean
    fun ldapTemplate(ldapContextSource: LdapContextSource): LdapTemplate = LdapTemplate(ldapContextSource)

    //@Bean
    //fun authenticationManager(): AuthenticationManager {
    //    val contextSource: DefaultSpringSecurityContextSource =
    //        DefaultSpringSecurityContextSource("ldap://localhost:389")
    //    contextSource.setUserDn("cn=admin,dc=example,dc=com")
    //    contextSource.setPassword("password")
    //    contextSource.afterPropertiesSet()
    //    val factory: LdapBindAuthenticationManagerFactory = LdapBindAuthenticationManagerFactory(contextSource)
    //    factory.setUserDnPatterns("cn={0},ou=Users,dc=example,dc=com")
    //    factory.setUserSearchBase("dc=ramhlocal,dc=com")
    //    factory.setUserSearchFilter("cn={0}")
    //    return factory.createAuthenticationManager()
    //}
}
