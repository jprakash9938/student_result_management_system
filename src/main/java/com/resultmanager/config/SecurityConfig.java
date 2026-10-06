package com.resultmanager.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/css/**", "/js/**", "/favicon.ico", "/images/**");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");

        http
            .securityContext(context -> context.requireExplicitSave(false))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(requestHandler)
            )
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // Static public pages
                .requestMatchers("/login.html", "/index.html", "/", "/favicon.ico").permitAll()
                
                // Static assets
                .requestMatchers("/css/**", "/js/**").permitAll()
                
                // Static protected pages (Role-based)
                .requestMatchers("/admin-dashboard.html").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/teacher-dashboard.html", "/subjects.html", "/marks.html", "/upload-excel.html", "/results.html").hasAuthority("ROLE_TEACHER")
                .requestMatchers("/students.html", "/add-student.html").hasAnyAuthority("ROLE_TEACHER", "ROLE_ADMIN")
                .requestMatchers("/student-dashboard.html", "/student-result.html").hasAuthority("ROLE_STUDENT")
                
                // REST API Auth endpoints
                .requestMatchers("/api/auth/me", "/api/auth/login", "/api/auth/logout").permitAll()
                
                // REST APIs role-based
                .requestMatchers("/api/teachers/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/students/**").hasAnyAuthority("ROLE_TEACHER", "ROLE_ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/subjects", "/api/subjects/**").authenticated()
                .requestMatchers("/api/subjects", "/api/subjects/**").hasAuthority("ROLE_TEACHER")
                .requestMatchers("/api/results/student/**").authenticated() // Dynamic logic checks user identity
                .requestMatchers("/api/results/**").hasAuthority("ROLE_TEACHER")
                
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginProcessingUrl("/api/auth/login")
                .successHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"success\":true,\"message\":\"Login successful\"}");
                })
                .failureHandler((request, response, exception) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"success\":false,\"message\":\"Invalid username or password\"}");
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"success\":true,\"message\":\"Logout successful\"}");
                })
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("RESULT_SESSION")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    String uri = request.getRequestURI();
                    if (uri.endsWith(".html") || uri.equals("/")) {
                        response.sendRedirect("/login.html");
                    } else {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"message\":\"Full authentication is required to access this resource.\",\"status\":401}");
                    }
                })
            );

        return http.build();
    }

    /**
     * Filter to force generation and setting of CSRF cookie on requests.
     */
    private static class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken(); // Forces generation of cookie value
            }
            filterChain.doFilter(request, response);
        }
    }
}
