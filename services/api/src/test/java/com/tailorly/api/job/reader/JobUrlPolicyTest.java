package com.tailorly.api.job.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import org.junit.jupiter.api.Test;

class JobUrlPolicyTest {
  private static InetAddress ip(String literal) {
    try {
      return InetAddress.getByName(literal);
    } catch (UnknownHostException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static JobUrlPolicy resolvingTo(String... addresses) {
    return new JobUrlPolicy(host -> {
      InetAddress[] resolved = new InetAddress[addresses.length];
      for (int i = 0; i < addresses.length; i++)
        resolved[i] = ip(addresses[i]);
      return resolved;
    });
  }

  private static String codeOf(JobUrlPolicy policy, String url) {
    return assertThrows(BadRequestException.class, () -> policy.validate(url)).getMessage();
  }

  @Test
  void acceptsPublicHttpAndHttpsLinks() {
    JobUrlPolicy policy = resolvingTo("93.184.216.34");

    assertEquals("https://acme.gupy.io/jobs/1?x=1", policy.validate(" https://acme.gupy.io/jobs/1?x=1 ").toString());
    assertEquals("http://acme.test/vaga", policy.validate("http://acme.test/vaga").toString());
    assertEquals("https://acme.test:443/vaga", policy.validate("https://acme.test:443/vaga").toString());
  }

  @Test
  void rejectsInvalidSchemesAndMalformedLinks() {
    JobUrlPolicy policy = resolvingTo("93.184.216.34");

    for (String url : new String[] {"", "   ", "ftp://acme.test/x", "file:///etc/passwd", "javascript:alert(1)",
        "acme.test/vaga", "http://", "https://exa mple.test", "https://user:pass@acme.test/"}) {
      assertEquals(ErrorCodes.Job.URL_INVALID, codeOf(policy, url), url);
    }
    assertEquals(ErrorCodes.Job.URL_INVALID, codeOf(policy, null));
  }

  @Test
  void rejectsNonStandardPorts() {
    JobUrlPolicy policy = resolvingTo("93.184.216.34");

    assertEquals(ErrorCodes.Job.URL_NOT_ALLOWED, codeOf(policy, "http://acme.test:8080/"));
    assertEquals(ErrorCodes.Job.URL_NOT_ALLOWED, codeOf(policy, "https://acme.test:22/"));
  }

  @Test
  void rejectsHostsThatResolveToInternalAddresses() {
    for (String address : new String[] {"127.0.0.1", "10.0.0.5", "172.16.3.4", "192.168.1.10", "169.254.169.254",
        "0.0.0.0", "100.64.0.1", "198.18.0.1", "224.0.0.1", "255.255.255.255", "::1", "fe80::1", "fd00::1",
        "::ffff:127.0.0.1", "::ffff:10.1.2.3"}) {
      assertEquals(ErrorCodes.Job.URL_NOT_ALLOWED, codeOf(resolvingTo(address), "https://acme.test/"), address);
    }
  }

  @Test
  void rejectsWhenAnyResolvedAddressIsInternal() {
    assertEquals(ErrorCodes.Job.URL_NOT_ALLOWED,
        codeOf(resolvingTo("93.184.216.34", "10.0.0.1"), "https://acme.test/"));
  }

  @Test
  void rejectsLiteralInternalIpsAndUnknownHosts() {
    JobUrlPolicy realResolver = new JobUrlPolicy();

    assertEquals(ErrorCodes.Job.URL_NOT_ALLOWED, codeOf(realResolver, "http://127.0.0.1/"));
    assertEquals(ErrorCodes.Job.URL_NOT_ALLOWED, codeOf(realResolver, "http://[::1]/"));
    assertEquals(ErrorCodes.Job.URL_NOT_ALLOWED, codeOf(realResolver, "http://169.254.169.254/latest/meta-data/"));
    assertEquals(ErrorCodes.Job.URL_NOT_ALLOWED, codeOf(realResolver, "http://localhost/"));

    JobUrlPolicy unknown = new JobUrlPolicy(host -> {
      throw new UnknownHostException(host);
    });
    assertEquals(ErrorCodes.Job.URL_INVALID, codeOf(unknown, "https://nao-existe.invalid/"));
  }

  @Test
  void publicAddressesAreNotFlaggedAsInternal() {
    assertFalse(JobUrlPolicy.isInternal(ip("8.8.8.8")));
    assertFalse(JobUrlPolicy.isInternal(ip("2606:4700:4700::1111")));
    assertFalse(JobUrlPolicy.isInternal(ip("::ffff:8.8.8.8")));
    assertTrue(JobUrlPolicy.isInternal(ip("192.0.0.8")));
  }
}
