package com.tailorly.api.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tailorly.api.search.WebSearchClient;
import com.tailorly.api.vault.VaultLocations;
import org.junit.jupiter.api.Test;

class FeaturesControllerTest {
  @Test
  void reportsOnlyWhatTheServerActuallyHas() {
    var vault = mock(VaultLocations.class);
    var search = mock(WebSearchClient.class);
    when(vault.isAvailable()).thenReturn(false);
    when(search.isConfigured()).thenReturn(true);

    assertEquals(new FeaturesController.Features(false, true),
        new FeaturesController(vault, search).get());
  }
}
