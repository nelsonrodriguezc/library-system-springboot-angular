import { Injectable } from '@angular/core';
import { AuthenticatedProfile } from '../models/api.models';

const TOKEN_KEY = 'libris.token';
const PROFILE_KEY = 'libris.profile';

interface StoredSession {
  token: string;
  profile: AuthenticatedProfile;
}

/**
 * Keeps the session where the reader asked for it.
 *
 * "Recordarme" is the difference between localStorage, which survives closing the
 * browser, and sessionStorage, which does not. Reading checks both so a session started
 * either way is picked up on reload.
 *
 * <p>The profile is stored next to the token instead of being decoded from the JWT: the
 * token is for the server to verify, not for the client to parse.
 */
@Injectable({ providedIn: 'root' })
export class TokenStorage {
  save(token: string, profile: AuthenticatedProfile, remember: boolean): void {
    this.clear();
    const store = remember ? localStorage : sessionStorage;
    store.setItem(TOKEN_KEY, token);
    store.setItem(PROFILE_KEY, JSON.stringify(profile));
  }

  read(): StoredSession | null {
    for (const store of [localStorage, sessionStorage]) {
      const token = store.getItem(TOKEN_KEY);
      const rawProfile = store.getItem(PROFILE_KEY);
      if (token && rawProfile) {
        try {
          return { token, profile: JSON.parse(rawProfile) as AuthenticatedProfile };
        } catch {
          // A corrupted entry is worth nothing; drop it and carry on as signed out.
          store.removeItem(TOKEN_KEY);
          store.removeItem(PROFILE_KEY);
        }
      }
    }
    return null;
  }

  token(): string | null {
    return this.read()?.token ?? null;
  }

  clear(): void {
    for (const store of [localStorage, sessionStorage]) {
      store.removeItem(TOKEN_KEY);
      store.removeItem(PROFILE_KEY);
    }
  }
}
