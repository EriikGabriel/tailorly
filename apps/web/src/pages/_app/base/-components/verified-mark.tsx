import { BadgeCheck } from "@react-zero-ui/icon-sprite";

/** Selo de fato verificado (confirmado ou corrigido pela pessoa). */
export function VerifiedMark({
  label = "Verificado",
  showLabel = false,
  className = "",
}: {
  label?: string;
  showLabel?: boolean;
  className?: string;
}) {
  return (
    <span
      role="img"
      aria-label={label}
      title={label}
      className={`inline-flex items-center gap-1 text-tertiary-700 ${className}`}
    >
      <BadgeCheck aria-hidden="true" className="size-4 shrink-0" />
      {showLabel && <span className="text-[11px] font-semibold">{label}</span>}
    </span>
  );
}
