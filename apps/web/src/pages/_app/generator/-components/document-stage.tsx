import { Tabs, TabsList, TabsTab } from "@animate/base/tabs";
import { Button } from "@animate/buttons/button";
import { useCvPreview } from "@hooks/use-cv-preview";
import { apiErrorMessage } from "@lib/api/error-message";
import { cvTemplateFindById } from "@lib/api/generated/tailorly";
import { tokenForOwnerKind } from "@queries/api-credential";
import {
  downloadCvFile,
  useBundledCvTemplate,
  useGenerateCv,
  useSaveCvTemplate,
  useSelectedGeneration,
} from "@queries/cv-generation";
import {
  Code2,
  Columns2,
  Download,
  Eye,
  FileText,
  Save,
  Sparkles,
} from "@react-zero-ui/icon-sprite";
import { useBaseCvStore } from "@stores/base-cv-store";
import { useCvGenerationStore } from "@stores/cv-generation-store";
import { useJobDraftStore } from "@stores/job-draft-store";
import { skipToken, useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { AtsScoreCard } from "./ats-score-card";
import { CvPreview } from "./cv-preview";
import { TypstEditor } from "./typst-editor";

export type ViewMode = "visual" | "typst" | "split";

const tabClass =
  "h-7.5 gap-1.5 px-2.5 text-xs tracking-[0.3px] text-on-surface-variant data-selected:font-semibold data-selected:text-on-surface";

export function DocumentStage({
  viewMode,
  onViewModeChange,
}: {
  viewMode: ViewMode;
  onViewModeChange: (mode: ViewMode) => void;
}) {
  const baseCv = useBaseCvStore((state) => state.file);
  const { mode, jobUrl, jobText } = useJobDraftStore();
  const hasJob =
    mode === "url" ? jobUrl.trim().length > 0 : jobText.trim().length > 0;
  const ready = Boolean(baseCv) && hasJob;

  const selected = useCvGenerationStore((state) => state.selected);
  const generation = useSelectedGeneration();
  const token = tokenForOwnerKind(selected?.ownerKind ?? null);
  const generationId = generation.data?.id ?? null;

  // O código que a pessoa edita começa como o template usado nesta geração.
  const templateId = generation.data?.template?.id;
  const template = useQuery({
    queryKey: ["cv", "template", "by-id", templateId ?? ""],
    queryFn:
      templateId && token
        ? () => cvTemplateFindById(templateId, { accessToken: token })
        : skipToken,
    staleTime: Number.POSITIVE_INFINITY,
  });
  const bundled = useBundledCvTemplate(token);
  const [draft, setDraft] = useState<string | null>(null);
  useEffect(() => {
    setDraft(
      generation.data?.template?.content ?? template.data?.content ?? null,
    );
  }, [generation.data?.template?.content, template.data?.content]);

  const preview = useCvPreview(generationId, draft, token);
  const save = useSaveCvTemplate();
  const regenerate = useGenerateCv();
  const [actionError, setActionError] = useState<string | null>(null);
  const dirty = draft !== null && draft !== template.data?.content;

  async function saveAndRegenerate() {
    if (!generation.data?.ownerId || !token || draft === null) return;
    setActionError(null);
    try {
      await save.mutateAsync({
        ownerId: generation.data.ownerId,
        token,
        content: draft,
      });
      await regenerate.mutateAsync();
    } catch (cause) {
      setActionError(
        apiErrorMessage(cause, "Não foi possível salvar o template."),
      );
    }
  }

  async function download(format: "pdf" | "markdown") {
    if (!generationId || !token) return;
    setActionError(null);
    try {
      await downloadCvFile(generationId, format, token);
    } catch (cause) {
      setActionError(
        apiErrorMessage(cause, "Não foi possível baixar o arquivo."),
      );
    }
  }

  return (
    <div className="flex h-full w-full flex-1 flex-col gap-4">
      <div
        className={`flex flex-wrap items-center justify-between gap-3 bg-card ${
          viewMode !== "visual"
            ? "rounded-lg px-3 py-2 shadow-xs"
            : "rounded-xl p-4 shadow-xs"
        }`}
      >
        <div className="flex min-w-0 flex-col gap-1.5">
          <h2 className="text-base font-semibold leading-6 text-on-surface">
            Currículo Otimizado
          </h2>
          <p className="text-xs font-medium tracking-[0.3px] text-on-surface-variant">
            {generation.data
              ? `Gerado do Ground Truth v${generation.data.groundTruth?.snapshotNumber} e da vaga v${generation.data.job?.versionNumber}`
              : ready
                ? "Pronto para gerar • revise antes de baixar"
                : "Preencha o Ground Truth e a vaga alvo para liberar a geração"}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <Tabs
            value={viewMode}
            onValueChange={(value) => onViewModeChange(value as ViewMode)}
          >
            <TabsList
              aria-label="Modo de visualização do currículo"
              className="h-auto min-h-9.5 gap-1 bg-surface-container p-1 **:data-[slot=motion-highlight]:border-0 **:data-[slot=motion-highlight]:bg-card"
            >
              <TabsTab value="visual" type="button" className={tabClass}>
                <Eye className="size-3.5" aria-hidden="true" />
                PDF
              </TabsTab>
              <TabsTab value="typst" type="button" className={tabClass}>
                <Code2 className="size-3.5" aria-hidden="true" />
                Typst
              </TabsTab>
              <TabsTab value="split" type="button" className={tabClass}>
                <Columns2 className="size-3.5" aria-hidden="true" />
                Dividido
              </TabsTab>
            </TabsList>
          </Tabs>

          <Button
            type="button"
            variant="outline"
            onClick={() => download("markdown")}
            disabled={!generationId}
            className="h-9.5 gap-1.5 rounded-lg  px-3 text-xs font-semibold text-on-surface disabled:opacity-40"
          >
            <FileText className="size-3.5" aria-hidden="true" />
            Markdown
          </Button>
          <Button
            type="button"
            onClick={() => download("pdf")}
            disabled={!generationId}
            title={
              generationId ? undefined : "Disponível após gerar o currículo"
            }
            className="h-9.5 gap-1.5 rounded-lg bg-primary-900 px-3 text-xs font-semibold text-primary-foreground disabled:opacity-40"
          >
            <Download className="size-3.5" aria-hidden="true" />
            Baixar PDF
          </Button>
        </div>
      </div>

      {generation.data && draft !== null ? (
        <div
          className={`grid min-h-[min(1120px,141.4vw)] flex-1 gap-4 ${
            viewMode === "split" ? "items-start lg:grid-cols-2" : ""
          }`}
        >
          {(viewMode === "typst" || viewMode === "split") && (
            <TypstEditor value={draft} onChange={setDraft} preview={preview} />
          )}
          {(viewMode === "visual" || viewMode === "split") && (
            <CvPreview preview={preview} />
          )}
        </div>
      ) : (
        <div className="relative flex min-h-100 w-full flex-1 items-center justify-center overflow-hidden rounded-2xl bg-surface-container-high p-6 sm:p-10">
          <div className="relative flex h-full w-full max-w-180 flex-1 flex-col items-center justify-center rounded-lg bg-card p-8 shadow-md sm:p-12">
            <EmptyDocumentState
              ready={ready}
              loading={generation.isLoading || template.isLoading}
            />
          </div>
        </div>
      )}

      {(viewMode === "typst" || viewMode === "split") && generation.data && (
        <div className="flex flex-wrap items-center gap-2 rounded-lg bg-card px-3 py-2 shadow-xs">
          <Button
            type="button"
            onClick={saveAndRegenerate}
            disabled={!dirty || save.isPending || regenerate.isPending}
            className="h-9 gap-1.5 rounded-lg bg-primary-900 px-3 text-xs font-semibold text-primary-foreground disabled:opacity-40"
          >
            <Save className="size-3.5" aria-hidden="true" />
            {regenerate.isPending ? "Gerando..." : "Salvar e gerar de novo"}
          </Button>
          <Button
            type="button"
            onClick={() =>
              bundled.data && setDraft(bundled.data.content ?? null)
            }
            disabled={!bundled.data}
            className="h-9 rounded-lg bg-surface-container px-3 text-xs font-semibold text-on-surface disabled:opacity-40"
          >
            Restaurar template padrão
          </Button>
          <span className="text-xs text-on-surface-variant">
            {dirty
              ? "Alterações não salvas: o PDF acima é só uma prévia."
              : `Template ${template.data?.name} v${template.data?.version}`}
          </span>
        </div>
      )}
      {actionError && (
        <p role="alert" className="text-sm text-error">
          {actionError}
        </p>
      )}

      {generation.data && (
        <div className="w-full">
          <AtsScoreCard generation={generation.data} />
          <div className="mt-4 flex w-full flex-wrap items-center justify-between gap-2 rounded-lg bg-surface-container-low px-3 py-2 text-xs text-on-surface-variant">
            <span>Gerado apenas a partir do seu Currículo Base aprovado.</span>
            <span className="flex items-center gap-1.5">
              <Sparkles className="size-3.25" aria-hidden="true" />A IA só
              ajusta a apresentação, nunca escreve fatos.
            </span>
          </div>
        </div>
      )}
    </div>
  );
}

function EmptyDocumentState({
  ready,
  loading,
}: {
  ready: boolean;
  loading: boolean;
}) {
  return (
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
  );
}
