import { create } from "zustand";

/** Diretrizes que a pessoa pode ativar; orientam só a ordem do que já está aprovado no Currículo Base. */
export const availableGuidelines = [
  "Destacar Liderança Técnica",
  "Arquitetura em Nuvem & Escala",
  "Impacto de Negócio Quantificado",
  "Projetos Open Source",
] as const;

interface GenerationPreferencesState {
  guidelines: string[];
  optimizeForAts: boolean;
  templateName: string;
  toggleGuideline: (label: string) => void;
  setOptimizeForAts: (value: boolean) => void;
  setTemplateName: (value: string) => void;
  reset: () => void;
}

/** Preferências de interface enviadas a cada geração; o servidor as registra com o currículo gerado. */
export const useGenerationPreferencesStore =
  create<GenerationPreferencesState>()((set) => ({
    guidelines: availableGuidelines.slice(0, 3),
    optimizeForAts: true,
    templateName: "classic",
    toggleGuideline: (label) =>
      set((state) => ({
        guidelines: state.guidelines.includes(label)
          ? state.guidelines.filter((item) => item !== label)
          : [...state.guidelines, label],
      })),
    setOptimizeForAts: (optimizeForAts) => set({ optimizeForAts }),
    setTemplateName: (templateName) => set({ templateName }),
    reset: () =>
      set({
        guidelines: availableGuidelines.slice(0, 3),
        optimizeForAts: true,
        templateName: "classic",
      }),
  }));
