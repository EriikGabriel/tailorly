import { Button } from "@components/ui/animate/buttons/button";
import {
  Dialog,
  DialogBackdrop,
  DialogDescription,
  DialogPopup,
  DialogPortal,
  DialogTitle,
} from "@components/ui/animate/primitives/base/dialog";
import {
  AiSparklesIcon,
  ArrowRight02Icon,
  Cancel01Icon,
  ExclamationMarkBigIcon,
  ListTodoIcon,
} from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { Link, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import type {
  ReviewAnswer,
  ReviewAnswers,
  ReviewCategory,
  ReviewIssue,
} from "@/types/review";
import {
  isReviewIssueComplete,
  selectQuickReviewIssues,
} from "@/utils/review-issues";
import { ReviewSection } from "./review-section";

type ValidateDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSaveDraft: () => void;
  file?: File | null;
  issues?: readonly ReviewIssue[];
  requiresFullReview?: boolean;
};

export const previewIssues: readonly ReviewIssue[] = [
  {
    id: "employment-current",
    category: "employment",
    type: "temporal",
    question: "Você ainda atua como Tech Lead na FinTech Solutions?",
    evidence: "Tech Lead (2021–presente) — FinTech Solutions.",
    motivation: "A vaga pede disponibilidade integral ao longo do período.",
    choices: [
      { value: "current", label: "Sim, ainda atuo", result: "confirmed" },
      {
        value: "ended",
        label: "Não, o vínculo terminou",
        result: "corrected",
        correction: {
          label: "Mês e ano de saída",
          type: "month",
          min: "2021-01",
        },
      },
    ],
  },
  {
    id: "project-savings",
    category: "projects",
    type: "metric",
    question:
      "A economia com a migração de microsserviços foi de US$ 45.000 por ano?",
    evidence:
      "...conduziu programa FinOps com economia comprovada de $45.000/ano na nuvem AWS.",
    motivation:
      "A vaga valoriza fortemente eficiência em Cloud (FinOps). Confirmar essa métrica factual fortalece o alinhamento com os requisitos da vaga.",
    choices: [
      { value: "confirmed", label: "Confirmar valor", result: "confirmed" },
      {
        value: "edit",
        label: "Corrigir valor",
        result: "corrected",
        correction: {
          label: "Economia anual em US$",
          type: "number",
          min: "1",
          placeholder: "Novo valor anual",
          originalValue: "45000",
        },
      },
      { value: "omit", label: "Não incluir a métrica", result: "omitted" },
    ],
  },
];

export function ValidateDialog({
  open,
  onOpenChange,
  onSaveDraft,
  file,
  issues,
  requiresFullReview = false,
}: ValidateDialogProps) {
  const navigate = useNavigate();
  const [answers, setAnswers] = useState<ReviewAnswers>({});
  const [answerSource, setAnswerSource] = useState({ file, issues });
  const allIssues = issues ?? previewIssues;
  const quickIssues = selectQuickReviewIssues(allIssues);
  const needsFullReview =
    requiresFullReview || quickIssues.length < allIssues.length;
  const reviewComplete = quickIssues.every((issue) =>
    isReviewIssueComplete(issue, answers[issue.id]),
  );
  const categories = [...new Set(quickIssues.map((issue) => issue.category))];

  if (answerSource.file !== file || answerSource.issues !== issues) {
    setAnswerSource({ file, issues });
    setAnswers({});
  }

  function updateAnswer(id: string, answer: ReviewAnswer) {
    setAnswers((current) => ({ ...current, [id]: answer }));
  }

  function finishReview() {
    if (needsFullReview || !reviewComplete) return;
    onSaveDraft();
    sessionStorage.setItem(
      "tailorly:review-preview",
      JSON.stringify({
        preview: issues === undefined,
        answers: Object.fromEntries(
          quickIssues.map((issue) => [issue.id, answers[issue.id]]),
        ),
      }),
    );
    onOpenChange(false);
    navigate({ to: "/generator" });
  }

  const count = quickIssues.length;
  const heading =
    count === 0
      ? "Tudo pronto para gerar"
      : `Confirme ${count} ${count === 1 ? "informação" : "informações"} antes de gerar`;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogPortal>
        <DialogBackdrop className="fixed inset-0 z-50 bg-primary-950/65" />
        <DialogPopup
          className="flex max-h-[min(90dvh,760px)] w-[min(680px,calc(100vw-2rem))] max-w-none flex-col gap-0 overflow-hidden rounded-xl border border-primary-200 bg-card p-0 text-primary-950 shadow-2xl"
          style={{
            position: "fixed",
            top: "50%",
            left: "50%",
            translate: "-50% -50%",
            zIndex: 51,
          }}
        >
          <div className="shrink-0 border-b border-outline-variant/30 bg-surface-container-low px-5 py-5 sm:px-7">
            <div className="flex items-start justify-between gap-3">
              <div className="flex flex-col gap-2">
                <DialogTitle className="flex items-center gap-2 text-lg font-semibold leading-6">
                  <span className="flex size-8 items-center justify-center rounded-lg bg-primary text-white">
                    <HugeiconsIcon icon={ListTodoIcon} className="size-4.5" />
                  </span>
                  {heading}
                </DialogTitle>
                <span className="w-fit rounded-full border bg-white px-3 py-0.5 text-[10px] font-bold uppercase leading-4 tracking-[0.3px] text-tertiary">
                  {`${count} Dúvidas rápidas detectadas`}
                </span>
                <DialogDescription className="text-sm leading-5 text-on-surface-variant">
                  A IA fez uma verificação silenciosa com base nos requisitos da
                  vaga. Ajuste apenas o que impacta este currículo.
                </DialogDescription>
              </div>
              <Button
                variant="ghost"
                type="button"
                onClick={() => onOpenChange(false)}
                aria-label="Fechar validação"
                className="rounded-md p-2 text-on-surface-variant hover:bg-surface-container focus-visible:outline-2 focus-visible:outline-primary-900"
              >
                <HugeiconsIcon icon={Cancel01Icon} className="size-4" />
              </Button>
            </div>
          </div>

          <div className="min-h-0 space-y-4 overflow-y-auto overscroll-contain px-5 py-5 sm:px-7">
            <div className="flex flex-wrap items-center justify-between gap-2 border-b border-outline-variant/30 pb-1.5">
              <p className="flex items-center text-xs font-bold uppercase tracking-wide text-secondary-700">
                <HugeiconsIcon
                  icon={ExclamationMarkBigIcon}
                  className="size-4.5 fill-tertiary text-tertiary"
                />
                {count === 0
                  ? "Nenhuma pendência"
                  : "Itens que exigem sua confirmação"}
              </p>
              {file && (
                <p
                  className="max-w-full py-1 truncate text-xs"
                  title={file.name}
                >
                  Currículo Base:{" "}
                  <span className="font-semibold">{file.name}</span>
                </p>
              )}
            </div>

            {needsFullReview && (
              <p className="rounded-lg border border-outline-variant/40 bg-surface-container-low p-3 text-sm text-on-surface-variant">
                {allIssues.length > count
                  ? `Mostrando ${count} de ${allIssues.length} pendências. `
                  : "Este currículo exige revisão de mais detalhes. "}
                Continue na revisão completa antes de gerar.
              </p>
            )}

            {categories.map((category: ReviewCategory) => (
              <ReviewSection
                key={category}
                category={category}
                issues={quickIssues.filter(
                  (issue) => issue.category === category,
                )}
                answers={answers}
                onAnswerChange={updateAnswer}
              />
            ))}
            {count === 0 && !needsFullReview && (
              <p className="text-sm text-on-surface-variant">
                Não há informações a confirmar nesta versão.
              </p>
            )}
          </div>

          <div className="flex shrink-0 flex-col gap-3 bg-surface-container-low border-t border-outline-variant/30 px-5 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-7">
            <Link
              to="/cvs"
              onClick={onSaveDraft}
              className="text-xs font-semibold text-secondary-700 underline underline-offset-2 hover:text-primary-900"
            >
              Revisar currículo completo
            </Link>
            <div className="flex flex-wrap gap-4">
              <Button
                variant="outline"
                type="button"
                onClick={() => onOpenChange(false)}
                className="rounded-xl border border-outline-variant/50 px-6 py-6 text-sm font-medium hover:bg-surface-container"
              >
                Agora não
              </Button>
              {needsFullReview ? (
                <Link
                  to="/cvs"
                  onClick={onSaveDraft}
                  className="rounded-xl bg-primary-900 px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary-800"
                >
                  Ir para revisão completa
                </Link>
              ) : (
                <Button
                  type="button"
                  onClick={finishReview}
                  disabled={!reviewComplete}
                  className="rounded-xl bg-primary-900 px-6! py-6 text-sm font-semibold text-primary-foreground hover:bg-primary-800 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  <HugeiconsIcon
                    icon={AiSparklesIcon}
                    className="inline-block size-12 zoom-40 shrink-0 mr-4"
                    aria-hidden="true"
                  />
                  Confirmar e gerar currículo
                  <HugeiconsIcon
                    icon={ArrowRight02Icon}
                    className="inline-block size-12 zoom-40 shrink-0 mr-4"
                    aria-hidden="true"
                  />
                </Button>
              )}
            </div>
          </div>
        </DialogPopup>
      </DialogPortal>
    </Dialog>
  );
}
