package com.messier333.proxyportal.security.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.SecurityFilterChain;

import com.messier333.proxyportal.security.filter.LoginAttemptFilter;
import com.messier333.proxyportal.security.handler.LoginFailureHandler;
import com.messier333.proxyportal.user.service.CustomUserDetailService;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private static final String CREATE_SPRING_SESSION_ATTRIBUTES_TABLE_BLOB = """
            CREATE TABLE IF NOT EXISTS SPRING_SESSION_ATTRIBUTES (
                SESSION_PRIMARY_ID CHAR(36) NOT NULL,
                ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
                ATTRIBUTE_BYTES BLOB NOT NULL,
                CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
                CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID)
                    REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
            )
            """;

    private static final String CREATE_SPRING_SESSION_ATTRIBUTES_TABLE_BYTEA = """
            CREATE TABLE IF NOT EXISTS SPRING_SESSION_ATTRIBUTES (
                SESSION_PRIMARY_ID CHAR(36) NOT NULL,
                ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
                ATTRIBUTE_BYTES BYTEA NOT NULL,
                CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
                CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID)
                    REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
            )
            """;

    private final CustomUserDetailService customUserDetailService;
    private final LoginAttemptFilter loginAttemptFilter;
    private final LoginFailureHandler loginFailureHandler;

    @Value("${app.security.remember-me.key}")
    private String rememberMeKey;

    @Value("${app.security.remember-me.token-validity-seconds}")
    private int rememberMeTokenValiditySeconds;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, PersistentTokenRepository tokenRepository) throws Exception {
        http
                .userDetailsService(customUserDetailService)
                .authorizeHttpRequests(auth -> auth
                                .requestMatchers("/login", "/login/**", "/setup", "/setup/**", "/error").permitAll()
                                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                                .requestMatchers("/dashboard/account/**").hasRole("ADMIN")
                                .anyRequest().authenticated()
                )
                .formLogin(form -> form
                                .loginPage("/login")
                                .failureHandler(loginFailureHandler)
                                .permitAll()
                )
                .rememberMe(rememberMe -> rememberMe
                                .key(rememberMeKey)
                                .rememberMeParameter("remember-me")
                                .tokenValiditySeconds(rememberMeTokenValiditySeconds)
                                .useSecureCookie(true)
                                .userDetailsService(customUserDetailService)
                                .tokenRepository(tokenRepository)
                )
                .logout(logout -> logout
                                .logoutUrl("/logout")
                                .logoutSuccessUrl("/login?logout")
                                .permitAll()
                )
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                )
                .addFilterBefore(loginAttemptFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CookieSameSiteSupplier rememberMeCookieSameSiteSupplier() {
        return CookieSameSiteSupplier.ofLax().whenHasName("remember-me");
    }

    @Bean
    public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);
        return tokenRepository;
    }

    @Bean
    @Profile("!prod")
    public ApplicationRunner persistentLoginsTableInitializer(JdbcTemplate jdbcTemplate) {
        return args -> jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS persistent_logins (
                    username VARCHAR(64) NOT NULL,
                    series VARCHAR(64) PRIMARY KEY,
                    token VARCHAR(64) NOT NULL,
                    last_used TIMESTAMP NOT NULL
                )
                """);
    }

    @Bean
    @Profile("!prod")
    public ApplicationRunner springSessionTableInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        return args -> {
            String createAttributesTableSql = isPostgres(dataSource)
                    ? CREATE_SPRING_SESSION_ATTRIBUTES_TABLE_BYTEA
                    : CREATE_SPRING_SESSION_ATTRIBUTES_TABLE_BLOB;

            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS SPRING_SESSION (
                        PRIMARY_ID CHAR(36) NOT NULL,
                        SESSION_ID CHAR(36) NOT NULL,
                        CREATION_TIME BIGINT NOT NULL,
                        LAST_ACCESS_TIME BIGINT NOT NULL,
                        MAX_INACTIVE_INTERVAL INT NOT NULL,
                        EXPIRY_TIME BIGINT NOT NULL,
                        PRINCIPAL_NAME VARCHAR(100),
                        CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
                    )
                    """);
            jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME)");
            jdbcTemplate.execute(createAttributesTableSql);
        };
    }

    private boolean isPostgres(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            return productName != null && productName.toLowerCase(Locale.ROOT).contains("postgres");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to resolve datasource type for session table schema", e);
        }
    }
}
