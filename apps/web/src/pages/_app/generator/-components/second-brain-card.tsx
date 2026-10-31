import { Checkbox } from "@animate/base/checkbox";
import { Network } from "@react-zero-ui/icon-sprite";
import { useId, useState } from "react";
import { SectionHeading } from "./section-heading";

export function SecondBrainCard() {
  const consultId = useId();
  const saveId = useId();
  const [consultSecondBrain, setConsultSecondBrain] = useState(false);
  const [saveToSecondBrain, setSaveToSecondBrain] = useState(false);

  return (
    <div className="flex flex-col gap-3">
      <SectionHeading
        step={5}
        title="Second Brain (Opcional & Secundário)"
        tone="muted"
        trailing={
          <span className="rounded-full bg-surface-container px-2 py-0.5 font-semibold text-on-surface-variant">
            Opcional
          </span>
        }
      />

      <div className="flex flex-col gap-3 rounded-xl border border-outline-variant/60 bg-surface-container-low p-3.75">
        <label
          htmlFor={consultId}
          className="flex items-start justify-between gap-3"
        >
          <div className="flex items-start gap-2.5">
            <Checkbox
              id={consultId}
              checked={consultSecondBrain}
              onCheckedChange={(value) => setConsultSecondBrain(value === true)}
              disabled
              size="sm"
              className="mt-0.5"
            />
            <div>
              <p className="text-sm font-medium tracking-[0.14px] text-on-surface">
                Consultar notas complementares do Second Brain
              </p>
              <p className="pl-0 text-[13px] leading-5 text-on-surface-variant">
                Fonte secundária para enriquecer detalhes de projetos. O fluxo
                funciona perfeitamente sem ele.
              </p>
            </div>
          </div>
          <Network
            aria-hidden="true"
            className="mt-0.5 size-4 shrink-0 text-on-surface-variant"
          />
        </label>

        <label
          htmlFor={saveId}
          className="flex items-center gap-2 border-t border-outline-variant/40 pt-2.5"
        >
          <Checkbox
            id={saveId}
            checked={saveToSecondBrain}
            onCheckedChange={(value) => setSaveToSecondBrain(value === true)}
            disabled
            size="sm"
          />
          <span className="text-xs font-medium tracking-[0.3px] text-on-surface">
            Salvar cópia gerada automaticamente no Second Brain após download
          </span>
        </label>

        <p className="text-[11px] font-medium tracking-[0.3px] text-on-surface-variant/80">
          Integração com Notion/Obsidian ainda em desenvolvimento.
        </p>
      </div>
    </div>
  );
}
