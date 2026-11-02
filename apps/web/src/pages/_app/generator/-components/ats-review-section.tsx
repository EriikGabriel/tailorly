import type {
  AtsReview,
  AtsReviewRequirementCheck,
  CvGenerationResponse,
} from "@api/generated/model";
import { AiStageLabel } from "@components/ai-progress/ai-stage-label";
import { cn } from "@lib/utils";
import { useAtsReview } from "@queries/cv-generation";
import { useCvGenerationStore } from "@stores/cv-generation-store";
import { useEffect, useState } from "react";

const STATUS: Record<string, { label: string; tone: string }> = {
  met: { label: "Atendido", tone: "bg-tertiary-50 text-on-tertiary-container" },
  partial: {
    label: "Parcial",
    tone: "bg-secondary-container text-on-secondary-container",
  },
  missing: {
    label: "Ausente",
    tone: "bg-error-container text-on-error-container",
  },
};

const FAILURE: Record<string, string> = {
  unavailable:
    "Sem IA configurada: configure um provedor no perfil para analisar.",
  failed: "A IA não conseguiu analisar agora. Tente de novo em instantes.",
};

function RequirementRow({ check }: { check: AtsReviewRequirementCheck }) {
  const status = STATUS[check.status] ?? STATUS.missing;
  return (
    <li className="flex flex-col gap-0.5 rounded-lg bg-surface-container-low p-2.5">
      <div className="flex items-start justify-between gap-2">
        <span className="text-xs font-medium text-on-surface">
          {check.requirement}
          {check.importance === "required" && (
            <span className="ml-1 text-[11px] font-normal text-on-surface-variant">
              (obrigatório)
            </span>
          )}
        </span>
        <span
          className={cn(
            "shrink-0 rounded-full px-2 py-0.5 text-[11px] font-semibold",
            status.tone,
          )}
        >
          {status.label}
        </span>
      </div>
      {check.evidence && (
        <q className="text-[11px] italic text-on-surface-variant">
          {check.evidence}
        </q>
      )}
      {check.note && (
        <span className="text-[11px] text-on-surface-variant">
          {check.note}
        </span>
      )}
    </li>
  );
}

/** A leitura completa da IA: requisito a requisito, pontos fortes e sugestões (vive no diálogo). */
export function AtsReviewDetails({ review }: { review: AtsReview }) {
  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-baseline justify-between gap-3">
        <p className="text-xs leading-5 text-on-surface-variant">
          {review.summary}
        </p>
        <span className="shrink-0 text-lg font-semibold tabular-nums text-primary-900">
          {review.alignment}%
          <span className="sr-only"> de aderência à vaga segundo a IA</span>
        </span>
      </div>
      <ul className="flex flex-col gap-1.5">
        {review.requirements.map((check) => (
          <RequirementRow key={check.requirement} check={check} />
        ))}
      </ul>
      {review.strengths.length > 0 && (
        <div className="text-xs text-on-surface-variant">
          <p className="font-semibold text-on-surface">Pontos fortes</p>
          <ul className="list-disc pl-4">
            {review.strengths.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </div>
      )}
      {review.suggestions.length > 0 && (
        <div className="text-xs text-on-surface-variant">
          <p className="font-semibold text-on-surface">
            Sugestões de apresentação
          </p>
          <ul className="list-disc pl-4">
            {review.suggestions.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
          <p className="mt-1">
            São só sugestões: nada é alterado sem você, e fatos novos só entram
            pelo Currículo Base.
          </p>
        </div>
      )}
    </div>
  );
}

function counts(review: AtsReview) {
  const total = (status: string) =>
    review.requirements.filter((check) => check.status === status).length;
  return {
    met: total("met"),
    partial: total("partial"),
    missing: total("missing"),
  };
}

/**
 * Parte compacta da leitura da IA, na própria coluna: aderência, contagem por situação e o botão de
 * analisar. O detalhe por requisito fica no diálogo "Ver análise completa".
 */
export function AtsReviewSection({
  generation,
}: {
  generation: CvGenerationResponse;
  onOpenDetails: () => void;
}) {
  const analyze = useAtsReview(generation.id);
  const [manual, setManual] = useState(false);
  const generationId = generation.id;
  const hasReview = Boolean(generation.atsReview);
  const { mutate } = analyze;

  // Um currículo recém-gerado é lido pela IA sem o clique; o histórico só quando a pessoa pede.
  useEffect(() => {
    if (!generationId || hasReview) return;
    if (useCvGenerationStore.getState().consumeAutoReview(generationId)) {
      mutate(undefined);
    }
  }, [generationId, hasReview, mutate]);

  const review =
    analyze.data?.status === "completed" ? analyze.data : generation.atsReview;
  const failed =
    analyze.data && analyze.data.status !== "completed"
      ? analyze.data.status
      : null;
  const summary = review ? counts(review) : null;

  return (
    <div className="flex flex-col gap-2 border-t border-outline-variant/50 pt-3">
      <div className="flex items-center justify-between gap-3">
        <div className="min-w-0">
          <h4 className="text-sm font-semibold text-on-surface">
            Leitura da IA
          </h4>
          {summary && review ? (
            <p className="text-xs text-on-surface-variant">
              <strong className="font-semibold text-on-surface">
                {review.alignment}%
              </strong>{" "}
              de aderência · {summary.met} atendidos · {summary.partial}{" "}
              parciais · {summary.missing} ausentes
            </p>
          ) : (
            <p className="text-xs text-on-surface-variant">
              Confere cada requisito da vaga no texto que o parser extraiu.
            </p>
          )}
        </div>
        <button
          type="button"
          onClick={() => {
            setManual(true);
            analyze.mutate({ refresh: Boolean(review) });
          }}
          disabled={analyze.isPending}
          className="shrink-0 rounded-lg bg-surface-container px-2.5 py-1.5 text-xs font-medium text-on-surface hover:bg-surface-container-high disabled:opacity-60"
        >
          {analyze.isPending ? (
            <AiStageLabel
              orb="solving"
              label="Lendo como um triador"
              theme="light"
            />
          ) : review ? (
            "Analisar de novo"
          ) : (
            "Analisar com IA"
          )}
        </button>
      </div>
      {review?.summary && (
        <p className="line-clamp-2 text-xs leading-5 text-on-surface-variant">
          {review.summary}
        </p>
      )}
      {failed && (manual || failed !== "unavailable") && (
        <p role="alert" className="text-xs text-error">
          {FAILURE[failed] ?? FAILURE.failed}
        </p>
      )}
      {analyze.isError && (
        <p role="alert" className="text-xs text-error">
          {analyze.error.message}
        </p>
      )}
    </div>
  );
}
