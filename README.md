# SpringBoot LDAP
This is an exploration/demo project on how to use LdapTemplate to craft LDAP Queries to retrieve information from the

## Overview
This project populates the LDAP server (AD) with mock user data. The Spring Boot project provides sample codes on how to
craft LDAP queries with LdapTemplate.

## Usage
1. Start OpenLdap Server

   ```bash
   docker-compose -f ./openldap/docker-compose.yml up -d
   ```

   This will create the ldap server (localhost:389) as well as a web interface (localhost:3800) for interacting with the
   ldap server. The login credential is user = `cn=admin,dc=example,dc=org`, pass = `admin`.

2. Start the Spring Boot application

   ```bash
   ./gradlew bootrun
   ```
