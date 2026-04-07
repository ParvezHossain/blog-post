package com.parvez.blogs.config;

import com.parvez.blogs.dto.TokenResponse;
import com.parvez.blogs.entity.Role;
import com.parvez.blogs.entity.User;
import com.parvez.blogs.repository.UserRepository;
import com.parvez.blogs.security.JwtUtil;
import com.parvez.blogs.service.AuthService;
import com.parvez.blogs.service.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AuthService authService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = authToken.getAuthorizedClientRegistrationId(); // "google" or "github"

        OAuthUserProfile profile = extractProfile(oAuth2User, registrationId);

        if (profile.email == null) {
            // GitHub users with private emails — cannot create an account without email
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("""
                    { "error": "Email not available. Please make your GitHub email public and try again." }
                    """);
            return;
        }

        User user = userRepository.findByEmail(profile.email())
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(profile.email());
                    newUser.setFirstName(profile.firstName());
                    newUser.setLastName(profile.lastName());
                    newUser.setUsername(profile.username());
                    newUser.setPassword(null);
                    newUser.setProvider(profile.provider());
                    newUser.setRole(Role.USER);
                    return userRepository.save(newUser);
                });

        // Prevent account hijacking across providers
        if (user.getProvider() != profile.provider()) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.getWriter().write("""
                    { "error": "Email already registered with %s login." }
                    """.formatted(user.getProvider().name().toLowerCase()));
            return;
        }

        String accessToken = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        // Persist refresh token so rotation and logout work correctly
        authService.rotateRefreshToken(user.getUsername());
        authService.saveRefreshToken(user.getUsername(), refreshToken);

        // Return proper JSON
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        response.getWriter().write("""
                {
                  "accessToken":  "%s",
                  "refreshToken": "%s"
                }
                """.formatted(accessToken, refreshToken));
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private OAuthUserProfile extractProfile(OAuth2User oAuth2User, String registrationId) {
        return switch (registrationId) {
            case "google" -> extractGoogleProfile(oAuth2User);
            case "github" -> extractGithubProfile(oAuth2User);
            default -> throw new IllegalArgumentException("Unsupported provider: " + registrationId);
        };
    }

    private OAuthUserProfile extractGoogleProfile(OAuth2User oAuth2User) {
        String sub = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");

        return new OAuthUserProfile(
                email,
                firstName != null ? firstName : "Unknown",
                lastName != null ? lastName : "Unknown",
                "google_" + sub,
                AuthProvider.GOOGLE
        );
    }

    private OAuthUserProfile extractGithubProfile(OAuth2User oAuth2User) {
        // GitHub returns id as an Integer
        Integer id = oAuth2User.getAttribute("id");
        String email = oAuth2User.getAttribute("email");   // may be null if private
        String fullName = oAuth2User.getAttribute("name");    // "Parvez Hossain" as one string
        String login = oAuth2User.getAttribute("login");   // GitHub username e.g. "parvez123"

        // Split full name into first/last best-effort
        String firstName = "Unknown";
        String lastName = "Unknown";

        if (fullName != null && !fullName.isBlank()) {
            String[] parts = fullName.trim().split("\\s+", 2);
            firstName = parts[0];
            lastName = parts.length > 1 ? parts[1] : parts[0];
        }

        return new OAuthUserProfile(
                email,
                firstName,
                lastName,
                "github_" + id, // stable — GitHub login can change, id cannot
                AuthProvider.GITHUB
        );
    }

    private record OAuthUserProfile(
            String email,
            String firstName,
            String lastName,
            String username,
            AuthProvider provider
    ) {
    }
}
