package com.melearning.elearning.config;

import com.melearning.elearning.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    // application-dev.properties-ben true, application-prod.properties-ben false
    @Value("${h2.console.security.enabled:false}")
    private boolean h2ConsoleEnabled;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .userDetailsService(userDetailsService)
                .authorizeHttpRequests(authz -> {
                    // H2 konzol: csak dev módban és csak ADMIN-nek
                    if (h2ConsoleEnabled) {
                        authz.requestMatchers("/h2-console/**").hasRole("ADMIN");
                    }
                    authz
                            .requestMatchers("/", "/login", "/register",
                                    "/css/**", "/js/**", "/images/**").permitAll()
                            .requestMatchers("/courses/create", "/courses/*/manage",
                                    "/courses/*/add-student", "/courses/*/remove-student")
                            .hasAnyRole("INSTRUCTOR", "ADMIN")
                            .requestMatchers("/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated();
                })
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll()
                );

        // H2 konzol CSRF és frame kivétel csak dev módban
        if (h2ConsoleEnabled) {
            http
                    .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                    .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        }

        return http.build();
    }
}