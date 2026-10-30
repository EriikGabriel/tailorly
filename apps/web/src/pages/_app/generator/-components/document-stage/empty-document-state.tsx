import { FileText } from "@react-zero-ui/icon-sprite";

/** Estado vazio do palco: explica o que falta (ou o que clicar) para ver o currículo. */
export function EmptyDocumentState({
  ready,
  loading,
}: {
  ready: boolean;
  loading: boolean;
}) {
  return (
    <div className="relative flex min-h-100 w-full flex-1 items-center justify-center overflow-hidden rounded-2xl bg-surface-container-high p-6 sm:p-10">
      <div className="relative flex h-full w-full max-w-180 flex-1 flex-col items-center justify-center rounded-lg bg-card p-8 shadow-md sm:p-12">
        <div className="flex w-full flex-col items-center gap-3 text-center">
          <span className="flex size-14 items-center justify-center rounded-full bg-secondary-container text-on-secondary-container">
            <FileText className="size-6" aria-hidden="true" />
          </span>
          <h3 className="text-title-md text-on-surface">
            Seu currículo sob medida aparecerá aqui
          </h3>
          <p className="max-w-sm text-sm leading-6 text-on-surface-variant">
            {loading
              ? "Carregando o currículo gerado..."
              : ready
                ? "Tudo pronto. Clique em “Gerar Currículo Sob Medida com IA” para confirmar o Ground Truth e iniciar a geração."
                : "Configure o Ground Truth e a vaga alvo no painel ao lado para liberar a prévia."}
          </p>
        </div>
      </div>
    </div>
  );
}
