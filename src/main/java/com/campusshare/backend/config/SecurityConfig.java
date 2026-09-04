package com.campusshare.backend.config;

import com.campusshare.backend.filter.JwtRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Autowired
    private JwtRequestFilter jwtRequestFilter;
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 🔴 CHANGE 1: Disable internal CORS so the external Filter can handle it
            .cors(AbstractHttpConfigurer::disable)
            // 🔴 CHANGE 2: Standard way to disable CSRF
            .csrf(AbstractHttpConfigurer::disable)
            
            .authorizeHttpRequests(auth -> auth

                // 🟢 MOVE THIS TO THE TOP
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() 
                
                .requestMatchers("/api/auth/**").permitAll() 
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/items/**").permitAll()
                .anyRequest().authenticated()
            )
            // 🟢 2. ADD THE FILTER TO THE CHAIN (Crucial!)
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    // 🟢 THIS IS THE MISSING PIECE FOR SIGNUP 403
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of(
            "https://unirent-gray.vercel.app",
            "https://unirent-amartya-sens-projects-b10cf8e1.vercel.app",
            "https://unirent-git-main-amartya-sens-projects-b10cf8e1.vercel.app",
            "https://unirent-p6gazn0tg-amartya-sens-projects-b10cf8e1.vercel.app",
            "https://unirent-22w7ctge4-amartya-sens-projects-b10cf8e1.vercel.app",
            "https://unirent-p1ymon7y7-amartya-sens-projects-b10cf8e1.vercel.app"
            "http://localhost:5500",
            "http://127.0.0.1:5500"
        ));
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE); // 🚀 Forces access for Signup
        return bean;
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // The Encryption Tool
    }
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Explicitly tell Java to trust your Vercel Link
        config.setAllowedOrigins(List.of(
            "https://unirent-gray.vercel.app",
            "https://unirent-amartya-sens-projects-b10cf8e1.vercel.app",
            "https://unirent-git-main-amartya-sens-projects-b10cf8e1.vercel.app",
            "https://unirent-p6gazn0tg-amartya-sens-projects-b10cf8e1.vercel.app",
            "https://unirent-22w7ctge4-amartya-sens-projects-b10cf8e1.vercel.app",
            "http://localhost:5500",
            "http://127.0.0.1:5500"
        ));
        
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}








