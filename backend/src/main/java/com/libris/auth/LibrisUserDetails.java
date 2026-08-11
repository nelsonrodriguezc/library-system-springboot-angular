package com.libris.auth;

import com.libris.user.AppUser;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Adapts an {@link AppUser} to what Spring Security needs to verify a sign-in. */
public class LibrisUserDetails implements UserDetails {

    private final transient AppUser user;

    public LibrisUserDetails(AppUser user) {
        this.user = user;
    }

    public AppUser appUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(user.getRole().authority()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    /**
     * Always true on purpose. The overdue block only stops a reader from taking new books
     * out; it never stops them from signing in to see their loans or return what they have.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
}
