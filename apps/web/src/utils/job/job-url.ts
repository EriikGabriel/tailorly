export const MAX_JOB_URL_LENGTH = 2048;

export type JobUrlCheck =
  | { status: "empty" }
  | { status: "invalid"; message: string }
  | { status: "warning"; message: string }
  | { status: "valid"; message: string };

// Sites que só entregam a vaga depois de login ou por JavaScript; a leitura por link não funciona.
const LOGIN_WALLED = ["linkedin.com"];

const KNOWN_PLATFORMS: Record<string, string> = {
  "gupy.io": "Gupy",
  "greenhouse.io": "Greenhouse",
  "lever.co": "Lever",
};

function hostMatches(host: string, domain: string) {
  return host === domain || host.endsWith(`.${domain}`);
}

function isPrivateIpv4(host: string) {
  const match = /^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$/.exec(host);
  if (!match) return false;
  const [a, b] = [Number(match[1]), Number(match[2])];
  return (
    a === 0 ||
    a === 10 ||
    a === 127 ||
    a >= 224 ||
    (a === 100 && b >= 64 && b <= 127) ||
    (a === 169 && b === 254) ||
    (a === 172 && b >= 16 && b <= 31) ||
    (a === 192 && b === 168)
  );
}

function isInternalHost(host: string) {
  return (
    host === "localhost" ||
    host.endsWith(".localhost") ||
    host.endsWith(".local") ||
    host.endsWith(".internal") ||
    host.startsWith("[") ||
    isPrivateIpv4(host) ||
    // Sem ponto não é um site público (ex.: "intranet").
    !host.includes(".")
  );
}

/**
 * Validação imediata do link da vaga enquanto a pessoa digita. Espelha as regras do servidor
 * (só http/https, portas 80/443, sem credenciais, sem destinos internos), que continuam sendo a
 * garantia: aqui só se evita enviar o que o servidor recusaria e se explica o motivo.
 */
export function checkJobUrl(raw: string): JobUrlCheck {
  const value = raw.trim();
  if (!value) return { status: "empty" };

  if (value.length > MAX_JOB_URL_LENGTH)
    return {
      status: "invalid",
      message: `O link passa de ${MAX_JOB_URL_LENGTH} caracteres.`,
    };
  if (/\s/.test(value))
    return { status: "invalid", message: "O link não pode ter espaços." };

  // Sem esquema, a pessoa provavelmente colou "empresa.gupy.io/jobs/1".
  if (!/^[a-z][a-z0-9+.-]*:/i.test(value))
    return {
      status: "invalid",
      message: "Comece o link com https:// (ou http://).",
    };

  let url: URL;
  try {
    url = new URL(value);
  } catch {
    return { status: "invalid", message: "Este não parece um link válido." };
  }

  if (url.protocol !== "https:" && url.protocol !== "http:")
    return {
      status: "invalid",
      message: "Só links http:// ou https:// são aceitos.",
    };
  if (!url.hostname)
    return { status: "invalid", message: "Falta o endereço do site." };
  if (url.username || url.password)
    return {
      status: "invalid",
      message: "O link não pode conter usuário ou senha.",
    };

  const host = url.hostname.toLowerCase();
  if (isInternalHost(host))
    return {
      status: "invalid",
      message: "Este endereço não é um site público que possamos acessar.",
    };
  if (url.port && url.port !== "80" && url.port !== "443")
    return {
      status: "invalid",
      message: "Só as portas padrão (80 e 443) são aceitas.",
    };

  if (LOGIN_WALLED.some((domain) => hostMatches(host, domain)))
    return {
      status: "warning",
      message:
        "Este site costuma exigir login e não pode ser lido por link. Cole o texto da vaga.",
    };

  const platform = Object.entries(KNOWN_PLATFORMS).find(([domain]) =>
    hostMatches(host, domain),
  )?.[1];
  return {
    status: "valid",
    message: platform ? `Link da ${platform} reconhecido.` : "Link válido.",
  };
}
