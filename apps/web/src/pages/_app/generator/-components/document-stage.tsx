import { useCvPreview } from "@hooks/use-cv-preview";
import { tokenForOwnerKind } from "@queries/api-credential";
import { useSelectedGeneration } from "@queries/cv-generation";
import { useBaseCvStore } from "@stores/base-cv-store";
import { useCvGenerationStore } from "@stores/cv-generation-store";
import { useJobDraftStore } from "@stores/job-draft-store";
import { hasJobTarget } from "@/utils/job-draft";
import { useDocumentActions } from "../-hooks/use-document-actions";
import { useTypstTemplateDraft } from "../-hooks/use-typst-template-draft";
import type { ViewMode } from "../-types/view-mode";
import { CvPreview } from "./cv-preview";
import { EmptyDocumentState } from "./document-stage/empty-document-state";
import { GenerationFooter } from "./document-stage/generation-footer";
import { StageHeader } from "./document-stage/stage-header";
import { TemplateActionsBar } from "./document-stage/template-actions-bar";
import { TypstEditor } from "./typst-editor";

/**
 * Palco do currículo gerado: PDF, editor do template Typst ou os dois lado a lado. Reúne os hooks
 * de geração, rascunho do template e ações; a apresentação fica nos componentes de `document-stage/`.
 */
export function DocumentStage({
  viewMode,
  onViewModeChange,
}: {
  viewMode: ViewMode;
  onViewModeChange: (mode: ViewMode) => void;
}) {
  const baseCv = useBaseCvStore((state) => state.file);
  const { mode, jobUrl, jobText } = useJobDraftStore();
  const ready = Boolean(baseCv) && hasJobTarget(mode, jobUrl, jobText);

  const selected = useCvGenerationStore((state) => state.selected);
  const generation = useSelectedGeneration();
  const token = tokenForOwnerKind(selected?.ownerKind ?? null);

  const templateDraft = useTypstTemplateDraft(generation.data, token);
  const { draft, template } = templateDraft;
  const preview = useCvPreview(generation.data?.id ?? null, draft, token);
  const actions = useDocumentActions(generation.data, token, draft);

  const showsEditor = viewMode === "typst" || viewMode === "split";
  const showsPreview = viewMode === "visual" || viewMode === "split";

  return (
    <div className="flex h-full w-full flex-1 flex-col gap-4">
      <StageHeader
        generation={generation.data}
        ready={ready}
        viewMode={viewMode}
        onViewModeChange={onViewModeChange}
        onDownload={actions.download}
      />

      {generation.data && draft !== null ? (
        <div
          className={`grid min-h-[min(1120px,141.4vw)] flex-1 gap-4 ${
            viewMode === "split" ? "items-start lg:grid-cols-2" : ""
          }`}
        >
          {showsEditor && (
            <TypstEditor
              value={draft}
              onChange={templateDraft.setDraft}
              preview={preview}
            />
          )}
          {showsPreview && <CvPreview preview={preview} />}
        </div>
      ) : (
        <EmptyDocumentState
          ready={ready}
          loading={generation.isLoading || template.isLoading}
        />
      )}

      {showsEditor && generation.data && (
        <TemplateActionsBar
          dirty={templateDraft.dirty}
          busy={actions.busy}
          regenerating={actions.regenerating}
          canRestore={templateDraft.canRestore}
          templateLabel={`Template ${template.data?.name} v${template.data?.version}`}
          onSave={actions.saveAndRegenerate}
          onRestore={templateDraft.restore}
        />
      )}
      {actions.error && (
        <p role="alert" className="text-sm text-error">
          {actions.error}
        </p>
      )}

      {generation.data && <GenerationFooter generation={generation.data} />}
    </div>
  );
}
