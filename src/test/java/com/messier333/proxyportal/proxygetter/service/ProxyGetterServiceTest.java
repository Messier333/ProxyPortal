package com.messier333.proxyportal.proxygetter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.messier333.proxyportal.proxygetter.client.NpmClient;
import com.messier333.proxyportal.proxygetter.dto.NpmProxyHostDto;

@ExtendWith(MockitoExtension.class)
class ProxyGetterServiceTest {

    @Mock
    private NpmClient npmClient;

    @InjectMocks
    private ProxyGetterService proxyGetterService;

    @Test
    void getProxyHostsList_shouldFlattenDomainNames() {
        NpmProxyHostDto first = new NpmProxyHostDto();
        ReflectionTestUtils.setField(first, "domain_names", new String[] {"a.example.com", "b.example.com"});
        NpmProxyHostDto second = new NpmProxyHostDto();
        ReflectionTestUtils.setField(second, "domain_names", new String[] {"c.example.com"});
        when(npmClient.getProxyHosts()).thenReturn(List.of(first, second));

        List<String> result = proxyGetterService.getProxyHostsList();

        assertThat(result).containsExactly("a.example.com", "b.example.com", "c.example.com");
    }

    @Test
    void getProxyHostsList_shouldThrowWhenClientThrows() {
        when(npmClient.getProxyHosts()).thenThrow(new RuntimeException("down"));

        assertThatThrownBy(() -> proxyGetterService.getProxyHostsList())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("down");
    }

    @Test
    void getProxyHostsList_shouldIgnoreNullDomainNames() {
        NpmProxyHostDto first = new NpmProxyHostDto();
        ReflectionTestUtils.setField(first, "domain_names", null);
        NpmProxyHostDto second = new NpmProxyHostDto();
        ReflectionTestUtils.setField(second, "domain_names", new String[] {"only.example.com"});
        when(npmClient.getProxyHosts()).thenReturn(List.of(first, second));

        List<String> result = proxyGetterService.getProxyHostsList();

        assertThat(result).containsExactly("only.example.com");
    }
}
