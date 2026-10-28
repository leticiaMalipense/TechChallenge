package br.com.autoshop.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SpringSecurityConfigTest {

    private static final String JWT_KEY = "a-test-signing-key-with-at-least-32-bytes";

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(SpringSecurityConfig.class)
            .withPropertyValues("jwt.key=" + JWT_KEY)
            .withBean(UserDetailsService.class, () -> mock(UserDetailsService.class));

    @Test
    void createsSecurityFilterChainAndSecurityBeans() {
        contextRunner.run(context -> {
            assertTrue(context.isRunning());
            assertNotNull(context.getBean(SecurityFilterChain.class));
            assertNotNull(context.getBean(PasswordEncoder.class));
            assertNotNull(context.getBean(org.springframework.security.authentication.AuthenticationManager.class));
            assertNotNull(context.getBean(org.springframework.security.oauth2.jwt.JwtDecoder.class));
            assertNotNull(context.getBean(org.springframework.security.oauth2.jwt.JwtEncoder.class));
        });
    }
}
