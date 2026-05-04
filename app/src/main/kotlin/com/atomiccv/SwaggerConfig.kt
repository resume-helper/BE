package com.atomiccv

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {
    @Bean
    fun openAPI(): OpenAPI {
        val info =
            Info()
                .title("Atomic CV API")
                .description("Atomic CV 백엔드 API 명세서")
                .version("v1.0")
                .contact(Contact().name("Atomic CV Team"))

        val accessTokenScheme =
            SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .`in`(SecurityScheme.In.COOKIE)
                .name("access_token")
                .description("JWT Access Token (HttpOnly Cookie)")

        val refreshTokenScheme =
            SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .`in`(SecurityScheme.In.COOKIE)
                .name("refresh_token")
                .description("JWT Refresh Token (HttpOnly Cookie, Path=/api/auth/refresh)")

        val components =
            Components()
                .addSecuritySchemes(ACCESS_TOKEN_COOKIE, accessTokenScheme)
                .addSecuritySchemes(REFRESH_TOKEN_COOKIE, refreshTokenScheme)

        return OpenAPI()
            .info(info)
            .components(components)
            .addSecurityItem(SecurityRequirement().addList(ACCESS_TOKEN_COOKIE))
    }

    companion object {
        const val ACCESS_TOKEN_COOKIE = "access_token_cookie"
        const val REFRESH_TOKEN_COOKIE = "refresh_token_cookie"
    }
}
