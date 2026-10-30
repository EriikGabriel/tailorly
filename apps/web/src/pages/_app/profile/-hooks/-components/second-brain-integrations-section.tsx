import { Checkbox } from "@animate/base/checkbox";
import { Gem, Notebook, Share2 } from "@react-zero-ui/icon-sprite";
import { useId, useState } from "react";
import { previewSecondBrainConnections } from "@/data/profile-preview";
import { SectionCard } from "./section-card";

export function SecondBrainIntegrationsSection() {
  const [notion, setNotion] = useState(previewSecondBrainConnections.notion);
  const [obsidian, setObsidian] = useState(
    previewSecondBrainConnections.obsidian,
  );

  return (
    <SectionCard
      icon={Share2}
      title="3. Gerenciamento do Second Brain (Opcional)"
      description="Conecte ferramentas de anotação pessoal para enriquecer contextualizações de projetos sem ferir o Ground Truth."
    >
      <div className="grid gap-4 lg:grid-cols-2">
        <div className="flex flex-col gap-4 rounded-xl bg-surface-container-low p-4">
          <div className="flex flex-col gap-1">
            <div className="flex items-center justify-between gap-2">
              <span className="flex items-center gap-2 text-title-md font-semibold text-primary-950">
                <Notebook aria-hidden="true" className="size-4" />
                Notion API Sync
              </span>
              <span className="shrink-0 rounded bg-surface-container px-2 py-0.5 text-label-sm font-medium text-on-secondary-container">
                Conectado
              </span>
            </div>
            <p className="text-body-sm text-on-surface-variant">
              Workspace ativo: <strong>{notion.workspace}</strong>.{" "}
              {notion.description}
            </p>
          </div>

          <div className="flex flex-col gap-2 pt-1">
            <ToggleRow
              label="Arquivar versões geradas no Notion"
              checked={notion.archiveGenerated}
              onCheckedChange={(checked) =>
                setNotion((prev) => ({ ...prev, archiveGenerated: checked }))
              }
            />
            <ToggleRow
              label="Consultar notas de projetos para bullets de impacto"
              checked={notion.useForBullets}
              onCheckedChange={(checked) =>
                setNotion((prev) => ({ ...prev, useForBullets: checked }))
              }
            />
            <div className="flex items-center justify-between pt-1">
              <button
                type="button"
                className="text-label-sm font-semibold text-on-surface-variant hover:text-primary-950"
              >
                Testar Conexão
              </button>
              <button
                type="button"
                className="text-label-sm font-semibold text-destructive hover:text-destructive/80"
              >
                Desconectar
              </button>
            </div>
          </div>
        </div>

        <div className="flex flex-col gap-4 rounded-xl bg-surface-container-low p-4">
          <div className="flex flex-col gap-1">
            <div className="flex items-center justify-between gap-2">
              <span className="flex items-center gap-2 text-title-md font-semibold text-primary-950">
                <Gem aria-hidden="true" className="size-4" />
                Obsidian Local Vault
              </span>
              <span className="shrink-0 rounded bg-surface-container px-2 py-0.5 text-label-sm font-medium text-on-secondary-container">
                Conectado
              </span>
            </div>
            <p className="text-body-sm text-on-surface-variant">
              Vault local:{" "}
              <span className="font-mono text-primary-950">
                {obsidian.vaultPath}
              </span>
              . {obsidian.description}
            </p>
          </div>

          <div className="flex flex-col gap-2 pt-1">
            <ToggleRow
              label="Exportar em formato .MD simultaneamente"
              checked={obsidian.exportMarkdown}
              onCheckedChange={(checked) =>
                setObsidian((prev) => ({ ...prev, exportMarkdown: checked }))
              }
            />
            <ToggleRow
              label="Vincular links bidirecionais de tags de vagas"
              checked={obsidian.linkJobTags}
              onCheckedChange={(checked) =>
                setObsidian((prev) => ({ ...prev, linkJobTags: checked }))
              }
            />
            <div className="flex items-center justify-between pt-1">
              <button
                type="button"
                className="text-label-sm font-semibold text-on-surface-variant hover:text-primary-950"
              >
                Sincronizar Agora
              </button>
              <button
                type="button"
                className="text-label-sm font-semibold text-destructive hover:text-destructive/80"
              >
                Remover Caminho
              </button>
            </div>
          </div>
        </div>
      </div>
    </SectionCard>
  );
}

function ToggleRow({
  label,
  checked,
  onCheckedChange,
}: {
  label: string;
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
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
        onCheckedChange={(value) => onCheckedChange(value === true)}
      />
    </div>
  );
}
