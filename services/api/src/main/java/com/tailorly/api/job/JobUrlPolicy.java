package com.tailorly.api.job;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

import com.tailorly.api.shared.exception.BadRequestException;
import com.tailorly.api.shared.exception.ErrorCodes;
import org.springframework.stereotype.Component;

/**
 * Valida os links de vaga enviados por visitantes antes de o servidor acessá-los (SSRF): só
 * http/https nas portas padrão, sem credenciais no link e sem destinos internos. O host é resolvido
 * a cada validação, inclusive a cada redirecionamento. Como o Jsoup resolve o nome de novo ao
 * conectar, um DNS malicioso ainda poderia trocar a resposta entre as duas consultas; a mitigação
 * completa exigiria fixar o IP na conexão.
 */
@Component
public class JobUrlPolicy {
  private static final Set<Integer> ALLOWED_PORTS = Set.of(80, 443);

  /** Resolve nomes de host; substituível nos testes. */
  @FunctionalInterface
  interface HostResolver {
    InetAddress[] resolve(String host) throws UnknownHostException;
  }

  private final HostResolver resolver;

  public JobUrlPolicy() {
    this(InetAddress::getAllByName);
  }

  JobUrlPolicy(HostResolver resolver) {
    this.resolver = resolver;
  }

  /** Retorna o URI normalizado ou lança {@link BadRequestException} quando o link não é permitido. */
  public URI validate(String rawUrl) {
    URI uri = parse(rawUrl);
    String host = uri.getHost();

    if (host == null || host.isBlank() || uri.getUserInfo() != null)
      throw new BadRequestException(ErrorCodes.Job.URL_INVALID);

    int port = uri.getPort() == -1 ? ("https".equals(uri.getScheme()) ? 443 : 80) : uri.getPort();
    if (!ALLOWED_PORTS.contains(port))
      throw new BadRequestException(ErrorCodes.Job.URL_NOT_ALLOWED);

    InetAddress[] addresses;
    try {
      addresses = resolver.resolve(host);
    } catch (UnknownHostException exception) {
      throw new BadRequestException(ErrorCodes.Job.URL_INVALID);
    }

    if (addresses.length == 0)
      throw new BadRequestException(ErrorCodes.Job.URL_INVALID);
    for (InetAddress address : addresses) {
      if (isInternal(address))
        throw new BadRequestException(ErrorCodes.Job.URL_NOT_ALLOWED);
    }
    return uri;
  }

  private static URI parse(String rawUrl) {
    if (rawUrl == null || rawUrl.isBlank())
      throw new BadRequestException(ErrorCodes.Job.URL_INVALID);
    try {
      URI uri = new URI(rawUrl.trim());
      String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
      if (!scheme.equals("http") && !scheme.equals("https"))
        throw new BadRequestException(ErrorCodes.Job.URL_INVALID);
      return uri;
    } catch (URISyntaxException exception) {
      throw new BadRequestException(ErrorCodes.Job.URL_INVALID);
    }
  }

  static boolean isInternal(InetAddress address) {
    if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
        || address.isSiteLocalAddress() || address.isMulticastAddress())
      return true;

    byte[] bytes = address.getAddress();
    if (address instanceof Inet4Address)
      return isReservedV4(bytes);

    if (address instanceof Inet6Address) {
      // fc00::/7 (unique local)
      if ((bytes[0] & 0xfe) == 0xfc)
        return true;
      // ::ffff:a.b.c.d (IPv4 mapeado) herda a regra do endereço IPv4 embutido.
      boolean mapped = true;
      for (int i = 0; i < 10; i++)
        mapped &= bytes[i] == 0;
      if (mapped && bytes[10] == (byte) 0xff && bytes[11] == (byte) 0xff)
        return isReservedV4(new byte[] {bytes[12], bytes[13], bytes[14], bytes[15]});
    }
    return false;
  }

  private static boolean isReservedV4(byte[] b) {
    int first = b[0] & 0xff;
    int second = b[1] & 0xff;
    return first == 0                                     // 0.0.0.0/8
        || (first == 100 && second >= 64 && second <= 127) // 100.64.0.0/10 (CGNAT)
        || (first == 192 && second == 0 && (b[2] & 0xff) == 0) // 192.0.0.0/24
        || (first == 198 && (second == 18 || second == 19))   // 198.18.0.0/15
        || first >= 240;                                  // 240.0.0.0/4 e broadcast
  }
}
