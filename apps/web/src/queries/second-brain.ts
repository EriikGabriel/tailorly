import { useViewer } from "@queries/cv-generation";
import { type OwnerVaultSettings, useOwnerVault } from "@queries/owner-vault";
import { useServerFeatures } from "@queries/server-features";

/**
 * Estado do Second Brain derivado das preferências reais da conta no servidor. Só contas têm vault;
 * visitantes anônimos sempre aparecem como desconectados.
 */
export interface SecondBrainStatus {
  /** Vault configurado e a pasta ainda existe dentro da raiz permitida. */
  connected: boolean;
  /** Notas do vault entram na busca de evidências complementares. */
  readsNotes: boolean;
  /** PDF e Markdown gerados são copiados para o vault. */
  exportsArtifacts: boolean;
  /** Busca web de projetos está disponível no servidor e ligada pela conta. */
  webSearch: boolean;
}

export function secondBrainStatusOf(
  settings: OwnerVaultSettings | undefined,
): SecondBrainStatus {
  const connected = settings?.usable === true;
  return {
    connected,
    readsNotes: connected && settings?.readEnabled === true,
    exportsArtifacts: connected && settings?.exportEnabled === true,
    webSearch:
      settings?.webSearchAvailable === true && settings.webSearchEnabled,
  };
}

export function useSecondBrain() {
  const viewer = useViewer();
  const isAccount = viewer.kind === "account" && viewer.ownerId !== null;
  const vault = useOwnerVault(isAccount ? viewer.ownerId : null, viewer.token);
  const features = useServerFeatures();

  return {
    isAccount,
    /** O servidor lê e exporta para vaults Obsidian (só em instalação local). */
    vaultAvailable: features.vault,
    /** O servidor tem busca web configurada. */
    webSearchAvailable: features.webSearch,
    /** Há algo do Second Brain para configurar neste servidor. */
    available: features.vault || features.webSearch,
    isLoading: vault.isLoading,
    settings: vault.data,
    status: secondBrainStatusOf(vault.data),
  };
}
