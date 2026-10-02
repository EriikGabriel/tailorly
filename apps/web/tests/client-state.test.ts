import assert from "node:assert/strict";
import { beforeEach, test } from "node:test";
import { useBaseCvStore as base } from "../src/stores/base-cv-store";
import { useDocumentViewerStore as viewer } from "../src/stores/document-viewer-store";
import { useJobDraftStore as job } from "../src/stores/job-draft-store";
import { usePresentationStore as presentation } from "../src/stores/presentation-store";
import { useQuickReviewStore as quick } from "../src/stores/quick-review-store";
import { resetClientState } from "../src/stores/reset-client-state";
import { useReviewDraftStore as review } from "../src/stores/review-draft-store";
import { useWorkspaceStore as workspace } from "../src/stores/workspace-store";
import type { ReviewIssue } from "../src/types/review";

const pdf = (name = "cv.pdf") =>
  new File(["%PDF-test"], name, { type: "application/pdf" });
const issues: ReviewIssue[] = [
  {
    id: "current",
    category: "employment",
    type: "temporal",
    question: "Vínculo atual?",
    motivation: "Confirmar período",
    choices: [{ value: "yes", label: "Sim", result: "confirmed" }],
  },
];
beforeEach(resetClientState);

test("a shared selection retains drafts, step and zoom when read by another consumer", () => {
  const file = pdf();
  base.getState().selectFile(file);
  const sourceId = base.getState().selectionId;
  review.getState().setIdentity(sourceId, { name: "Pessoa de teste" });
  review.getState().setActiveStep("skills");
  viewer.getState().setZoom(125);
  base.getState().selectFile(file);
  assert.equal(base.getState().file, file);
  assert.equal(review.getState().draft.identity.name, "Pessoa de teste");
  assert.equal(review.getState().activeStep, "skills");
  assert.equal(viewer.getState().zoom, 125);
});

test("replacing the source resets dependent data and rejects stale source updates", () => {
  base.getState().selectFile(pdf());
  const oldId = base.getState().selectionId;
  review.getState().setIdentity(oldId, { name: "Original" });
  review
    .getState()
    .setCollection(oldId, "experience", [
      { id: "role", fields: { company: "Teste" } },
    ]);
  review.getState().setExpandedRecord(oldId, "experience", "role");
  review.getState().setActiveStep("experience");
  quick.getState().configure("old", issues, true);
  quick
    .getState()
    .setAnswer("old", "current", { choice: "yes", correction: "" });
  viewer.getState().setZoom(150);
  job.getState().setJobText("Vaga");
  presentation.getState().setPreferences({
    ...presentation.getState().preferences,
    language: "en-US",
  });
  base.getState().selectFile(pdf("next.pdf"));
  review.getState().setIdentity(oldId, { name: "Resposta atrasada" });
  assert.notEqual(base.getState().selectionId, oldId);
  assert.deepEqual(review.getState().draft.identity, {});
  assert.deepEqual(review.getState().draft.experience, []);
  assert.deepEqual(review.getState().expandedRecords, {});
  assert.equal(review.getState().activeStep, "identity");
  assert.deepEqual(quick.getState().answers, {});
  assert.equal(viewer.getState().zoom, "fit");
  assert.equal(job.getState().jobText, "Vaga");
  assert.equal(presentation.getState().preferences.language, "en-US");
});

test("invalid and empty selections preserve the existing file and draft", () => {
  const file = pdf();
  base.getState().selectFile(file);
  review
    .getState()
    .setIdentity(base.getState().selectionId, { name: "Preservado" });
  assert.equal(base.getState().selectFile(new File([], "empty.pdf")), false);
  assert.equal(base.getState().selectFile(pdf("not-a-pdf.docx"), "pdf"), false);
  assert.equal(base.getState().file, file);
  assert.equal(review.getState().draft.identity.name, "Preservado");
  assert.ok(base.getState().error);
});

test("removing a file clears its drafts, quick review and viewer state", () => {
  base.getState().selectFile(pdf());
  review
    .getState()
    .setIdentity(base.getState().selectionId, { name: "Apagar" });
  quick.getState().configure("source", issues, true);
  quick
    .getState()
    .setAnswer("source", "current", { choice: "yes", correction: "" });
  quick.getState().setOpen(true);
  viewer.getState().setZoom(200);
  base.getState().selectFile(null);
  assert.equal(base.getState().selectionId, null);
  assert.equal(base.getState().file, null);
  assert.deepEqual(review.getState().draft.identity, {});
  assert.deepEqual(quick.getState().answers, {});
  assert.equal(quick.getState().open, false);
  assert.equal(viewer.getState().zoom, "fit");
});

test("job changes invalidate quick review without erasing the base CV draft", () => {
  base.getState().selectFile(pdf());
  review
    .getState()
    .setIdentity(base.getState().selectionId, { name: "Preservado" });
  quick.getState().configure("job-1", issues, true);
  quick
    .getState()
    .setAnswer("job-1", "current", { choice: "yes", correction: "" });
  const previousRevision = job.getState().revision;
  job.getState().setJobUrl("https://example.com/job");
  assert.equal(job.getState().revision, previousRevision + 1);
  assert.deepEqual(quick.getState().answers, {});
  assert.equal(review.getState().draft.identity.name, "Preservado");
});

test("quick review keeps answers on remount but rejects old or unknown issues", () => {
  quick.getState().configure("a", issues, true);
  quick.getState().setAnswer("a", "current", { choice: "yes", correction: "" });
  quick.getState().savePreview("a");
  quick.getState().configure("a", issues, true);
  assert.equal(quick.getState().answers.current.choice, "yes");
  assert.ok(quick.getState().submittedPreview);
  quick.getState().configure("b", issues, true);
  quick.getState().setAnswer("a", "current", { choice: "yes", correction: "" });
  quick.getState().setAnswer("b", "missing", { choice: "yes", correction: "" });
  assert.deepEqual(quick.getState().answers, {});
  assert.equal(quick.getState().submittedPreview, null);
});

test("session reset clears all contexts and restores independent preference defaults", () => {
  base.getState().selectFile(pdf());
  job.getState().setJobText("Dados de uma sessão anterior");
  job.getState().saveDraft();
  presentation.getState().setPreferences({
    ...presentation.getState().preferences,
    language: "es-ES",
    links: { linkedin: false, github: false, whatsapp: true },
  });
  workspace.getState().setActiveView("preview");
  resetClientState();
  assert.equal(base.getState().file, null);
  assert.equal(job.getState().jobText, "");
  assert.equal(job.getState().savedAt, null);
  assert.equal(presentation.getState().preferences.language, "pt-BR");
  assert.equal(presentation.getState().preferences.links.linkedin, true);
  assert.equal(workspace.getState().activeView, "editor");
});

test("viewer enforces supported zoom and ignores invalid numeric values", () => {
  viewer.getState().setZoom(500);
  assert.equal(viewer.getState().zoom, 200);
  viewer.getState().setZoom(1);
  assert.equal(viewer.getState().zoom, 50);
  viewer.getState().setZoom(Number.NaN);
  assert.equal(viewer.getState().zoom, 50);
  viewer.getState().setZoom("fit");
  assert.equal(viewer.getState().zoom, "fit");
});
