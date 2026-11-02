import { useSecondBrain } from "@queries/second-brain";
import { Gem, Globe, Share2 } from "@react-zero-ui/icon-sprite";
import { ObsidianVaultForm } from "./obsidian-vault-form";
import { SectionCard } from "./section-card";

/**
 * Preferências de Second Brain e busca web. Mostra só o que o servidor oferece: o vault Obsidian
 * existe apenas em instalação local, e a busca web depende de um provedor configurado. Sem nenhum
 * dos dois (por exemplo, produção sem chave de busca), a seção inteira some.
 */
export function SecondBrainIntegrationsSection() {
  const { available, vaultAvailable } = useSecondBrain();
  if (!available) return null;

  return (
    <div id="section-connections" className="scroll-mt-16">
      <SectionCard
        icon={vaultAvailable ? Share2 : Globe}
        title={
          vaultAvailable
            ? "3. Gerenciamento do Second Brain (Opcional)"
            : "3. Busca web (Opcional)"
        }
        description={
          vaultAvailable
            ? "Conecte suas notas para enriquecer a descrição dos projetos no currículo, sem alterar o Ground Truth."
            : "Procure páginas públicas dos seus projetos para sugerir evidências complementares, sem alterar o Ground Truth."
        }
      >
        <div className="flex flex-col gap-4 rounded-xl bg-surface-container-low p-4">
          {vaultAvailable && (
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
          )}
          <ObsidianVaultForm />
        </div>
      </SectionCard>
    </div>
  );
}
