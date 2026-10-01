import { Button } from "@animate/primitives/buttons/button";
import { AutoHeight } from "@animate/primitives/effects/auto-height";
import {
  ArrangeByNumbersOneNineIcon,
  CalendarDate1Icon,
  CheckIcon,
  EyeOffIcon,
  ListIcon,
  Mail01Icon,
  QuoteIcon,
  TextIcon,
} from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { cn } from "cn";
import { useReducedMotion } from "motion/react";
import type {
  CorrectionField,
  ReviewAnswer,
  ReviewCategory,
  ReviewChoice,
  ReviewIssue,
} from "@/types/review";
import { isReviewIssueComplete, reviewCategories } from "@/utils/review-issues";
import { ReviewCorrectionInput } from "./review-correction-input";

type ReviewSectionProps = {
  category: ReviewCategory;
  issues: readonly ReviewIssue[];
  answers: Record<string, ReviewAnswer>;
  onAnswerChange: (id: string, answer: ReviewAnswer) => void;
};

const correctionIcons = {
  text: TextIcon,
  email: Mail01Icon,
  month: CalendarDate1Icon,
  number: ArrangeByNumbersOneNineIcon,
  select: ListIcon,
} satisfies Record<CorrectionField["type"], typeof TextIcon>;

function getChoiceIcon(choice: ReviewChoice) {
  switch (choice.result) {
    case "confirmed":
      return CheckIcon;
    case "omitted":
      return EyeOffIcon;
    case "corrected":
      return correctionIcons[choice.correction.type];
  }
}

export function ReviewSection({
  category,
  issues,
  answers,
  onAnswerChange,
}: ReviewSectionProps) {
  const reducedMotion = useReducedMotion();
  const complete = issues.every((issue) =>
    isReviewIssueComplete(issue, answers[issue.id]),
  );

  return (
    <section
      className="rounded-lg border border-outline-variant/40 bg-surface-container-low p-4"
      aria-labelledby={`review-${category}-heading`}
    >
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center justify-center h-fit gap-3">
          <h3
            id={`review-${category}-heading`}
            className="text-sm font-semibold"
          >
            {reviewCategories[category]}
          </h3>
          <span className="flex items-center justify-center w-fit rounded-full py-0.5 px-3 border border-secondary-200 bg-secondary-container/40 text-[10px] font-semibold leading-4 tracking-[0.3px] text-secondary-800">
            {issues[0]?.type === "metric"
              ? "Métrica relevante para esta vaga"
              : "Dúvida Temporal"}
          </span>
        </div>
        <span
          className={cn(
            "rounded-full px-2 py-0.5 text-[10px] border border-outline-variant/30 text-on-surface-variant font-bold uppercase",
            complete ? "bg-surface-dim" : "bg-surface-container",
          )}
        >
          {complete ? "Respondido" : "Pendente"}
        </span>
      </div>

      <div className="divide-y divide-outline-variant/30">
        {issues.map((issue) => {
          const answer = answers[issue.id];
          const choice = issue.choices.find(
            (item) => item.value === answer?.choice,
          );
          const correction = choice?.correction;
          const invalidCorrection =
            Boolean(answer?.correction) &&
            !isReviewIssueComplete(issue, answer);

          return (
            <div key={issue.id} className="py-3 last:pb-0">
              <p className="text-sm font-semibold">{issue.question}</p>
              {issue.evidence && (
                <p className="mt-2 flex min-w-0 items-center rounded-lg border bg-card p-2 text-xs leading-5 text-on-surface-variant">
                  <HugeiconsIcon
                    icon={QuoteIcon}
                    strokeWidth={2}
                    className="inline-block text-primary size-8 zoom-40 shrink-0 mr-4"
                  />
                  <span className="shrink-0 mr-1">
                    Extraído do Currículo Base:
                  </span>
                  <span
                    className="flex min-w-0 flex-1 font-medium italic text-secondary "
                    title={issue.evidence}
                  >
                    <span className="shrink-0">“</span>
                    <span className="min-w-0 truncate underline">
                      {issue.evidence}
                    </span>
                    <span className="shrink-0">”</span>
                  </span>
                </p>
              )}
              <p className="text-xs mt-2 text-on-surface-variant leading-5">
                {issue.motivation}
              </p>
              <fieldset className="mt-3 flex flex-wrap gap-2 border-t border-outline-variant/50 pt-4">
                <legend className="sr-only">{issue.question}</legend>
                {issue.choices.map((item) => {
                  const selected = answer?.choice === item.value;

                  return (
                    <Button
                      key={item.value}
                      type="button"
                      aria-pressed={selected}
                      hoverScale={reducedMotion ? 1 : 1.015}
                      tapScale={reducedMotion ? 1 : 0.97}
                      animate={{ y: selected && !reducedMotion ? -2 : 0 }}
                      transition={{
                        type: "spring",
                        stiffness: 420,
                        damping: 30,
                      }}
                      onClick={() =>
                        onAnswerChange(issue.id, {
                          choice: item.value,
                          correction: selected ? answer.correction : "",
                        })
                      }
                      className={cn(
                        "inline-flex items-center gap-1.5 rounded-lg border px-3 py-2 text-left text-xs font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-900",
                        selected
                          ? "border-primary-900 bg-primary-900 text-primary-foreground shadow-sm hover:bg-primary-800"
                          : "border-outline-variant/50 bg-card text-primary-950 hover:border-primary-300 hover:bg-primary-50",
                        item.result === "omitted" && "self-end ml-auto",
                      )}
                    >
                      <HugeiconsIcon
                        icon={getChoiceIcon(item)}
                        strokeWidth={2}
                        aria-hidden="true"
                        className="inline-block size-8 zoom-40 shrink-0 mr-4"
                      />
                      {item.label}
                    </Button>
                  );
                })}
              </fieldset>

              <AutoHeight
                deps={[choice?.value]}
                transition={reducedMotion ? { duration: 0 } : undefined}
              >
                {correction && (
                  <ReviewCorrectionInput
                    key={`${issue.id}-${choice.value}`}
                    id={`review-correction-${issue.id}`}
                    field={correction}
                    value={answer.correction}
                    invalid={invalidCorrection}
                    onChange={(value) =>
                      onAnswerChange(issue.id, { ...answer, correction: value })
                    }
                  />
                )}
              </AutoHeight>
            </div>
          );
        })}
      </div>
    </section>
  );
}
