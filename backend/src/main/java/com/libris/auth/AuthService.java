package com.libris.auth;

import com.libris.auth.dto.AuthResponse;
import com.libris.auth.dto.LoginRequest;
import com.libris.auth.dto.RegisterRequest;
import com.libris.auth.jwt.TokenIssuer;
import com.libris.user.AppUser;
import com.libris.user.AppUserRepository;
import com.libris.user.UserRole;
import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenIssuer tokenIssuer;

    public AuthService(AppUserRepository users,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       TokenIssuer tokenIssuer) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenIssuer = tokenIssuer;
    }

    /**
     * Public sign-up always creates a BIBLIOTECARIO account. Handing out ADMIN through an
     * unauthenticated endpoint would make the role check on the rest of the API pointless.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalise(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }
        AppUser user = users.save(new AppUser(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                UserRole.BIBLIOTECARIO));
        return tokenFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalise(request.email()), request.password()));
            AppUser user = ((LibrisUserDetails) authentication.getPrincipal()).appUser();
            return tokenFor(user);
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException();
        }
    }

    private AuthResponse tokenFor(AppUser user) {
        TokenIssuer.IssuedToken token = tokenIssuer.issue(user);
        return new AuthResponse(
                token.value(), token.expiresAt(), AuthResponse.AuthenticatedProfile.from(user));
    }

    private String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
