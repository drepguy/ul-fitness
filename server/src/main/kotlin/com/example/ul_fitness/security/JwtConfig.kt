package com.example.ul_fitness.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

object JwtConfig {
    private val secret: String = System.getenv("JWT_SECRET") ?: "dev-secret-change-me-please-32chars"
    private val algorithm = Algorithm.HMAC256(secret)
    val issuer = "ul-fitness"
    val audience = "ul-fitness-users"
    const val accessExpiresSec = 15 * 60L // 15m
    const val refreshExpiresSec = 30 * 24 * 3600L // 30d
    val verifier = JWT.require(algorithm).withIssuer(issuer).build()

    fun generateAccessToken(userId: Long, email: String): String =
        JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("uid", userId)
            .withClaim("email", email)
            .withExpiresAt(Date(System.currentTimeMillis() + accessExpiresSec * 1000))
            .sign(algorithm)

    fun generateRefreshToken(userId: Long): String =
        JWT.create()
            .withIssuer(issuer)
            .withClaim("uid", userId)
            .withClaim("type", "refresh")
            .withExpiresAt(Date(System.currentTimeMillis() + refreshExpiresSec * 1000))
            .sign(algorithm)

    fun verify(token: String) = verifier.verify(token)
}
