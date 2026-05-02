package com.atomiccv.auth.infrastructure

import com.atomiccv.auth.infrastructure.client.CustomOAuth2UserService
import com.atomiccv.auth.infrastructure.client.OAuth2AuthenticationSuccessHandler
import com.atomiccv.auth.interfaces.rest.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val oAuth2AuthenticationSuccessHandler: OAuth2AuthenticationSuccessHandler,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it
                    .requestMatchers(
                        "/oauth2/**",
                        "/login/**",
                        "/actuator/health",
                    ).permitAll()
                it.anyRequest().authenticated()
            }.oauth2Login {
                it.userInfoEndpoint { endpoint -> endpoint.userService(customOAuth2UserService) }
                it.successHandler(oAuth2AuthenticationSuccessHandler)
            }.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
