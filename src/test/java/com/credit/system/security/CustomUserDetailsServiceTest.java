package com.credit.system.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.credit.system.domain.User;
import com.credit.system.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService 单元测试")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Nested
    @DisplayName("loadUserByUsername")
    class LoadUserByUsername {

        @Test
        @DisplayName("应成功加载用户")
        void shouldLoadUserSuccessfully() {
            User user = new User();
            user.setUsername("admin");
            user.setPassword("encoded-password");
            user.setRole("ADMIN");
            user.setEnabled(true);

            given(userRepository.findByUsername("admin")).willReturn(Optional.of(user));

            UserDetails userDetails = userDetailsService.loadUserByUsername("admin");

            assertThat(userDetails.getUsername()).isEqualTo("admin");
            assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
            assertThat(userDetails.isEnabled()).isTrue();
            assertThat(userDetails.getAuthorities())
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }

        @Test
        @DisplayName("USER 角色应正确加载")
        void shouldLoadUserRole() {
            User user = new User();
            user.setUsername("zhangsan");
            user.setPassword("encoded-password");
            user.setRole("USER");
            user.setEnabled(true);

            given(userRepository.findByUsername("zhangsan")).willReturn(Optional.of(user));

            UserDetails userDetails = userDetailsService.loadUserByUsername("zhangsan");

            assertThat(userDetails.getAuthorities())
                    .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
        }

        @Test
        @DisplayName("用户不存在应抛出异常")
        void shouldThrowWhenUserNotFound() {
            given(userRepository.findByUsername("nonexistent")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("用户不存在");
        }

        @Test
        @DisplayName("禁用用户应正确反映 enabled 状态")
        void shouldReflectDisabledStatus() {
            User user = new User();
            user.setUsername("disabled-user");
            user.setPassword("encoded-password");
            user.setRole("USER");
            user.setEnabled(false);

            given(userRepository.findByUsername("disabled-user")).willReturn(Optional.of(user));

            UserDetails userDetails = userDetailsService.loadUserByUsername("disabled-user");

            assertThat(userDetails.isEnabled()).isFalse();
        }
    }
}
