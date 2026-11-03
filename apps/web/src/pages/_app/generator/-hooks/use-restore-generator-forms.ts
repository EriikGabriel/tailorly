import { toLanguagePreference } from "@constants/generation-options";
import { useSelectedGeneration } from "@queries/cv-generation";
import {
  useJobPostingVersion,
  useSelectedJobPosting,
} from "@queries/job-posting";
import { useCvGenerationStore } from "@stores/cv-generation-store";
import { useGenerationPreferencesStore } from "@stores/generation-preferences-store";
import { useJobDraftStore } from "@stores/job-draft-store";
import { useJobPostingStore } from "@stores/job-posting-store";
import { useEffect, useRef } from "react";

/**
 * Recupera nos formulários do gerador o que o servidor já sabe, sem a pessoa digitar de novo:
 *
 * - ao abrir um currículo gerado: abre a vaga e a versão usadas, preenche o link (ou o texto) da vaga e
 *   volta as escolhas de layout, diretrizes e ATS registradas na geração;
 * - ao recarregar com uma vaga selecionada: preenche o link/texto se o formulário estiver vazio, sem
 *   sobrescrever o que a pessoa já digitou.
 */
export function useRestoreGeneratorForms() {
  const generation = useSelectedGeneration().data;
  const generationKind = useCvGenerationStore(
    (state) => state.selected?.ownerKind,
  );
  const selectedJob = useJobPostingStore((state) => state.selected);
  const detail = useSelectedJobPosting();
  const { version } = useJobPostingVersion(detail.data);

  const restoredGeneration = useRef<string | null>(null);
  const forceDraft = useRef(false);
  const filledVersion = useRef<string | null>(null);

  useEffect(() => {
    if (!generation?.id || restoredGeneration.current === generation.id) return;
    restoredGeneration.current = generation.id;

    const postingId = generation.job?.jobPostingId;
    if (postingId && generationKind) {
      useJobPostingStore
        .getState()
        .select(
          postingId,
          generationKind,
          generation.job?.versionNumber ?? null,
        );
      forceDraft.current = true;
    }
    const preferences = generation.preferences;
    if (preferences) {
      const current = useGenerationPreferencesStore.getState();
      current.restoreChoices({
        guidelines: preferences.guidelines,
        optimizeForAts: preferences.optimizeForAts ?? current.optimizeForAts,
        templateName: preferences.templateName ?? current.templateName,
        language: toLanguagePreference(preferences.language),
      });
    }
  }, [generation, generationKind]);

  useEffect(() => {
    const details = version?.details;
    if (
      !details ||
      !selectedJob ||
      version.jobPostingId !== selectedJob.postingId
    )
      return;
    const force = forceDraft.current;
    const versionKey = version.id ?? null;
    if (!force && filledVersion.current === versionKey) return;
    filledVersion.current = versionKey;
    forceDraft.current = false;

    const draft = useJobDraftStore.getState();
    if (!force && (draft.jobUrl || draft.jobText)) return;

    if (details.platform === "text") {
      draft.setMode("text");
      draft.setJobText(details.descriptionText ?? "");
    } else {
      draft.setMode("url");
      draft.setJobUrl(
        details.sourceUrl ?? details.canonicalUrl ?? details.finalUrl ?? "",
      );
    }
  }, [version, selectedJob]);
}
