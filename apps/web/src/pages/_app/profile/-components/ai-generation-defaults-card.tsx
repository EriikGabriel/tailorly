import { Switch } from "@animate/base/switch";
import { Select } from "@base-ui-components/react/select";
import { Check, ChevronDown } from "@react-zero-ui/icon-sprite";
import { useState } from "react";
import {
  languageOptions,
  typographyOptions,
} from "@/constants/generation-options";
import { FieldLabel } from "./section-card";

export function AiGenerationDefaultsCard() {
  const [strictAts, setStrictAts] = useState(true);
  const [quantifiedResults, setQuantifiedResults] = useState(true);
  const [typography, setTypography] = useState<string>(
    typographyOptions[0].value,
  );
  const [language, setLanguage] = useState<string>(languageOptions[0].value);

  return (
    <div className="flex flex-col gap-3 pt-2">
      <div className="flex items-center justify-between gap-2">
        <h3 className="text-title-md font-semibold text-primary-950">
          Diretrizes Padrão de Formatação ATS
        </h3>
        <span className="text-label-sm font-semibold text-on-surface-variant">
          Injeção de Prompt Universal
        </span>
      </div>

      <div className="flex flex-col gap-2">
        <ToggleRow
          title="Otimização Rigorosa para Robôs ATS (Workday, Greenhouse, Lever, Taleo)"
          description="Garante fonte padrão, hierarquia monocromática clara e ausência de tabelas complexas que quebram parsers."
          checked={strictAts}
          onCheckedChange={setStrictAts}
        />
        <ToggleRow
          title="Priorizar Resultados Quantificados (Framework XYZ do Google)"
          description='Estrutura bullets com: "Conquistou [X], medido por [Y], realizando [Z]".'
          checked={quantifiedResults}
          onCheckedChange={setQuantifiedResults}
        />
      </div>

      <div className="grid gap-4 pt-2 sm:grid-cols-2">
        <SettingsSelect
          label="Modelo Tipográfico Padrão"
          value={typography}
          onChange={setTypography}
          options={typographyOptions}
        />
        <SettingsSelect
          label="Idioma de Síntese Padrão"
          value={language}
          onChange={setLanguage}
          options={languageOptions}
        />
      </div>
    </div>
  );
}

function ToggleRow({
  title,
  description,
  checked,
  onCheckedChange,
}: {
  title: string;
  description: string;
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
}) {
  return (
    <div className="flex items-center justify-between gap-4 rounded-lg bg-surface-container-low p-2">
      <div className="flex flex-col gap-0.5">
        <p className="text-label-md font-semibold text-primary-950">{title}</p>
        <p className="text-body-sm text-on-surface-variant">{description}</p>
      </div>
      <Switch checked={checked} onCheckedChange={onCheckedChange} />
    </div>
  );
}

function SettingsSelect({
  label,
  value,
  onChange,
  options,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  options: readonly { value: string; label: string }[];
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <FieldLabel>{label}</FieldLabel>
      <Select.Root
        value={value}
        onValueChange={(next) => onChange(next ?? value)}
      >
        <Select.Trigger className="flex w-full items-center justify-between gap-2 rounded-lg bg-surface-container-low px-4 py-2.5 text-body-md text-primary-950 outline-none focus-visible:ring-3 focus-visible:ring-ring/50 data-popup-open:text-primary">
          <Select.Value className="truncate text-left">
            {(selected: string) =>
              options.find((option) => option.value === selected)?.label ??
              "Selecione uma opção"
            }
          </Select.Value>
          <Select.Icon className="shrink-0 text-on-surface-variant transition-transform duration-200 group-data-popup-open:rotate-180">
            <ChevronDown className="size-4" aria-hidden="true" />
          </Select.Icon>
        </Select.Trigger>
        <Select.Portal>
          <Select.Positioner
            alignItemWithTrigger={false}
            sideOffset={6}
            className="z-70"
          >
            <Select.Popup className="w-(--anchor-width) min-w-64 origin-(--transform-origin) rounded-lg border border-outline-variant/60 bg-card p-1 text-primary-950 shadow-xl outline-none transition-[opacity,transform] duration-150 data-starting-style:translate-y-1 data-starting-style:opacity-0 data-ending-style:opacity-0">
              <Select.List>
                {options.map((option) => (
                  <Select.Item
                    key={option.value}
                    value={option.value}
                    className="flex cursor-pointer items-center justify-between gap-3 rounded-md px-3 py-2 text-sm outline-none transition-colors data-highlighted:bg-surface-container data-selected:bg-primary data-selected:text-primary-foreground"
                  >
                    <Select.ItemText>{option.label}</Select.ItemText>
                    <Select.ItemIndicator>
                      <Check className="size-4" aria-hidden="true" />
                    </Select.ItemIndicator>
                  </Select.Item>
                ))}
              </Select.List>
            </Select.Popup>
          </Select.Positioner>
        </Select.Portal>
      </Select.Root>
    </div>
  );
}
