import type { LocalCliProvider } from "@@types/profile-settings";
import { SelectField } from "@components/fields/select-field";
import {
  LOCAL_CLI_TIMEOUT_RANGE,
  localCliEffortOptions,
  localCliMeta,
  localCliTaskLabels,
} from "@constants/ai-providers";
import { Brain, Clock, Gauge, Info } from "@react-zero-ui/icon-sprite";
import { useProfileSettingsStore } from "@stores/profile-settings-store";
import { isLocalCliDraftValid } from "@utils/ai-preferences";
import { FieldLabel } from "./section-card";

const inputClass =
  "w-full rounded-lg bg-card px-4 py-2.5 text-body-md text-primary-950 outline-none focus-visible:ring-3 focus-visible:ring-ring/50";

const effortLabel = (value: string) =>
  localCliEffortOptions.find((option) => option.value === value)?.label ??
  value;

/**
 * Ajustes de um provider local por CLI (`claude-cli` ou `codex-cli`): o CLI logado na máquina do
 * servidor, só disponível quando o servidor local o habilita. O executável continua sendo
 * configuração do servidor.
 */
export function AiLocalCliCard({ provider }: { provider: LocalCliProvider }) {
  const meta = localCliMeta[provider];
  const config = useProfileSettingsStore(
    (state) => state.aiDraft.localCli[provider],
  );
  const server = useProfileSettingsStore(
    (state) => state.aiDraft.localCliServer[provider].defaults,
  );
  const tasks = useProfileSettingsStore((state) => state.aiDraft.localCliTasks);
  const updateLocalCli = useProfileSettingsStore(
    (state) => state.updateLocalCli,
  );
  const updateEffort = useProfileSettingsStore(
    (state) => state.updateLocalCliEffort,
  );

  const rows = [...tasks, "default"];
  const customized =
    config.model !== server.model ||
    config.timeoutSeconds !== server.timeoutSeconds ||
    rows.some((task) => config.efforts[task] !== server.efforts[task]);
  const valid = isLocalCliDraftValid(provider, config);
  const modelId = `${provider}-model`;
  const timeoutId = `${provider}-timeout`;

  return (
    <div className="flex flex-col gap-4 rounded-xl border border-outline-variant bg-surface-container-low p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-start gap-2.5">
          <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-card">
            <Brain
              aria-hidden="true"
              className="size-4 text-on-surface-variant"
            />
          </span>
          <div>
            <h3 className="text-title-md font-semibold text-primary-950">
              {meta.title}
            </h3>
            <p className="text-body-sm text-on-surface-variant">
              {meta.description} Faça login antes de usar com
              <br />
              <span className="font-mono text-primary-950">
                {meta.loginCommand}
              </span>
              .
            </p>
          </div>
        </div>
        {customized && (
          <button
            type="button"
            onClick={() =>
              updateLocalCli(provider, {
                model: server.model,
                timeoutSeconds: server.timeoutSeconds,
                efforts: { ...server.efforts },
              })
            }
            className="shrink-0 rounded-lg px-2.5 py-1 text-label-sm font-semibold text-on-secondary-container hover:bg-card"
          >
            Restaurar padrões
          </button>
        )}
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <div className="flex flex-col gap-1.5">
          <FieldLabel>Modelo</FieldLabel>
          <input
            id={modelId}
            aria-label="Modelo"
            value={config.model}
            placeholder={meta.allowEmptyModel ? "Padrão do CLI" : undefined}
            onChange={(event) =>
              updateLocalCli(provider, { model: event.target.value })
            }
            aria-invalid={
              !valid && config.model.trim() !== "" ? true : undefined
            }
            className={inputClass}
          />
          {meta.modelSuggestions.length > 0 && (
            <div className="flex flex-wrap gap-1.5">
              <p className="text-body-sm text-on-surface-variant w-full">
                {meta.modelHint}
              </p>
              {meta.modelSuggestions.map((model) => (
                <button
                  key={model}
                  type="button"
                  onClick={() => updateLocalCli(provider, { model })}
                  className={`rounded-full px-3 py-1 text-label-sm font-semibold transition-colors ${
                    config.model === model
                      ? "bg-primary text-primary-foreground"
                      : "bg-card text-on-surface-variant hover:bg-surface-container"
                  }`}
                >
                  {model}
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="flex flex-col gap-1.5">
          <FieldLabel>
            <span className="inline-flex items-center gap-1.5">
              <Clock aria-hidden="true" className="size-3.5" />
              Tempo máximo por etapa (segundos)
            </span>
          </FieldLabel>
          <input
            id={timeoutId}
            aria-label="Tempo máximo por etapa (segundos)"
            type="number"
            inputMode="numeric"
            min={LOCAL_CLI_TIMEOUT_RANGE.min}
            max={LOCAL_CLI_TIMEOUT_RANGE.max}
            step={10}
            value={
              Number.isNaN(config.timeoutSeconds) ? "" : config.timeoutSeconds
            }
            onChange={(event) =>
              updateLocalCli(provider, {
                timeoutSeconds: Number.parseInt(event.target.value, 10),
              })
            }
            className={inputClass}
          />
          <p className="text-body-sm text-on-surface-variant">
            Entre {LOCAL_CLI_TIMEOUT_RANGE.min} e {LOCAL_CLI_TIMEOUT_RANGE.max}{" "}
            s. Padrão do servidor: {server.timeoutSeconds} s.
          </p>
        </div>
      </div>

      <div className="flex flex-col gap-2 rounded-lg bg-card p-3">
        <p className="flex items-center gap-1.5 text-label-md font-semibold text-primary-950">
          <Gauge aria-hidden="true" className="size-3.5" />
          Esforço de raciocínio por etapa
        </p>
        <p className="text-body-sm text-on-surface-variant">
          Mais esforço dá respostas melhores, mas demora mais e gasta mais dos
          limites da sua conta.
        </p>
        <ul className="flex flex-col divide-y divide-outline-variant">
          {rows.map((task) => {
            const label = localCliTaskLabels[task] ?? { name: task, hint: "" };
            const value = config.efforts[task] ?? server.efforts[task] ?? "";
            const serverValue = server.efforts[task];
            const selectId = `${provider}-effort-${task}`;
            return (
              <li
                key={task}
                className="grid items-center gap-2 py-2 sm:grid-cols-[1fr_14rem]"
              >
                <div>
                  <label
                    htmlFor={selectId}
                    className="text-label-md font-semibold text-primary-950"
                  >
                    {label.name}
                  </label>
                  <p className="text-body-sm text-on-surface-variant">
                    {label.hint}
                    {serverValue && value !== serverValue && (
                      <> Padrão do servidor: {effortLabel(serverValue)}.</>
                    )}
                  </p>
                </div>
                <SelectField
                  id={selectId}
                  value={value}
                  options={localCliEffortOptions}
                  emptyOption={false}
                  onChange={(next) => updateEffort(provider, task, next)}
                />
              </li>
            );
          })}
        </ul>
      </div>

      <p className="flex items-start gap-1.5 text-body-sm text-on-surface-variant">
        <Info aria-hidden="true" className="mt-0.5 size-3.5 shrink-0" />
        <span>
          O executável e a liberação deste modo ficam no servidor (
          <span className="font-mono">{meta.enableVariable}</span>). Visitantes
          sem conta usam a configuração do servidor.
        </span>
      </p>
    </div>
  );
}
