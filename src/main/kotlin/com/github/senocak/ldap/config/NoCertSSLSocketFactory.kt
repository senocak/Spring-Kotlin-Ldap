package com.github.senocak.ldap.config

import java.net.InetAddress
import java.net.Socket
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.SocketFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class NoCertSSLSocketFactory : SSLSocketFactory() {
    private val socketFactory: SSLSocketFactory

    init {
        try {
            val sslContext: SSLContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(xcs: Array<X509Certificate>, string: String) {}
                override fun checkServerTrusted(xcs: Array<X509Certificate>, string: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate>? = null
            }), SecureRandom())
            socketFactory = sslContext.socketFactory
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
    override fun getDefaultCipherSuites(): Array<String> = socketFactory.defaultCipherSuites
    override fun getSupportedCipherSuites(): Array<String> = socketFactory.supportedCipherSuites
    override fun createSocket(socket: Socket, string: String, i: Int, bln: Boolean): Socket = socketFactory.createSocket(socket, string, i, bln)
    override fun createSocket(string: String, i: Int): Socket = socketFactory.createSocket(string, i)
    override fun createSocket(string: String, i: Int, ia: InetAddress, i1: Int): Socket = socketFactory.createSocket(string, i, ia, i1)
    override fun createSocket(ia: InetAddress, i: Int): Socket = socketFactory.createSocket(ia, i)
    override fun createSocket(ia: InetAddress, i: Int, ia1: InetAddress, i1: Int): Socket = socketFactory.createSocket(ia, i, ia1, i1)
    override fun createSocket(): Socket = socketFactory.createSocket()

    companion object {
        @get:Suppress("unused")
        val default: SocketFactory
            get() = NoCertSSLSocketFactory()
    }
}