package com.github.senocak.ldap.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.ldap.repository.config.EnableLdapRepositories
import org.springframework.ldap.core.LdapClient
import org.springframework.ldap.core.LdapTemplate
import org.springframework.ldap.core.support.LdapContextSource
import java.util.Locale

@Configuration
@EnableLdapRepositories(basePackages = ["com.github.senocak"])
@ConfigurationProperties(prefix = "spring.ldap")
class LdapConfig(
    //private val ldapProperties: LdapProperties
){
    lateinit var urls: String
    lateinit var base: String
    lateinit var username: String
    lateinit var password: String

    @Bean
    fun ldapContextSource(): LdapContextSource = LdapContextSource()
        .also { lcs: LdapContextSource ->
            lcs.setUrl(urls)
            lcs.setBase(base)
            lcs.userDn = username
            lcs.password = password
            if (urls.lowercase(locale = Locale.getDefault()).contains(other = "ldaps://"))
                lcs.setBaseEnvironmentProperties(
                    mapOf(pair = "java.naming.ldap.factory.socket" to NoCertSSLSocketFactory::class.java.getName())
                )
            lcs.afterPropertiesSet()
        }

    @Bean
    fun ldapTemplate(ldapContextSource: LdapContextSource): LdapTemplate = LdapTemplate(ldapContextSource)

    @Bean
    fun ldapClient(ldapContextSource: LdapContextSource): LdapClient = LdapClient.create(ldapContextSource)
}
