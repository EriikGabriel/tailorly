import { AutoHeight } from "@animate/primitives/effects/auto-height";
import { AnonymousNotice } from "@components/anonymous-notice";
import { PageHeading } from "@components/page-heading";
import { useSelectBaseDocumentFile } from "@queries/base-document";
import {
  useDeactivateGroundTruth,
  useDeleteBaseVersion,
} from "@queries/base-version";
import { ShieldCheck } from "@react-zero-ui/icon-sprite";
import { useBaseCvStore } from "@stores/base-cv-store";
import { createFileRoute } from "@tanstack/react-router";
import { useReducedMotion } from "motion/react";
import { useRef } from "react";
import { BaseDocumentActions } from "./-components/base-document-actions";
import { ExtractionProgress } from "./-components/extraction-progress";
import { GovernanceSummary } from "./-components/governance-summary";
import {
  ReviewWorkspace,
  type ReviewWorkspaceHandle,
} from "./-components/review-workspace";
import { usePdfFilePicker } from "./-hooks/use-pdf-file-picker";
import { baseFileStatus } from "./-utils/file-status";

export const Route = createFileRoute("/_app/base/")({
  component: RouteComponent,
});

/** Auditoria do Currículo Base: envio do PDF, andamento da extração e revisão dos fatos. */
function RouteComponent() {
  const { file, error, selectionId, uploadedSelectionId, baseVersionId } =
    useBaseCvStore();
  const { selectFile, upload } = useSelectBaseDocumentFile();
  const picker = usePdfFilePicker(selectFile);
  const deactivate = useDeactivateGroundTruth();
  const deleteVersion = useDeleteBaseVersion();
  const reducedMotion = useReducedMotion();
  const reviewWorkspaceRef = useRef<ReviewWorkspaceHandle>(null);
  const uploadPending =
    file !== null && upload.variables === file && upload.isPending;
  const fileStatus = baseFileStatus({
    file,
    uploadPending,
    selectionId,
    uploadedSelectionId,
  });

  return (
    <main className="min-h-[calc(100dvh-4rem)] px-4 py-6 sm:px-8 lg:px-16">
      <section
        aria-labelledby="base-heading"
        className="mx-auto flex w-full max-w-384 flex-col gap-4 rounded-2xl  p-4 shadow-xs sm:p-6"
      >
        <PageHeading
          headingId="base-heading"
          breadcrumb="Auditoria & Curadoria do Ground Truth"
          status={fileStatus}
          eyebrowIcon={ShieldCheck}
          eyebrow="Cofre de conhecimento imutável"
          title="Auditoria & Curadoria dos Fatos do Currículo Base"
          description={
            <>
              Validação estruturada dos dados extraídos do seu currículo base.
              Revise fatos, credenciais e métricas para que ele seja a fonte das
              próximas adaptações, com{" "}
              <strong className="font-semibold text-primary-950">
                informações confirmadas por você
              </strong>
              .
            </>
          }
          actions={
            <BaseDocumentActions
              picker={picker}
              hasFile={file !== null}
              canApprove={Boolean(baseVersionId) && !uploadPending}
              onApprove={() => reviewWorkspaceRef.current?.approve()}
              canDeactivate={Boolean(baseVersionId) && !uploadPending}
              onDeactivate={() => deactivate.mutate()}
              deactivating={deactivate.isPending}
              canDelete={Boolean(baseVersionId) && !uploadPending}
              onDelete={() => {
                if (baseVersionId) deleteVersion.mutate(baseVersionId);
              }}
              deleting={deleteVersion.isPending}
            />
          }
        />
        <AutoHeight
          deps={[file, error, fileStatus]}
          transition={reducedMotion ? { duration: 0 } : undefined}
        >
          <div className="space-y-4 pt-1">
            <GovernanceSummary file={file} fileStatus={fileStatus} />

            <div aria-live="polite" className="sr-only">
              {file
                ? `${file.name} selecionado para prévia local.`
                : "Nenhum arquivo selecionado."}
            </div>
            <AnonymousNotice />
            {error && (
              <p role="alert" className="text-body-sm text-error">
                {error}
              </p>
            )}
            {upload.isSuccess && upload.variables === file && (
              <ExtractionProgress versionId={upload.data.version.id} />
            )}
          </div>
        </AutoHeight>
      </section>
      <ReviewWorkspace ref={reviewWorkspaceRef} onSelectFile={picker.open} />
    </main>
  );
}
