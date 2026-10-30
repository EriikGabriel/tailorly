import { EVIDENCE_TONE } from "../-constants/evidence";
import type { HighlightGroup, HighlightMark } from "../-types/evidence";
import { categoryLabel } from "../-utils/evidence-category";

/** Caixa com a etiqueta da categoria que envolve todas as marcações de um registro. */
export function HighlightGroupBox({ group }: { group: HighlightGroup }) {
  return (
    <div
      aria-hidden="true"
      className={`pointer-events-none absolute select-none rounded-xl transition-all duration-300 ${
        group.focused ? EVIDENCE_TONE.box.focused : EVIDENCE_TONE.box.neutral
      }`}
      style={{
        top: group.rect.top,
        left: group.rect.left,
        width: group.rect.width,
        height: group.rect.height,
      }}
    >
      <span
        className={`pointer-events-none absolute -top-2.5 right-3 inline-flex select-none items-center gap-1 whitespace-nowrap rounded-md px-2 py-0.5 text-[10px] font-semibold tracking-[0.3px] shadow-xs ${
          group.focused
            ? EVIDENCE_TONE.badge.focused
            : EVIDENCE_TONE.badge.neutral
        }`}
      >
        {categoryLabel(group.category)}{" "}
        {group.focused ? "• Em foco" : "mapeado"}
      </span>
    </div>
  );
}

/** Destaque translúcido sobre o texto de um fato. */
export function HighlightMarkBox({ mark }: { mark: HighlightMark }) {
  return (
    <span
      aria-hidden="true"
      className={`pointer-events-none absolute select-none rounded-[3px] transition-all duration-300 ${
        mark.focused ? EVIDENCE_TONE.mark.focused : EVIDENCE_TONE.mark.neutral
      }`}
      style={{
        top: mark.rect.top - 1,
        left: mark.rect.left - 2,
        width: mark.rect.width + 4,
        height: mark.rect.height + 2,
      }}
    />
  );
}
