import { useAnonymousSessionStatus } from "@queries/anonymous-session";
import { Info } from "@react-zero-ui/icon-sprite";
import { useAnonymousSessionStore } from "@stores/anonymous-session-store";
import { useAuthStore } from "@stores/auth-store";

const mib = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 });
const when = new Intl.DateTimeFormat("pt-BR", {
  day: "2-digit",
  month: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
});

/** Explica o modo visitante: dados temporários e os limites aplicados pelo servidor. */
export function AnonymousNotice() {
  const hasAccount = useAuthStore((state) => Boolean(state.accessToken));
  const hasSession = useAnonymousSessionStore((state) =>
    Boolean(state.session),
  );
  const status = useAnonymousSessionStatus().data;

  // Conta sem currículo de visitante pendente: não há nada a avisar.
  if (hasAccount && !hasSession) return null;

  return (
    <p
      role="note"
      className="flex items-start gap-2 rounded-lg border border-outline-variant/40 bg-surface-container-low p-3 text-xs leading-5 text-on-surface-variant"
    >
      <Info aria-hidden="true" className="mt-0.5 size-3.5 shrink-0" />
      <span>
        {hasAccount ? (
          "Você tem um currículo de visitante. Traga-o para a sua conta ou descarte-o."
        ) : (
          <>
            <strong className="font-semibold text-primary-950">
              Modo visitante:
            </strong>{" "}
            você pode usar tudo sem criar conta. Seu currículo fica guardado
            temporariamente
            {status ? ` (até ${when.format(new Date(status.expiresAt))})` : ""}{" "}
            e depois é apagado. Entre ou crie uma conta para guardar seu
            histórico.
          </>
        )}
        {status && (
          <>
            {" "}
            Limites: PDF de até{" "}
            {mib.format(status.limits.maxFileBytes / 1048576)} MiB e{" "}
            {status.limits.maxPages} páginas; {status.usage.uploads}/
            {status.limits.maxUploads} envios e {status.usage.extractions}/
            {status.limits.maxExtractions} processamentos usados.
          </>
        )}
      </span>
    </p>
  );
}
