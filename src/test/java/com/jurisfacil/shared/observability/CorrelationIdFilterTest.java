package com.jurisfacil.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void propagatesHeaderToResponseAndMdcThenCleansMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chainThatAssertsMdc("abc123"));

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("abc123");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void generatesSafeIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, chainThatAssertsGeneratedMdc());

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isNotBlank();
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void replacesHeaderContainingControlCharacter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "unsafe\nvalue");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chainThatAssertsGeneratedMdc());

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).matches("[0-9a-f-]{36}");
    }

    private FilterChain chainThatAssertsMdc(String expected) {
        return (request, response) -> assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isEqualTo(expected);
    }

    private FilterChain chainThatAssertsGeneratedMdc() {
        return (request, response) -> assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).matches("[0-9a-f-]{36}");
    }
}
