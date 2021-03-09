package com.codesoom.assignment.application;

import com.codesoom.assignment.domain.User;
import com.codesoom.assignment.domain.UserRepository;
import com.codesoom.assignment.errors.InvalidTokenException;
import com.codesoom.assignment.errors.LoginFailException;
import com.codesoom.assignment.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuthenticationServiceTest {
    private static final String SECRET = "12345678901234567890123456789012";

    private static final String VALID_TOKEN = "eyJhbGciOiJIUzI1NiJ9." +
            "eyJ1c2VySWQiOjF9.ZZ3CUl0jxeLGvQ1Js5nG2Ty5qGTlqai5ubDMXZOdaDk";
    private static final String INVALID_TOKEN = "eyJhbGciOiJIUzI1NiJ9." +
            "eyJ1c2VySWQiOjF9.ZZ3CUl0jxeLGvQ1Js5nG2Ty5qGTlqai5ubDMXZOdaD0";
    private static final String ENCODE_PASSWORD = "$2a$10$hfmGZDxIJ5of6BesJlffqeMFF4nIEr2aEivlXE/PmBAPGtJvri5Ku";
    private static final String WRONG_ENCODED_PASSWORD = "$2a$10$hfmGZDxIJ5of6BesJlffqeMFF4nIEr2aEivlXE/PmBAPGtJvri5Ku"+"_WRONG";

    private AuthenticationService authenticationService;

    private UserRepository userRepository = mock(UserRepository.class);

    private PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    @BeforeEach
    void setUp() {
        JwtUtil jwtUtil = new JwtUtil(SECRET);
        authenticationService = new AuthenticationService(
                userRepository, jwtUtil, passwordEncoder);
        User user = User.builder()
                .deleted(false)
                .password(ENCODE_PASSWORD)
                .build();

        given(passwordEncoder.encode(eq("test")))
                .willReturn(ENCODE_PASSWORD);

        given(passwordEncoder.encode(eq("xxx")))
                .willReturn(WRONG_ENCODED_PASSWORD);

        given(userRepository.findByEmail("tester@example.com"))
                .willReturn(Optional.of(user));
    }

    @Test
    void loginWithRightEmailAndPassword() {
        String accessToken = authenticationService.login(
                "tester@example.com", "test");

        assertThat(accessToken).isEqualTo(VALID_TOKEN);

        verify(userRepository).findByEmail("tester@example.com");
    }

    @Test
    void loginWithWrongEmail() {
        assertThatThrownBy(
                () -> authenticationService.login("badguy@example.com", "test")
        ).isInstanceOf(LoginFailException.class);

        verify(userRepository).findByEmail("badguy@example.com");
    }

    @Test
    void loginWithWrongPassword() {
        assertThatThrownBy(
                () -> authenticationService.login("tester@example.com", "xxx")
        ).isInstanceOf(LoginFailException.class);

        verify(userRepository).findByEmail("tester@example.com");
    }

    @Test
    void parseTokenWithValidToken() {
        Long userId = authenticationService.parseToken(VALID_TOKEN);

        assertThat(userId).isEqualTo(1L);
    }

    @Test
    void parseTokenWithInvalidToken() {
        assertThatThrownBy(
                () -> authenticationService.parseToken(INVALID_TOKEN)
        ).isInstanceOf(InvalidTokenException.class);
    }
}
