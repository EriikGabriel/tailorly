import { Gem, Notebook, Share2 } from "@react-zero-ui/icon-sprite";
import { ObsidianVaultForm } from "./obsidian-vault-form";
import { SectionCard } from "./section-card";

export function SecondBrainIntegrationsSection() {
  return (
    <SectionCard
      icon={Share2}
      title="3. Gerenciamento do Second Brain (Opcional)"
      description="Conecte suas notas para enriquecer a descrição dos projetos no currículo, sem alterar o Ground Truth."
    >
      <div className="grid gap-4 lg:grid-cols-2">
        <div className="flex flex-col gap-4 rounded-xl bg-surface-container-low p-4">
          <div className="flex flex-col gap-1">
            <span className="flex items-center gap-2 text-title-md font-semibold text-primary-950">
              <Gem aria-hidden="true" className="size-4" />
              Obsidian (vault local)
            </span>
            <p className="text-body-sm text-on-surface-variant">
              Cada conta usa um vault. O servidor lê só as notas dele, e só
              dentro da pasta permitida pelo administrador.
            </p>
          </div>
          <ObsidianVaultForm />
        </div>

        <div className="flex flex-col gap-4 rounded-xl bg-surface-container-low p-4">
          <div className="flex flex-col gap-1">
            <span className="flex items-center gap-2 text-title-md font-semibold text-primary-950">
              <Notebook aria-hidden="true" className="size-4" />
              Notion
            </span>
            <p className="text-body-sm text-on-surface-variant">
              Integração ainda não implementada. Nenhuma página do Notion é lida
              ou alterada.
            </p>
          </div>
        </div>
      </div>
    </SectionCard>
  );
}
