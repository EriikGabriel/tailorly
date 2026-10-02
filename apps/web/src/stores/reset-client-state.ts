import { useBaseCvStore } from "./base-cv-store";
import { useJobDraftStore } from "./job-draft-store";
import { usePresentationStore } from "./presentation-store";
import { useWorkspaceStore } from "./workspace-store";

/** Clear UI data when leaving an account/session. Server deletion is separate. */
export function resetClientState() {
  useBaseCvStore.getState().reset();
  useJobDraftStore.getState().reset();
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
