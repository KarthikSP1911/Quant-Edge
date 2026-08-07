package com.quantedge.backend.security;

import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.AuthProvider;
import com.quantedge.backend.enums.Role;
import com.quantedge.backend.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final OneTimeCodeService oneTimeCodeService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String providerId = oAuth2User.getAttribute("sub");

        if (email == null) {
            getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/login?error=EmailNotProvided");
            return;
        }

        User user = userRepository
                .findByEmail(email)
                .map(existing -> linkVerifiedGoogleIdentity(existing, providerId))
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .name(name != null ? name : email)
                            .role(Role.USER)
                            .authProvider(AuthProvider.GOOGLE)
                            .providerId(providerId)
                            .emailVerified(true)
                            .build();
                    return userRepository.save(newUser);
                });

        // Issue one-time code
        String code = oneTimeCodeService.generateCode(user);

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/callback")
                .queryParam("code", code)
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * Google has just proven ownership of this email address. If the matching account is an
     * unverified LOCAL account, it may have been pre-registered by someone else using this
     * user's email address with an attacker-controlled password (a pre-account-hijack). Since
     * the real owner has now proven ownership via Google, invalidate any existing password so
     * that attacker-controlled credentials stop working, and mark the account verified/linked.
     * Already-verified accounts (including ones the true owner registered and verified
     * themselves) are left untouched.
     */
    private User linkVerifiedGoogleIdentity(User existing, String providerId) {
        if (existing.getAuthProvider() == AuthProvider.LOCAL && !existing.isEmailVerified()) {
            existing.setPasswordHash(null);
            existing.setEmailVerified(true);
            existing.setProviderId(providerId);
            return userRepository.save(existing);
        }
        return existing;
    }
}
