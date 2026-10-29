import { useAnonymousSessionStore } from "./anonymous-session-store";
import { useAuthStore } from "./auth-store";
import { useBaseCvStore } from "./base-cv-store";
import { useJobDraftStore } from "./job-draft-store";
import { useJobPostingStore } from "./job-posting-store";
import { usePresentationStore } from "./presentation-store";
import { useWorkspaceStore } from "./workspace-store";

/** Clear UI data when leaving an account/session. Server deletion is separate. */
export function resetClientState() {
  useAuthStore.getState().clearSession();
  // A sessão anônima só deixa de valer no cliente; o servidor apaga os dados no TTL.
  useAnonymousSessionStore.getState().clear();
  useBaseCvStore.getState().reset();
  useJobDraftStore.getState().reset();
  useJobPostingStore.getState().clear();
  usePresentationStore.getState().reset();
  useWorkspaceStore.getState().reset();
  // Remove keys written by the former component-owned draft implementation.
  try {
    globalThis.sessionStorage?.removeItem("tailorly:job-draft");
    globalThis.sessionStorage?.removeItem("tailorly:review-preview");
  } catch {
    // Memory was cleared even if the browser disallows access to storage.
  }
}
