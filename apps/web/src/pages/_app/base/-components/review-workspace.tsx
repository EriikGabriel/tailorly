import {
  type CollectionProps,
  createReviewDraft,
  type DraftCollection,
  type ReviewStepId,
  reviewSteps,
} from "@@types/review-model";
import { Button } from "@animate/buttons/button";
import { AutoHeight } from "@animate/primitives/effects/auto-height";
import {
  useReviewIssues,
  useSubmitReviewAnswers,
} from "@hooks/use-review-issues";
import { apiErrorMessage } from "@lib/api/error-message";
import {
  useAnswerReviewIssue,
  useApproveGroundTruth,
  useCreateFact,
  useCreateFactRecord,
  useInvalidateBaseReview,
  useUpdateFactReviewState,
} from "@queries/base-review";
import { ArrowLeft, ArrowRight } from "@react-zero-ui/icon-sprite";
import { useBaseCvStore } from "@stores/base-cv-store";
import { useJobDraftStore } from "@stores/job-draft-store";
import { usePresentationStore } from "@stores/presentation-store";
import { useReviewDraftStore } from "@stores/review-draft-store";
import { useNavigate } from "@tanstack/react-router";
import { AnimatePresence, motion, useReducedMotion } from "motion/react";
import {
  forwardRef,
  useEffect,
  useImperativeHandle,
  useRef,
  useState,
} from "react";
import type { ReviewIssue } from "@/types/review";
import { firstStepWithPending, pendingByStep } from "@/utils/review-step-map";
import { clearSteps } from "@/utils/review-verification";
import { EducationReviewStep } from "../-steps/education-review-step";
import { ExperienceReviewStep } from "../-steps/experience-review-step";
import { IdentityReviewStep } from "../-steps/identity-review-step";
import { PreferencesReviewStep } from "../-steps/preferences-review-step";
import { ProjectsReviewStep } from "../-steps/projects-review-step";
import { SkillsReviewStep } from "../-steps/skills-review-step";
import { STEP_CATEGORIES } from "./evidence-style";
import { MasterDocumentPanel } from "./master-document-panel";
import { PendingFieldsProvider } from "./pending-fields";
import { ReviewCompletionCard } from "./review-completion-card";
import { ReviewStepper } from "./review-stepper";

export type ReviewWorkspaceHandle = {
  approve: () => void;
};

export const ReviewWorkspace = forwardRef<
  ReviewWorkspaceHandle,
  { onSelectFile: () => void }
>(function ReviewWorkspace({ onSelectFile }, ref) {
  const {
    sourceId,
    activeStep,
    setActiveStep,
    draft,
    setIdentity,
    setCollection,
    expandedRecords,
  } = useReviewDraftStore();
  // O card de uma entrada específica (ex.: "Experiência 2") aberto no
  // formulário à direita é o que define qual bounding box fica em foco no
  // documento à esquerda — não a etapa inteira.
  const focusedRecordKeys = new Set(
    STEP_CATEGORIES[activeStep]
      .map((category) => expandedRecords[category as DraftCollection])
      .filter((id): id is string => Boolean(id)),
  );
  const baseVersionId = useBaseCvStore((state) => state.baseVersionId);
  const { mode: jobMode, jobUrl, jobText } = useJobDraftStore();
  const hasJobTarget =
    jobMode === "url" ? jobUrl.trim().length > 0 : jobText.trim().length > 0;
  const { review, issues: pendingIssues } = useReviewIssues();
  const answerIssue = useAnswerReviewIssue(baseVersionId ?? "");
  const invalidateReview = useInvalidateBaseReview(baseVersionId ?? "");
  const submitAnswers = useSubmitReviewAnswers(baseVersionId ?? "");
  const pending = pendingByStep(pendingIssues);
  const clearByStep = clearSteps(
    review.records.data ?? [],
    Object.fromEntries(
      Object.entries(pending).map(([step, value]) => [step, value.total]),
    ) as Record<ReviewStepId, number>,
    STEP_CATEGORIES,
  );
  const createRecord = useCreateFactRecord(baseVersionId ?? "");
  const createFact = useCreateFact(baseVersionId ?? "");
  const updateFact = useUpdateFactReviewState(baseVersionId ?? "");
  const [confirmError, setConfirmError] = useState<string | null>(null);
  const approveGroundTruth = useApproveGroundTruth(baseVersionId ?? "");
  const navigate = useNavigate();
  const hydrateFromApi = useReviewDraftStore((state) => state.hydrateFromApi);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [approvalError, setApprovalError] = useState<string | null>(null);
  const approveRef = useRef<() => void>(() => {});
  const { preferences, setPreferences } = usePresentationStore();
  const reducedMotion = useReducedMotion();

  useEffect(() => {
    if (!review.records.isSuccess || !sourceId) return;

    const hydrated = createReviewDraft();

    for (const record of review.records.data) {
      if (record.category === "identity") {
        for (const fact of record.facts) {
          if (fact.reviewState !== "omitido") {
            hydrated.identity[fact.fieldKey] = String(
              fact.reviewedValue ?? fact.extractedValue ?? "",
            );
          }
        }

        continue;
      }

      if (
        ![
          "experience",
          "volunteering",
          "projects",
          "degrees",
          "certifications",
          "languages",
          "technologies",
        ].includes(record.category)
      )
        continue;

      const collection = record.category as DraftCollection;
      const rowValues = new Map<number, Record<string, string>>();

      for (const fact of record.facts) {
        if (fact.reviewState === "omitido") continue;
        const match = /^(\d+):(.*)$/.exec(fact.fieldKey);
        const index = match ? Number(match[1]) : 0;
        const key = match ? match[2] : fact.fieldKey;
        const values = rowValues.get(index) ?? {};
        values[key] = String(fact.reviewedValue ?? fact.extractedValue ?? "");
        rowValues.set(index, values);
      }

      hydrated[collection] = [...rowValues.entries()]
        .sort(([left], [right]) => left - right)
        .map(([index, fields]) => ({ id: `${record.id}-${index}`, fields }));
    }

    hydrateFromApi(sourceId, hydrated);
  }, [hydrateFromApi, review.records.data, review.records.isSuccess, sourceId]);

  async function saveDraft() {
    if (!baseVersionId) {
      setSaveError("Envie o Currículo Base antes de salvar os fatos.");
      return false;
    }
    if (review.records.isLoading) return false;
    setSaving(true);
    setSaveError(null);
    try {
      const groups: { category: string; fields: Record<string, string> }[] = [];
      if (
        Object.values(draft.identity).some(Boolean) ||
        review.records.data?.some((item) => item.category === "identity")
      ) {
        groups.push({ category: "identity", fields: draft.identity });
      }
      const collections: DraftCollection[] = [
        "experience",
        "volunteering",
        "projects",
        "degrees",
        "certifications",
        "languages",
        "technologies",
      ];
      for (const collectionKey of collections) {
        const fields: Record<string, string> = {};
        draft[collectionKey].forEach((record, index) => {
          for (const [key, value] of Object.entries(record.fields)) {
            if (value.trim()) fields[`${index}:${key}`] = value.trim();
          }
        });
        if (
          Object.keys(fields).length ||
          review.records.data?.some((item) => item.category === collectionKey)
        ) {
          groups.push({ category: collectionKey, fields });
        }
      }

      // Editar ou remover um campo pendente resolve a pendência (a resposta registra a decisão);
      // só atualizar o fato deixaria a pendência aberta e a aprovação bloqueada.
      const openIssueByFact = new Map(
        (review.issues.data ?? [])
          .filter((issue) => issue.status === "PENDING" && issue.factId)
          .map((issue) => [issue.factId as string, issue.id]),
      );

      for (const group of groups) {
        let record = review.records.data?.find(
          (item) => item.category === group.category,
        );
        if (!record) {
          record = await createRecord.mutateAsync({
            baseVersionId,
            category: group.category,
            displayOrder: groups.indexOf(group),
          });
        }
        const existing = new Map(
          record.facts.map((fact) => [fact.fieldKey, fact]),
        );
        for (const [fieldKey, value] of Object.entries(group.fields)) {
          const fact = existing.get(fieldKey);
          if (!fact) {
            await createFact.mutateAsync({
              recordId: record.id,
              fieldKey,
              reviewedValue: value,
              origin: "manual_entry",
              reviewState: "pendente",
            });
            continue;
          }
          const currentValue = String(
            fact.reviewedValue ?? fact.extractedValue ?? "",
          );
          if (currentValue !== value) {
            const issueId = openIssueByFact.get(fact.id);
            if (issueId) {
              await answerIssue.mutateAsync({
                id: issueId,
                answer: { reviewState: "corrigido", reviewedValue: value },
              });
            } else {
              await updateFact.mutateAsync({
                id: fact.id,
                reviewState: "corrigido",
                reviewedValue: value,
              });
            }
          }
        }
        for (const fact of record.facts) {
          if (
            fact.reviewState !== "omitido" &&
            !(fact.fieldKey in group.fields)
          ) {
            const issueId = openIssueByFact.get(fact.id);
            if (issueId) {
              await answerIssue.mutateAsync({
                id: issueId,
                answer: { reviewState: "omitido" },
              });
            } else {
              await updateFact.mutateAsync({
                id: fact.id,
                reviewState: "omitido",
              });
            }
          }
        }
      }
      invalidateReview();
      return true;
    } catch (cause) {
      invalidateReview();
      setSaveError(apiErrorMessage(cause, "Não foi possível salvar os fatos."));
      return false;
    } finally {
      setSaving(false);
    }
  }

  async function approve() {
    if (!(await saveDraft())) return;
    const refreshedIssues = await review.issues.refetch();
    const unresolved = (refreshedIssues.data ?? []).some(
      (issue) => issue.blocking && issue.status === "PENDING",
    );
    if (unresolved) {
      reviewPendingIssues();
      return;
    }
    setApprovalError(null);
    try {
      await approveGroundTruth.mutateAsync();
      // Com uma vaga já em andamento (o caso comum de chegar aqui vindo do fluxo de geração),
      // continua para o gerador em vez de voltar ao início e perder o contexto.
      navigate({ to: hasJobTarget ? "/generator" : "/" });
    } catch (cause) {
      setApprovalError(
        apiErrorMessage(cause, "Não foi possível aprovar o Ground Truth."),
      );
    }
  }

  approveRef.current = approve;
  useImperativeHandle(
    ref,
    () => ({
      approve: () => approveRef.current(),
    }),
    [],
  );

  async function confirmIssue(issue: ReviewIssue) {
    setConfirmError(null);
    try {
      await submitAnswers.submit([
        { issue, answer: { choice: "confirmado", correction: "" } },
      ]);
    } catch (cause) {
      setConfirmError(
        apiErrorMessage(cause, "Não foi possível confirmar o valor."),
      );
    }
  }

  // Resolve aqui mesmo: abre a etapa com as pendências mais importantes para a vaga.
  function reviewPendingIssues() {
    const step = firstStepWithPending(pending);
    if (step) setActiveStep(step);
    document
      .getElementById("review-active-panel")
      ?.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  function collection(key: DraftCollection): CollectionProps {
    return {
      collectionKey: key,
      records: draft[key],
      onChange: (records) => setCollection(sourceId, key, records),
    };
  }

  function renderStep() {
    switch (activeStep) {
      case "identity":
        return (
          <IdentityReviewStep
            values={draft.identity}
            onChange={(identity) => setIdentity(sourceId, identity)}
          />
        );
      case "experience":
        return (
          <ExperienceReviewStep
            experience={collection("experience")}
            volunteering={collection("volunteering")}
          />
        );
      case "projects":
        return <ProjectsReviewStep {...collection("projects")} />;
      case "education":
        return (
          <EducationReviewStep
            degrees={collection("degrees")}
            certifications={collection("certifications")}
          />
        );
      case "skills":
        return (
          <SkillsReviewStep
            languages={collection("languages")}
            technologies={collection("technologies")}
          />
        );
      case "preferences":
        return (
          <PreferencesReviewStep
            value={preferences}
            onChange={setPreferences}
          />
        );
    }
  }

  const stepIndex = reviewSteps.findIndex((step) => step.id === activeStep);
  return (
    <PendingFieldsProvider
      issues={pendingIssues}
      records={review.records.data ?? []}
      clearSteps={clearByStep}
      onConfirm={confirmIssue}
      busy={submitAnswers.isPending}
    >
      <ReviewStepper
        activeStep={activeStep}
        onStepChange={setActiveStep}
        pending={pending}
        clear={clearByStep}
      />
      <div className="mx-auto mt-6 grid w-full max-w-384 items-start gap-6 xl:grid-cols-[minmax(0,5fr)_minmax(0,7fr)]">
        <MasterDocumentPanel
          onSelectFile={onSelectFile}
          extractedPages={review.pages.data ?? []}
          extractionLoading={review.pages.isLoading}
          records={review.records.data ?? []}
          activeStep={activeStep}
          focusedRecordKeys={focusedRecordKeys}
        />
        <div className="min-w-0 space-y-4">
          <section
            id="review-active-panel"
            aria-label={reviewSteps[stepIndex].title}
          >
            <AutoHeight
              deps={[activeStep]}
              transition={reducedMotion ? { duration: 0 } : undefined}
            >
              <AnimatePresence initial={false} mode="wait">
                <motion.div
                  key={activeStep}
                  initial={{ opacity: 0, y: reducedMotion ? 0 : 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: reducedMotion ? 0 : -8 }}
                  transition={{ duration: reducedMotion ? 0 : 0.18 }}
                  className="p-0.5"
                >
                  {renderStep()}
                </motion.div>
              </AnimatePresence>
            </AutoHeight>
          </section>
          {confirmError && (
            <p role="alert" className="text-sm text-error">
              {confirmError}
            </p>
          )}
          <nav
            aria-label="Navegação entre etapas"
            className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-card border p-4"
          >
            <Button
              variant="accent"
              disabled={stepIndex === 0}
              onClick={() => setActiveStep(reviewSteps[stepIndex - 1].id)}
              className="rounded-lg"
            >
              <ArrowLeft aria-hidden="true" className="size-4" />
              Anterior
            </Button>
            <p aria-live="polite" className="text-xs font-medium">
              Etapa {stepIndex + 1} de {reviewSteps.length}
            </p>
            <Button
              disabled={stepIndex === reviewSteps.length - 1}
              onClick={() => setActiveStep(reviewSteps[stepIndex + 1].id)}
              className="rounded-lg"
            >
              Próxima
              <ArrowRight aria-hidden="true" className="size-4" />
            </Button>
          </nav>
          {activeStep === "preferences" && (
            <ReviewCompletionCard
              onSave={saveDraft}
              onApprove={approve}
              onReviewIssues={reviewPendingIssues}
              saving={saving}
              approving={approveGroundTruth.isPending}
              disabled={!baseVersionId || review.records.isLoading || saving}
              error={saveError ?? approvalError}
              pendingCount={
                (review.issues.data ?? []).filter(
                  (issue) => issue.blocking && issue.status === "PENDING",
                ).length
              }
            />
          )}
        </div>
      </div>
    </PendingFieldsProvider>
  );
});
