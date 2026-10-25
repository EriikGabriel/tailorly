import { Switch } from "@animate/base/switch";
import { Hourglass, ScanSearch } from "@react-zero-ui/icon-sprite";

export function AiDueDiligencePreviewCard() {
  return (
    <div className="flex flex-col gap-3 rounded-xl border border-outline-variant bg-surface-container-low p-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="flex items-center gap-2 text-title-md font-semibold text-primary-950">
          <ScanSearch aria-hidden="true" className="size-4" />
          Due Diligence de Empresas
        </h3>
        <span className="inline-flex items-center gap-1.5 rounded-full bg-card px-2.5 py-0.5 text-label-sm font-medium text-on-surface-variant">
          <Hourglass aria-hidden="true" className="size-3" />
          Backlog (RF08)
        </span>
      </div>
      <p className="text-body-sm text-on-surface-variant">
        Ainda sem especificação funcional definida. A ideia é consolidar sinais
        públicos sobre empresas antes da candidatura, com fatores e fontes
        rastreáveis, reaproveitando o provedor de IA configurado para extração e
        geração. Nenhuma configuração está disponível até a funcionalidade sair
        do backlog.
      </p>
      <div
        aria-disabled="true"
        className="flex items-center justify-between gap-3 pointer-events-none rounded-lg bg-card p-3 opacity-60"
      >
        <span className="text-label-md font-semibold text-primary-950">
          Ativar due diligence automática ao gerar currículos
        </span>
        <Switch checked={false} onCheckedChange={() => {}} />
      </div>
    </div>
  );
}
