import { Switch } from "@animate/base/switch";
import { useProfileDueDiligencePreferences } from "@queries/profile-due-diligence-preferences";
import { ScanSearch } from "@react-zero-ui/icon-sprite";
import { Link } from "@tanstack/react-router";

/**
 * Due diligence de empresas no perfil. O interruptor controla só a etapa automática, disparada pela
 * leitura de uma vaga (gasta busca web e IA); a página continua disponível para consultas manuais.
 * A mudança fica em rascunho até salvar pela barra de alterações do perfil.
 */
export function AiDueDiligencePreviewCard() {
  const { isAccount, isLoading, enabled, setDraft, save } =
    useProfileDueDiligencePreferences();
  const disabled = !isAccount || isLoading || save.isPending;

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

      <div className="flex items-center justify-between gap-4 rounded-lg">
        <div className="flex flex-col gap-0.5">
          <p className="text-label-md font-semibold text-primary-950">
            Consultar automaticamente ao ler uma vaga
          </p>
          <p className="text-body-sm text-on-surface-variant">
            {enabled
              ? "Ligada: ao ler uma vaga, a empresa é consultada em segundo plano e gasta busca web e IA."
              : "Desligada: nenhuma consulta automática, você ainda pode consultar a empresa na página."}
          </p>
        </div>
        <Switch
          checked={enabled}
          disabled={disabled}
          onCheckedChange={(next) => setDraft(next)}
          aria-label="Consultar automaticamente a due diligence ao ler uma vaga"
        />
      </div>

      <Link
        to="/due-diligence"
        className="self-start rounded-sm text-sm font-semibold text-primary-950 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-900"
      >
        Abrir Due Diligence
      </Link>
    </div>
  );
}
