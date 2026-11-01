import { Switch } from "@animate/base/switch";
import { GuidelineChips } from "@components/generation/guideline-chips";
import { LayoutPicker } from "@components/generation/layout-picker";
import { useViewer } from "@queries/cv-generation";
import {
  DEFAULT_TEMPLATE,
  useGenerationDefaults,
} from "@queries/generation-defaults";
import { Target } from "@react-zero-ui/icon-sprite";
import { useGenerationPreferencesStore } from "@stores/generation-preferences-store";
import { Link } from "@tanstack/react-router";
import { useEffect, useId } from "react";
import { SectionHeading } from "./section-heading";

/**
 * Escolhas desta vaga: diretrizes de destaque e layout. Contas partem dos padrões do perfil e os
 * ajustes aqui valem só para a geração atual (nada é gravado na conta). A otimização para ATS é um
 * padrão do perfil; só visitantes, que não têm perfil, escolhem aqui.
 */
export function AiGuidelinesCard() {
  const atsSwitchId = useId();
  const active = useGenerationPreferencesStore((state) => state.guidelines);
  const optimizeForAts = useGenerationPreferencesStore(
    (state) => state.optimizeForAts,
  );
  const toggle = useGenerationPreferencesStore(
    (state) => state.toggleGuideline,
  );
  const setOptimizeForAts = useGenerationPreferencesStore(
    (state) => state.setOptimizeForAts,
  );
  const templateName = useGenerationPreferencesStore(
    (state) => state.templateName,
  );
  const setTemplateName = useGenerationPreferencesStore(
    (state) => state.setTemplateName,
  );
  const applyDefaults = useGenerationPreferencesStore(
    (state) => state.applyDefaults,
  );
  const viewer = useViewer();
  const accountOwnerId = viewer.kind === "account" ? viewer.ownerId : null;
  const isAccount = accountOwnerId !== null;
  const defaults = useGenerationDefaults(accountOwnerId, viewer.token);

  // Os padrões do perfil viram a escolha desta geração; só são reaplicados quando mudam no perfil.
  const saved = defaults.data;
  useEffect(() => {
    if (saved)
      applyDefaults({
        templateName: saved.templateName ?? DEFAULT_TEMPLATE,
        optimizeForAts: saved.optimizeForAts,
        guidelines: saved.guidelines,
      });
  }, [saved, applyDefaults]);

  return (
    <div className="flex flex-col gap-3 border-b border-outline-variant/60 pb-4">
      <SectionHeading
        step={3}
        title="Diretrizes de Destaque da IA"
        trailing={`${active.length} ${active.length === 1 ? "ativa" : "ativas"}`}
      />

      <p className="text-[13px] leading-5 text-on-surface-variant">
        A IA usa essas diretrizes para dar ênfase ao que já está aprovado no seu
        Currículo Base, na reescrita das frases e na ordem das seções; ela não
        inventa informações nem acrescenta nada que as fontes não sustentem.
        {isAccount && (
          <> Partem dos seus padrões; ajustes aqui valem só para esta vaga.</>
        )}
      </p>

      <GuidelineChips active={active} onToggle={toggle} />

      {isAccount ? (
        <p className="flex items-start gap-2 rounded-lg bg-surface-container-low p-3 text-[13px] leading-5 text-on-surface-variant shadow-xs">
          <Target
            aria-hidden="true"
            className="mt-0.5 size-4.5 shrink-0 text-on-tertiary-container"
          />
          <span>
            Padrões do perfil: otimização para ATS{" "}
            {optimizeForAts ? "ligada" : "desligada"}.{" "}
            <Link
              to="/profile"
              hash="section-ai-preferences"
              className="font-medium text-primary underline"
            >
              Alterar no perfil
            </Link>
          </span>
        </p>
      ) : (
        <div className="flex items-center justify-between gap-3 rounded-lg bg-surface-container-low p-3 shadow-xs">
          <div className="flex items-start gap-2.5">
            <Target
              aria-hidden="true"
              className="mt-0.5 size-4.5 shrink-0 text-on-tertiary-container"
            />
            <div>
              <label
                htmlFor={atsSwitchId}
                className="text-sm font-semibold tracking-[0.14px] text-on-surface"
              >
                Otimizar rigorosamente para ATS
              </label>
              <p className="text-[13px] leading-5 text-on-surface-variant">
                A esteira mede a nota de ATS do PDF e ajusta ordem e densidade
                do layout até melhorar (uma coluna, texto real, sem tabelas).
              </p>
            </div>
          </div>
          <Switch
            id={atsSwitchId}
            checked={optimizeForAts}
            onCheckedChange={setOptimizeForAts}
            aria-label="Otimizar rigorosamente para ATS"
          />
        </div>
      )}

      <LayoutPicker value={templateName} onChange={setTemplateName} />
    </div>
  );
}
