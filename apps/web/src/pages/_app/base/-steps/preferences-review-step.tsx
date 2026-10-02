import type { PresentationPreferences } from "@@types/review-model";
import { Checkbox } from "@animate/base/checkbox";
import {
  Highlight,
  HighlightItem,
} from "@animate/primitives/effects/highlight";
import { useReducedMotion } from "motion/react";
import { ReviewStepShell } from "../-components/review-step-shell";

const languages = [
  { value: "pt-BR", label: "Português", region: "BR" },
  { value: "en-US", label: "Inglês", region: "US" },
  { value: "es-ES", label: "Espanhol", region: "ES" },
] as const;
const links = [
  { key: "linkedin", label: "LinkedIn" },
  { key: "github", label: "GitHub" },
  { key: "whatsapp", label: "WhatsApp Direto" },
] as const;

export function PreferencesReviewStep({
  value,
  onChange,
}: {
  value: PresentationPreferences;
  onChange: (value: PresentationPreferences) => void;
}) {
  const reducedMotion = useReducedMotion();
  const checkboxMotion = reducedMotion
    ? { whileTap: { scale: 1 }, whileHover: { scale: 1 } }
    : {};
  return (
    <ReviewStepShell
      number={6}
      title="Preferências de Apresentação (Saída Flexível)"
      description="Personalização editorial de cabeçalho, escopo e idioma"
      rule="Preferências de estilização editorial não alteram os fatos nem bloqueiam a geração. Nesta prévia, ficam apenas no rascunho local."
      editorial
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <fieldset className="min-w-0 space-y-3 rounded-xl bg-surface-container-low p-4">
          <legend className="sr-only">Formato do nome</legend>
          <p className="text-xs font-semibold text-primary-950">
            Formato de Exibição do Nome no Cabeçalho
          </p>
          {(
            [
              {
                value: "short",
                label: "Nome profissional curto",
                hint: "Executivo curto",
              },
              {
                value: "full",
                label: "Nome completo",
                hint: "Completo oficial",
              },
            ] as const
          ).map((option) => (
            <label
              key={option.value}
              className="flex cursor-pointer items-start gap-2 text-body-sm"
            >
              <input
                type="radio"
                name="presentation-name"
                value={option.value}
                checked={value.nameFormat === option.value}
                onChange={() =>
                  onChange({ ...value, nameFormat: option.value })
                }
                className="mt-1 accent-primary"
              />
              <span>
                {option.label}
                <span className="block text-muted-foreground">
                  ({option.hint})
                </span>
              </span>
            </label>
          ))}
        </fieldset>
        <fieldset className="min-w-0 space-y-4 rounded-xl bg-surface-container-low p-4">
          <legend className="sr-only">Idioma de emissão</legend>
          <p className="text-xs font-semibold text-primary-950">
            Idioma Padrão para os Currículos Gerados
          </p>
          <Highlight
            mode="parent"
            controlledItems
            forceUpdateBounds
            value={value.language}
            click={false}
            exitDelay={0}
            transition={reducedMotion ? { duration: 0 } : undefined}
            className="pointer-events-none rounded-lg bg-primary shadow-xs"
          >
            <div className="grid grid-cols-3 gap-1">
              {languages.map((language) => (
                <div
                  key={language.value}
                  className="rounded-lg bg-surface-container"
                >
                  <HighlightItem value={language.value} asChild>
                    <button
                      type="button"
                      aria-pressed={value.language === language.value}
                      aria-selected={undefined}
                      onClick={() =>
                        onChange({ ...value, language: language.value })
                      }
                      className={`relative z-10 w-full rounded-lg px-1 py-2 text-xs leading-4 outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 ${value.language === language.value ? "font-semibold text-primary-foreground" : "text-primary-950"}`}
                    >
                      {language.label}
                      <span className="block">({language.region})</span>
                    </button>
                  </HighlightItem>
                </div>
              ))}
            </div>
          </Highlight>
        </fieldset>
        <fieldset className="min-w-0 space-y-4 rounded-xl bg-surface-container-low p-4">
          <legend className="sr-only">Links no cabeçalho</legend>
          <p className="text-xs font-semibold text-primary-950">
            Links Ativos no Cabeçalho do PDF Gerado
          </p>
          <div className="flex flex-wrap gap-3">
            {links.map((link) => (
              <label
                key={link.key}
                htmlFor={`preference-link-${link.key}`}
                className="flex cursor-pointer items-center gap-2 text-body-sm"
              >
                <Checkbox
                  id={`preference-link-${link.key}`}
                  {...checkboxMotion}
                  checked={value.links[link.key]}
                  onCheckedChange={(checked) =>
                    onChange({
                      ...value,
                      links: { ...value.links, [link.key]: checked },
                    })
                  }
                />
                {link.label}
              </label>
            ))}
          </div>
          <p className="text-xs text-muted-foreground">
            Somente links informados e revisados poderão ser incluídos.
          </p>
        </fieldset>
        <fieldset className="min-w-0 space-y-4 rounded-xl bg-surface-container-low p-4">
          <legend className="sr-only">Escopo e senioridade</legend>
          <p className="text-xs font-semibold text-primary-950">
            Filtro de Escopo e Senioridade
          </p>
          <label
            htmlFor="preference-hide-older"
            className="flex cursor-pointer items-start gap-2 text-body-sm"
          >
            <Checkbox
              id="preference-hide-older"
              {...checkboxMotion}
              checked={value.hideOlderRoles}
              onCheckedChange={(checked) =>
                onChange({ ...value, hideOlderRoles: checked })
              }
            />
            Ocultar estágios e cargos com mais de 10 anos nas versões executivas
          </label>
        </fieldset>
      </div>
    </ReviewStepShell>
  );
}
