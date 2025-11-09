package com.codevision.codevisionbackend.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.codevision.codevisionbackend.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiKeyFilterTest {

    private SecurityProperties securityProperties;
    private ApiKeyFilter filter;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties();
        filter = new ApiKeyFilter(securityProperties);
    }

    @Test
    void allowsTrafficWhenApiKeyDisabled() throws ServletException, IOException {
        securityProperties.setApiKey(null);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/analyze");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TrackingChain chain = new TrackingChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(chain.invoked).isTrue();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void rejectsRequestsWithMissingOrInvalidKey() throws ServletException, IOException {
        securityProperties.setApiKey("super-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/analyze");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TrackingChain chain = new TrackingChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(chain.invoked).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getContentAsString()).isEqualTo("Unauthorized");
    }

    @Test
    void permitsRequestsWithMatchingHeader() throws ServletException, IOException {
        securityProperties.setApiKey("super-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/analyze");
        request.addHeader("X-API-KEY", "super-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TrackingChain chain = new TrackingChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(chain.invoked).isTrue();
    }

    @Test
    void shouldNotFilterSkipsGetAndActuatorRequests() {
        securityProperties.setApiKey("secret");
        MockHttpServletRequest getRequest = new MockHttpServletRequest("GET", "/project/1/overview");
        MockHttpServletRequest actuatorRequest = new MockHttpServletRequest("POST", "/actuator/health");

        assertThat(filter.shouldNotFilter(getRequest)).isTrue();
        assertThat(filter.shouldNotFilter(actuatorRequest)).isTrue();
    }

    @Test
    void shouldNotFilterAllowsErrorEndpoint() {
        securityProperties.setApiKey("secret");
        MockHttpServletRequest errorRequest = new MockHttpServletRequest("POST", "/error");

        assertThat(filter.shouldNotFilter(errorRequest)).isTrue();
    }

    private static final class TrackingChain implements FilterChain {

        private final AtomicBoolean invoked = new AtomicBoolean(false);

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            invoked.set(true);
            if (response instanceof HttpServletResponse httpServletResponse) {
                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            }
        }
    }
}
