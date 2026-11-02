import { create } from "zustand";

/** Diretrizes que a pessoa pode ativar; dão ênfase ao que já está aprovado no Currículo Base, sem acrescentar fatos. */
export const availableGuidelines = [
  "Destacar Liderança Técnica",
  "Arquitetura em Nuvem & Escala",
  "Impacto de Negócio Quantificado",
  "Projetos Open Source",
] as const;

/** Escolhas de uma geração: os padrões do perfil, ajustáveis por vaga. */
export interface GenerationChoices {
  guidelines: string[];
  optimizeForAts: boolean;
  templateName: string;
}

interface GenerationPreferencesState extends GenerationChoices {
  /** Padrões do perfil já aplicados, para não desfazer ajustes por vaga a cada visita ao gerador. */
  appliedDefaultsKey: string | null;
  toggleGuideline: (label: string) => void;
  setOptimizeForAts: (value: boolean) => void;
  setTemplateName: (value: string) => void;
  /** Escolhas de uma geração já feita foram colocadas no formulário; os padrões do perfil não as sobrescrevem. */
  restoredChoices: boolean;
  /** Reabre as escolhas registradas numa geração (ao abrir um currículo gerado). */
  restoreChoices: (choices: GenerationChoices) => void;
  /** Parte dos padrões do perfil; só reaplica quando eles mudam, não quando o gerador reabre. */
  applyDefaults: (defaults: GenerationChoices) => void;
  reset: () => void;
}

const initialChoices = (): GenerationChoices => ({
  guidelines: availableGuidelines.slice(0, 3),
  optimizeForAts: true,
  templateName: "classic",
});

/**
 * Preferências de interface enviadas a cada geração; o servidor as registra com o currículo gerado.
 * Contas partem dos padrões do perfil; mudar diretrizes ou layout aqui vale só para a geração atual.
 */
export const useGenerationPreferencesStore =
  create<GenerationPreferencesState>()((set) => ({
    ...initialChoices(),
    appliedDefaultsKey: null,
    restoredChoices: false,
    restoreChoices: (choices) =>
      set((state) => ({
        ...choices,
        // Só protege quando os padrões ainda vão chegar; se já foram aplicados, nada os reaplicaria.
        restoredChoices: state.appliedDefaultsKey === null,
      })),
    toggleGuideline: (label) =>
      set((state) => ({
        guidelines: state.guidelines.includes(label)
          ? state.guidelines.filter((item) => item !== label)
          : [...state.guidelines, label],
      })),
    setOptimizeForAts: (optimizeForAts) => set({ optimizeForAts }),
    setTemplateName: (templateName) => set({ templateName }),
    applyDefaults: (defaults) =>
      set((state) => {
        const key = JSON.stringify([
          defaults.templateName,
          defaults.optimizeForAts,
          [...defaults.guidelines].sort(),
        ]);
        if (state.appliedDefaultsKey === key) return state;
        // Escolhas restauradas de uma geração valem; só se registra a chave dos padrões.
        return state.restoredChoices
          ? { appliedDefaultsKey: key, restoredChoices: false }
          : { ...defaults, appliedDefaultsKey: key };
      }),
    reset: () =>
      set({
        ...initialChoices(),
        appliedDefaultsKey: null,
        restoredChoices: false,
      }),
  }));
