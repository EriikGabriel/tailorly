import { Tabs, TabsList, TabsTab } from "@animate/base/tabs";
import { Button } from "@animate/buttons/button";
import {
  Code2,
  Download,
  Eye,
  FileText,
  Save,
  Sparkles,
} from "@react-zero-ui/icon-sprite";
import { useBaseCvStore } from "@stores/base-cv-store";
import { useJobDraftStore } from "@stores/job-draft-store";
import { motion } from "motion/react";
import { useState } from "react";

type ViewMode = "visual" | "markdown";

export function DocumentStage() {
  const [viewMode, setViewMode] = useState<ViewMode>("visual");
  const baseCv = useBaseCvStore((state) => state.file);
  const { mode, jobUrl, jobText } = useJobDraftStore();
  const hasJob =
    mode === "url" ? jobUrl.trim().length > 0 : jobText.trim().length > 0;
  const ready = Boolean(baseCv) && hasJob;

  return (
    <div className="flex h-full w-full flex-1 flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-card p-4 shadow-xs">
        <div className="flex min-w-0 flex-col gap-1.5">
          <h2 className="text-base font-semibold leading-6 text-on-surface">
            Currículo Otimizado
          </h2>
          <p className="text-xs font-medium tracking-[0.3px] text-on-surface-variant">
            {ready
              ? "Pronto para gerar • revise antes de baixar"
              : "Preencha o Ground Truth e a vaga alvo para liberar a geração"}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <Tabs
            value={viewMode}
            onValueChange={(value) => setViewMode(value as ViewMode)}
          >
            <TabsList
              aria-label="Modo de visualização do currículo"
              className="h-auto min-h-9.5 gap-1 bg-surface-container p-1 **:data-[slot=motion-highlight]:border-0 **:data-[slot=motion-highlight]:bg-card"
            >
              <TabsTab
                value="visual"
                type="button"
                className="h-7.5 gap-1.5 px-2.5 text-xs tracking-[0.3px] text-on-surface-variant data-selected:font-semibold data-selected:text-on-surface"
              >
                <Eye className="size-3.5" aria-hidden="true" />
                Visual
              </TabsTab>
              <TabsTab
                value="markdown"
                type="button"
                className="h-7.5 gap-1.5 px-2.5 text-xs tracking-[0.3px] text-on-surface-variant data-selected:font-semibold data-selected:text-on-surface"
              >
                <Code2 className="size-3.5" aria-hidden="true" />
                Markdown
              </TabsTab>
            </TabsList>
          </Tabs>

          <Button
            type="button"
            disabled
            title="Disponível após gerar o currículo"
            className="h-9.5 gap-1.5 rounded-lg bg-primary-900 px-3 text-xs font-semibold text-primary-foreground disabled:opacity-40"
          >
            <Download className="size-3.5" aria-hidden="true" />
            Baixar PDF
          </Button>
        </div>
      </div>

      <div className="relative flex min-h-100 w-full flex-1 items-center justify-center overflow-hidden rounded-2xl bg-surface-container-high p-6 sm:p-10">
        <div className="relative flex w-full max-w-180 h-full flex-1 flex-col items-center justify-center rounded-lg bg-card p-8 shadow-[0_20px_25px_-5px_rgba(0,0,0,0.1),0_8px_10px_-6px_rgba(0,0,0,0.1)] sm:p-12">
          <EmptyDocumentState ready={ready} viewMode={viewMode} />
        </div>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-surface-container-low px-3 py-2 text-xs text-on-surface-variant">
        <span>Gerado apenas a partir do seu Currículo Base aprovado.</span>
        <span className="flex items-center gap-1.5">
          <Save className="size-3.25" aria-hidden="true" />
          Geração por IA: em desenvolvimento
        </span>
      </div>
    </div>
  );
}

function EmptyDocumentState({
  ready,
  viewMode,
}: {
  ready: boolean;
  viewMode: ViewMode;
}) {
  return (
    <motion.div
      key={viewMode}
      initial={{ opacity: 0, y: 6 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25, ease: "easeOut" }}
      className="flex w-full flex-col items-center gap-3  text-center"
    >
      <span className="flex size-14 items-center justify-center rounded-full bg-secondary-container text-on-secondary-container">
        {viewMode === "visual" ? (
          <FileText className="size-6" aria-hidden="true" />
        ) : (
          <Code2 className="size-6" aria-hidden="true" />
        )}
      </span>
      <h3 className="text-title-md text-on-surface">
        Seu currículo sob medida aparecerá aqui
      </h3>
      <p className="max-w-sm text-sm leading-6 text-on-surface-variant">
        {ready
          ? "Tudo pronto. Clique em “Gerar Currículo Sob Medida com IA” para confirmar o Ground Truth e iniciar a geração."
          : "Configure o Ground Truth e a vaga alvo no painel ao lado para liberar a prévia."}
      </p>
      <span className="mt-2 inline-flex items-center gap-1.5 rounded-full bg-surface-container px-3 py-1 text-xs font-semibold tracking-[0.3px] text-on-surface-variant">
        <Sparkles className="size-3" aria-hidden="true" />
        Pré-visualização • nenhum dado gerado ainda
      </span>
    </motion.div>
  );
}
