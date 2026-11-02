import { ScanSearch } from "@react-zero-ui/icon-sprite";
import { Link } from "@tanstack/react-router";

/** Resumo da due diligence (RF08) no perfil; não há ajustes próprios: usa o provedor de IA escolhido acima. */
export function AiDueDiligencePreviewCard() {
  return (
    <div className="flex flex-col gap-3 rounded-xl border border-outline-variant bg-surface-container-low p-4">
      <h3 className="flex items-center gap-2 text-title-md font-semibold text-primary-950">
        <ScanSearch aria-hidden="true" className="size-4" />
        Due Diligence de Empresas
      </h3>
      <p className="text-body-sm text-on-surface-variant">
        Consolida sinais públicos sobre a empresa de uma vaga em onze fatores,
        usando o provedor de IA configurado acima. Cada conclusão guarda fonte,
        data, entidade analisada e incerteza, e as análises ficam salvas na sua
        conta. Fontes privadas dependem de integração e acesso autorizado e
        ainda não estão disponíveis.
      </p>
      <Link
        to="/due-diligence"
        className="self-start rounded-sm text-sm font-semibold text-primary-950 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-900"
      >
        Abrir Due Diligence
      </Link>
    </div>
  );
}
