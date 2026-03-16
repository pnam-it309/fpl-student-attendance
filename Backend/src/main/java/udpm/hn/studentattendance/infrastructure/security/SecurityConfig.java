package udpm.hn.studentattendance.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import udpm.hn.studentattendance.infrastructure.constants.RoutesConstant;
import udpm.hn.studentattendance.infrastructure.security.router.AdminSecurityConfig;
import udpm.hn.studentattendance.infrastructure.security.router.AuthenticationSecurityConfig;
import udpm.hn.studentattendance.infrastructure.security.exception.CustomAccessDeniedHandler;
import udpm.hn.studentattendance.infrastructure.security.exception.CustomAuthenticationEntryPoint;
import udpm.hn.studentattendance.infrastructure.security.router.ExcelSecurityConfig;
import udpm.hn.studentattendance.infrastructure.security.router.StaffSecurityConfig;
import udpm.hn.studentattendance.infrastructure.security.router.StudentSecurityConfig;
import udpm.hn.studentattendance.infrastructure.security.router.TeacherSecurityConfig;
import udpm.hn.studentattendance.infrastructure.security.router.TestRedisSecurityConfig;

import java.util.Collections;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    private final AuthenticationSecurityConfig authenticationSecurityConfig;

    private final StaffSecurityConfig staffSecurityConfig;

    private final AdminSecurityConfig adminSecurityConfig;

    private final StudentSecurityConfig studentSecurityConfig;

    private final TeacherSecurityConfig teacherSecurityConfig;

    private final ExcelSecurityConfig excelSecurityConfig;

    private final TestRedisSecurityConfig testSecurityConfig;

    @Value("${allowed.origin}")
    public String ALLOWED_ORIGIN;

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        if (ALLOWED_ORIGIN != null && !ALLOWED_ORIGIN.equals("*")) {
            config.setAllowedOrigins(List.of(ALLOWED_ORIGIN.split(",")));
            config.setAllowCredentials(true);
        } else {
            config.setAllowedOrigins(List.of("*"));
            config.setAllowCredentials(false);
        }
        
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(c -> c.configurationSource(corsConfigurationSource()));
        http.csrf(AbstractHttpConfigurer::disable);
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.exceptionHandling(e -> {
            e.accessDeniedHandler(new CustomAccessDeniedHandler());
            e.authenticationEntryPoint(new CustomAuthenticationEntryPoint());
        });

        // Cấu hình cho các endpoint test phải được thêm trước các cấu hình khác
        testSecurityConfig.configure(http);

        // Thêm từng config routes vào đây
        authenticationSecurityConfig.configure(http);
        staffSecurityConfig.configure(http);
        adminSecurityConfig.configure(http);
        studentSecurityConfig.configure(http);
        teacherSecurityConfig.configure(http);
        excelSecurityConfig.configure(http);

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(RoutesConstant.API_PREFIX + "/**").authenticated()
                .anyRequest().permitAll());
        return http.build();
    }

}