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

## Demo

- List All Users
  ```bash
  curl --location 'http://localhost:8085/api?uid=8c73e44d-7ddc-46c2-b8ad-59561a4e82b2&cn=Amanda&sn=Becca&givenName=Amanda&displayname=Amanda&email=test.org'
  ```

- Find User by Uid
  ```bash
    curl --location 'http://localhost:8085/api/<uid>'
    ```
  
- Create User
  ```bash
    curl --location 'http://localhost:8085/api' \
   --header 'Content-Type: application/json' \
   --data '{
    "firstName": "Anjali",
    "username": "Lynn_Bergstrom95",
    "givenName": "Jada.Schneider",
    "lastName": "Jast",
    "password": "asenocak"
   }'
    ```
  
- Authenticate User
  ```bash
    curl --location 'http://localhost:8085/api/login' \
   --header 'Content-Type: application/json' \
   --data '{
    "uid": "e2c0d42e-c0bf-49c2-9e12-5863307fc9b1",
    "password": "asenocak"
   }'
    ```