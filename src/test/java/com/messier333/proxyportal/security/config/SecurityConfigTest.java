package com.messier333.proxyportal.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import com.messier333.proxyportal.security.filter.LoginAttemptFilter;
import com.messier333.proxyportal.security.handler.LoginFailureHandler;
import com.messier333.proxyportal.user.service.CustomUserDetailService;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private CustomUserDetailService customUserDetailService;

    @Mock
    private LoginAttemptFilter loginAttemptFilter;

    @Mock
    private LoginFailureHandler loginFailureHandler;

    @Mock
    private DataSource dataSource;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData databaseMetaData;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(customUserDetailService, loginAttemptFilter, loginFailureHandler);
    }

    @Test
    void springSessionTableInitializer_shouldUseByteaForPostgres() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("PostgreSQL 16");

        ApplicationRunner runner = securityConfig.springSessionTableInitializer(dataSource, jdbcTemplate);
        runner.run(null);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(5)).execute(sqlCaptor.capture());
        List<String> executedSql = sqlCaptor.getAllValues();
        assertThat(executedSql)
                .anyMatch(sql -> sql.contains("ATTRIBUTE_BYTES BYTEA"))
                .noneMatch(sql -> sql.contains("ATTRIBUTE_BYTES BLOB"));
        verify(connection).close();
    }

    @Test
    void springSessionTableInitializer_shouldUseBlobForNonPostgres() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("H2");

        ApplicationRunner runner = securityConfig.springSessionTableInitializer(dataSource, jdbcTemplate);
        runner.run(null);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(5)).execute(sqlCaptor.capture());
        List<String> executedSql = sqlCaptor.getAllValues();
        assertThat(executedSql)
                .anyMatch(sql -> sql.contains("ATTRIBUTE_BYTES BLOB"))
                .noneMatch(sql -> sql.contains("ATTRIBUTE_BYTES BYTEA"));
        verify(connection).close();
    }

    @Test
    void springSessionTableInitializer_shouldThrowWhenDatasourceMetadataLookupFails() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("boom"));

        ApplicationRunner runner = securityConfig.springSessionTableInitializer(dataSource, jdbcTemplate);

        assertThatThrownBy(() -> runner.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to resolve datasource type for session table schema")
                .hasRootCauseMessage("boom");
        verify(jdbcTemplate, never()).execute(org.mockito.ArgumentMatchers.anyString());
    }
}
