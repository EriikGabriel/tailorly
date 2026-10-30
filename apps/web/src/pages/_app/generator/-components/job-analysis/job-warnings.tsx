import { AlertTriangle } from "@react-zero-ui/icon-sprite";
import { JOB_WARNING_LABELS } from "../../-constants/job-labels";

/** Avisos da leitura da página (vaga encerrada, pouco texto, seções não detectadas). */
export function JobWarnings({ warnings }: { warnings: readonly string[] }) {
  if (warnings.length === 0) return null;
  return (
    <ul className="flex flex-col gap-1">
      {warnings.map((warning) => (
        <li
          key={warning}
          className="flex items-start gap-1.5 text-[13px] leading-5 text-on-surface-variant"
        >
          <AlertTriangle
            className="mt-0.5 size-3.5 shrink-0"
            aria-hidden="true"
          />
          {JOB_WARNING_LABELS[warning] ?? warning}
        </li>
      ))}
    </ul>
  );
}
