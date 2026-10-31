import { Checkbox } from "@animate/base/checkbox";
import { useViewer } from "@queries/cv-generation";
import {
  type OwnerVaultInput,
  type OwnerVaultSettings,
  useClearOwnerVault,
  useOwnerVault,
  useSaveOwnerVault,
} from "@queries/owner-vault";
import { useId, useState } from "react";

/**
 * Preferências de segundo cérebro desta conta. O caminho do vault é relativo à pasta que o
 * servidor permite; o servidor só lê e exporta dentro dela, e só para esta conta.
 */
export function ObsidianVaultForm() {
  const viewer = useViewer();
  const isAccount = viewer.kind === "account" && viewer.ownerId !== null;
  const vault = useOwnerVault(isAccount ? viewer.ownerId : null, viewer.token);
  const save = useSaveOwnerVault(viewer.ownerId, viewer.token);
  const clear = useClearOwnerVault(viewer.ownerId, viewer.token);
  const [draft, setDraft] = useState<OwnerVaultInput | null>(null);

  if (!isAccount) {
    return (
      <p className="text-body-sm text-on-surface-variant">
        Entre na conta para configurar o vault do Obsidian e a busca web.
      </p>
    );
  }
  if (vault.isLoading || !vault.data) {
    return (
      <p className="text-body-sm text-on-surface-variant">
        Carregando preferências...
      </p>
    );
  }

  const settings = vault.data;
  const current: OwnerVaultInput = draft ?? toInput(settings);
  const changed = draft !== null;

  return (
    <form
      className="flex flex-col gap-4"
      onSubmit={(event) => {
        event.preventDefault();
        save.mutate(current, { onSuccess: () => setDraft(null) });
      }}
    >
      <Field
        label="Pasta do vault (dentro da pasta permitida pelo servidor)"
        htmlFor="vault-path"
      >
        <input
          id="vault-path"
          value={current.relativePath}
          onChange={(event) =>
            setDraft({ ...current, relativePath: event.target.value })
          }
          placeholder="Ex.: Pessoal/Second Brain"
          className="w-full rounded-lg bg-surface-container px-2.5 py-1.5 text-body-sm text-on-surface"
        />
      </Field>
      {!settings.rootAvailable && (
        <p className="text-xs text-on-surface-variant">
          Este servidor não tem uma pasta de vaults configurada, então o vault
          não pode ser lido nem exportado.
        </p>
      )}
      {settings.relativePath && !settings.usable && (
        <p className="text-xs text-error" role="alert">
          A pasta configurada não existe mais dentro da pasta permitida.
        </p>
      )}

      <ToggleRow
        label="Ler notas do vault para enriquecer projetos"
        checked={current.readEnabled}
        onChange={(readEnabled) => setDraft({ ...current, readEnabled })}
      />
      <ToggleRow
        label="Exportar os currículos gerados para o vault"
        checked={current.exportEnabled}
        onChange={(exportEnabled) => setDraft({ ...current, exportEnabled })}
      />
      <Field
        label="Subpasta de exportação (vazio usa o padrão do servidor)"
        htmlFor="vault-subfolder"
      >
        <input
          id="vault-subfolder"
          value={current.exportSubfolder}
          onChange={(event) =>
            setDraft({ ...current, exportSubfolder: event.target.value })
          }
          placeholder="Ex.: B03 Resources/CVs"
          className="w-full rounded-lg bg-surface-container px-2.5 py-1.5 text-body-sm text-on-surface"
        />
      </Field>
      <ToggleRow
        label={
          settings.webSearchAvailable
            ? "Buscar páginas sobre meus projetos na web"
            : "Buscar páginas sobre meus projetos na web (indisponível: o servidor não tem SearXNG)"
        }
        checked={current.webSearchEnabled}
        disabled={!settings.webSearchAvailable}
        onChange={(webSearchEnabled) =>
          setDraft({ ...current, webSearchEnabled })
        }
      />

      <div className="flex items-center justify-between gap-3">
        <button
          type="button"
          onClick={() =>
            clear.mutate(undefined, { onSuccess: () => setDraft(null) })
          }
          disabled={!settings.relativePath || clear.isPending}
          className="text-label-sm font-semibold text-destructive hover:text-destructive/80 disabled:opacity-60"
        >
          Remover vault
        </button>
        <button
          type="submit"
          disabled={!changed || save.isPending}
          className="rounded-lg bg-primary-950 px-3 py-1.5 text-xs font-medium text-white disabled:opacity-60"
        >
          {save.isPending ? "Salvando..." : "Salvar"}
        </button>
      </div>
      {(save.isError || clear.isError) && (
        <p className="text-xs text-error" role="alert">
          {(save.error ?? clear.error)?.message}
        </p>
      )}
    </form>
  );
}

function toInput(settings: OwnerVaultSettings): OwnerVaultInput {
  return {
    relativePath: settings.relativePath ?? "",
    readEnabled: settings.readEnabled,
    exportEnabled: settings.exportEnabled,
    exportSubfolder: settings.exportSubfolder ?? "",
    webSearchEnabled: settings.webSearchEnabled,
  };
}

function Field({
  label,
  htmlFor,
  children,
}: {
  label: string;
  htmlFor: string;
  children: React.ReactNode;
}) {
  return (
    <div className="flex flex-col gap-1">
      <label
        htmlFor={htmlFor}
        className="text-label-sm font-semibold text-on-surface"
      >
        {label}
      </label>
      {children}
    </div>
  );
}

function ToggleRow({
  label,
  checked,
  disabled = false,
  onChange,
}: {
  label: string;
  checked: boolean;
  disabled?: boolean;
  onChange: (checked: boolean) => void;
}) {
  const id = useId();

  return (
    <div className="flex items-center justify-between gap-3">
      <label
        htmlFor={id}
        className="text-label-sm font-semibold text-on-surface"
      >
        {label}
      </label>
      <Checkbox
        id={id}
        size="sm"
        checked={checked}
        disabled={disabled}
        onCheckedChange={(value) => onChange(value === true)}
      />
    </div>
  );
}
