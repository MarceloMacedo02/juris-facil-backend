package com.jurisfacil.shared.config;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jurisfacil.shared.security.RateLimitConfig;
import com.jurisfacil.shared.security.RateLimitFilter;
import com.jurisfacil.shared.tenant.TenantFilter;
import com.jurisfacil.iam.security.JwtAuthFilter;
import com.jurisfacil.iam.security.JwtService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(RateLimitConfig.class)
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain workspaceChain(
            HttpSecurity http,
            CorsConfigurationSource corsSource,
            RateLimitFilter rateLimitFilter,
            JwtService jwtService,
            ObjectProvider<TenantFilter> tenantFilterProvider) throws Exception {
        http
                .securityMatcher("/api/auth/**", "/api/v1/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/**", "/api/v1/auth/accept-invite", "/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JwtAuthFilter(jwtService, JwtAuthFilter.Surface.WORKSPACE), AuthorizationFilter.class)
                .addFilterAfter(rateLimitFilter, AuthorizationFilter.class)
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
                        (request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)));
        TenantFilter tenantFilter = tenantFilterProvider.getIfAvailable();
        if (tenantFilter != null) {
            http.addFilterAfter(tenantFilter, JwtAuthFilter.class);
        }
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain adminChain(
            HttpSecurity http,
            CorsConfigurationSource corsSource,
            RateLimitFilter rateLimitFilter,
            JwtService jwtService,
            ObjectProvider<TenantFilter> tenantFilterProvider) throws Exception {
        http
                .securityMatcher("/api/admin/**", "/api/admin/v1/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/admin/v1/auth/login").permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterAfter(rateLimitFilter, AuthorizationFilter.class)
                .addFilterBefore(new JwtAuthFilter(jwtService, JwtAuthFilter.Surface.ADMIN), AuthorizationFilter.class)
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
                        (request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)));
        TenantFilter tenantFilter = tenantFilterProvider.getIfAvailable();
        if (tenantFilter != null) {
            http.addFilterAfter(tenantFilter, JwtAuthFilter.class);
        }
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("X-Correlation-Id"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public RateLimitFilter rateLimitFilter(RateLimitConfig rateLimitConfig, ObjectMapper objectMapper) {
        return new RateLimitFilter(rateLimitConfig, objectMapper);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Profile({"dev", "prod"})
    public TenantFilter tenantFilter() {
        return new TenantFilter();
    }
}
