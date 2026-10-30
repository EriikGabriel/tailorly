import { useAnonymousSessionStore } from "./anonymous-session-store";
import { useAuthStore } from "./auth-store";
import { useBaseCvStore } from "./base-cv-store";
import { useCvGenerationStore } from "./cv-generation-store";
import { useGenerationPreferencesStore } from "./generation-preferences-store";
import { useJobDraftStore } from "./job-draft-store";
import { useJobPostingStore } from "./job-posting-store";
import { useWorkspaceStore } from "./workspace-store";

/** Clear UI data when leaving an account/session. Server deletion is separate. */
export function resetClientState() {
  useAuthStore.getState().clearSession();
  // A sessão anônima só deixa de valer no cliente; o servidor apaga os dados no TTL.
  useAnonymousSessionStore.getState().clear();
  useBaseCvStore.getState().reset();
  useJobDraftStore.getState().reset();
  useCvGenerationStore.getState().clear();
  useJobPostingStore.getState().clear();
  useGenerationPreferencesStore.getState().reset();
  useWorkspaceStore.getState().reset();
  // Remove keys written by the former component-owned draft implementation.
  try {
    globalThis.sessionStorage?.removeItem("tailorly:job-draft");
    globalThis.sessionStorage?.removeItem("tailorly:review-preview");
  } catch {
    // Memory was cleared even if the browser disallows access to storage.
  }
}
