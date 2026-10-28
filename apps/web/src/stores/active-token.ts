import type { OwnerKind } from "./base-cv-store";

interface TokenChoice {
  accountToken: string | null;
  anonymousToken: string | null;
  ownerKind: OwnerKind | null;
}

// Uma versão enviada como visitante continua acessível só pelo token anônimo até
// ser migrada ou descartada, mesmo que a pessoa entre numa conta no meio da revisão.
export function pickToken({
  accountToken,
  anonymousToken,
  ownerKind,
}: TokenChoice): string | null {
  if (ownerKind === "anonymous" && anonymousToken) return anonymousToken;
  return accountToken ?? anonymousToken ?? null;
}
