import {
  useAnswerReviewIssue,
  useBaseReview,
  useInvalidateBaseReview,
} from "@queries/base-review";
import {
  useJobPostingVersion,
  useSelectedJobPosting,
} from "@queries/job-posting";
import { useBaseCvStore } from "@stores/base-cv-store";
import { useReviewDraftStore } from "@stores/review-draft-store";
import { useMemo } from "react";
import type {
  ReviewAnswer,
  ReviewCategory,
  ReviewChoice,
  ReviewIssue,
} from "@/types/review";
import { factFieldLabel, formatFactValue } from "@/utils/fact-display";
import {
  matchJobToFacts,
  relevanceForFact,
  verifiableMatches,
} from "@/utils/job-fact-match";
import { applyAnswerToDraft } from "@/utils/review-draft-sync";

// Categorias dos fatos (backend) -> seções da revisão.
const reviewCategoryByFactCategory: Record<string, ReviewCategory> = {
  identity: "identity",
  experience: "employment",
  volunteering: "volunteering",
  projects: "projects",
  degrees: "education",
  certifications: "education",
  languages: "languages",
  technologies: "skills",
};

/**
 * Pendências do Currículo Base da versão atual, prontas para responder, com a relevância para a
 * vaga selecionada. Usado pelo diálogo de geração e pela auditoria completa, para que as duas
 * telas mostrem as mesmas perguntas e destaquem as mesmas prioridades.
 */
export function useReviewIssues() {
  const baseVersionId = useBaseCvStore((state) => state.baseVersionId);
  const review = useBaseReview(baseVersionId ?? undefined);
  const jobDetail = useSelectedJobPosting();
  const { version: jobVersion } = useJobPostingVersion(jobDetail.data);

  const jobMatch = useMemo(
    () =>
      jobVersion?.analysis && review.records.data
        ? matchJobToFacts(jobVersion.analysis, review.records.data)
        : null,
    [jobVersion?.analysis, review.records.data],
  );

  const issues = useMemo(
    () =>
      (review.issues.data ?? [])
        .filter((issue) => issue.status === "PENDING" && issue.fact)
        .map((issue): ReviewIssue => {
          const configured = issue.questionAndChoices ?? {};
          const fieldLabel = factFieldLabel(
            issue.fact?.fieldKey ?? "",
            issue.category,
          );
          const currentValue = formatFactValue(
            issue.fact?.reviewedValue ?? issue.fact?.extractedValue,
          );
          const rawChoices = Array.isArray(configured.choices)
            ? configured.choices
            : ["confirmado", "corrigido", "omitido"];
          const choices: ReviewChoice[] = rawChoices.flatMap(
            (raw): ReviewChoice[] => {
              if (raw === "confirmado")
                return [
                  { value: raw, label: "Está correto", result: "confirmed" },
                ];
              if (raw === "corrigido")
                return [
                  {
                    value: raw,
                    label: "Corrigir",
                    result: "corrected",
                    correction: {
                      label: `Valor correto para ${fieldLabel}`,
                      type: "text",
                      placeholder: currentValue,
                      originalValue: currentValue,
                    },
                  },
                ];
              if (raw === "omitido")
                return [
                  {
                    value: raw,
                    label: "Não usar no currículo",
                    result: "omitted",
                  },
                ];
              return [];
            },
          );
          return {
            id: issue.id,
            category:
              reviewCategoryByFactCategory[issue.category] ?? "identity",
            type: "fact",
            question: `${fieldLabel} está correto?`,
            fieldLabel,
            value: currentValue,
            evidence: issue.fact?.evidence[0]?.sourceExcerpt,
            source: issue.fact
              ? {
                  category: issue.category,
                  recordId: issue.fact.recordId,
                  fieldKey: issue.fact.fieldKey,
                  value: String(
                    issue.fact.reviewedValue ?? issue.fact.extractedValue ?? "",
                  ),
                }
              : undefined,
            jobRelevance:
              jobMatch && issue.fact
                ? relevanceForFact(jobMatch, issue.fact)
                : undefined,
            motivation: issue.reason,
            choices,
          };
        }),
    [review.issues.data, jobMatch],
  );

  // A vaga só prioriza quando nomeia algo que dá para procurar nos fatos; sem isso (IA indisponível,
  // vaga sem tecnologias) todas as pendências têm o mesmo peso.
  const verifiable = jobMatch ? verifiableMatches(jobMatch) : [];
  const canPrioritize = verifiable.length > 0;
  const relevantIssues = useMemo(
    () =>
      issues
        .filter((issue) => issue.jobRelevance)
        .sort(
          (a, b) =>
            Number(b.jobRelevance?.importance === "required") -
            Number(a.jobRelevance?.importance === "required"),
        ),
    [issues],
  );

  return {
    baseVersionId,
    review,
    issues,
    relevantIssues,
    canPrioritize,
    verifiable,
    jobMatch,
    jobVersion,
  };
}

/** Salva as respostas das pendências na versão do Currículo Base. */
export function useSubmitReviewAnswers(baseVersionId: string) {
  const answerIssue = useAnswerReviewIssue(baseVersionId);
  const invalidate = useInvalidateBaseReview(baseVersionId);

  // O rascunho da auditoria não pode ficar com o valor antigo: o próximo "Salvar" o regravaria.
  function syncDraft(
    issue: ReviewIssue,
    result: ReviewChoice["result"],
    correction: string,
  ) {
    if (!issue.source) return;
    const state = useReviewDraftStore.getState();
    const next = applyAnswerToDraft(
      state.draft,
      issue.source,
      result,
      correction,
    );
    if (next === state.draft) return;
    useReviewDraftStore.setState({ draft: next });
  }

  /**
   * Responde uma a uma e devolve as respondidas. Em falha, o que já foi salvo continua salvo e a
   * lista é atualizada mesmo assim, para a tela não mostrar como pendente o que já foi respondido.
   */
  async function submit(
    items: readonly { issue: ReviewIssue; answer: ReviewAnswer }[],
  ): Promise<ReviewIssue[]> {
    const saved: ReviewIssue[] = [];
    try {
      for (const { issue, answer } of items) {
        const choice = issue.choices.find(
          (item) => item.value === answer.choice,
        );
        if (!choice) continue;
        const reviewState =
          choice.result === "confirmed"
            ? "confirmado"
            : choice.result === "corrected"
              ? "corrigido"
              : "omitido";
        await answerIssue.mutateAsync({
          id: issue.id,
          answer: {
            reviewState,
            ...(choice.result === "corrected"
              ? { reviewedValue: answer.correction }
              : {}),
          },
        });
        saved.push(issue);
        syncDraft(issue, choice.result, answer.correction);
      }
    } finally {
      invalidate();
    }
    return saved;
  }

  return { submit, isPending: answerIssue.isPending };
}
