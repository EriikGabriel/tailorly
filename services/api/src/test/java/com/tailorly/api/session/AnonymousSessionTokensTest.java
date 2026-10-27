package com.tailorly.api.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AnonymousSessionTokensTest {
  @Test
  void generatesUniquePrefixedTokens() {
    String first = AnonymousSessionTokens.generate();
    String second = AnonymousSessionTokens.generate();

    assertTrue(first.startsWith(AnonymousSessionTokens.PREFIX));
    assertNotEquals(first, second);
    assertTrue(first.length() > 40);
  }

  @Test
  void hashIsStableHexAndDoesNotContainTheToken() {
    String token = AnonymousSessionTokens.generate();
    String hash = AnonymousSessionTokens.hash(token);

    assertEquals(hash, AnonymousSessionTokens.hash(token));
    assertEquals(64, hash.length());
    assertFalse(hash.contains(token));
  }

  @Test
  void recognizesOnlyPrefixedTokens() {
    assertTrue(AnonymousSessionTokens.isAnonymousToken("tlr_anon_abc"));
    assertFalse(AnonymousSessionTokens.isAnonymousToken("eyJhbGciOiJIUzI1NiJ9.e30.x"));
    assertFalse(AnonymousSessionTokens.isAnonymousToken(null));
  }
}
