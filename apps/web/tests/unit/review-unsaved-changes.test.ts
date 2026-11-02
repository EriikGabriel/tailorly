import { createReviewDraft } from "@@types/review-model";
import assert from "node:assert/strict";
import { beforeEach, test } from "node:test";
import {
  hasUnsavedReviewChanges,
  useReviewDraftStore as store,
} from "@stores/review-draft-store";

function unsaved() {
  return hasUnsavedReviewChanges(store.getState());
}

beforeEach(() => store.getState().resetForSource("v1"));

test("o rascunho hidratado da API não conta como alteração", () => {
  const draft = createReviewDraft();
  draft.identity = { name: "Ana" };
  store.getState().hydrateFromApi("v1", draft);
  assert.equal(unsaved(), false);
});

test("editar marca alteração, salvar e descartar limpam", () => {
  const draft = createReviewDraft();
  draft.identity = { name: "Ana" };
  store.getState().hydrateFromApi("v1", draft);

  store.getState().setIdentity("v1", { name: "Ana Souza" });
  assert.equal(unsaved(), true);

  store.getState().discardChanges();
  assert.equal(store.getState().draft.identity.name, "Ana");
  assert.equal(unsaved(), false);

  store.getState().setIdentity("v1", { name: "Ana Souza" });
  store.getState().markSaved(store.getState().draft);
  assert.equal(unsaved(), false);
});

test("o que foi digitado durante o salvamento continua pendente", () => {
  store.getState().setIdentity("v1", { name: "Ana" });
  const sent = store.getState().draft;
  store.getState().setIdentity("v1", { name: "Ana Souza" });
  store.getState().markSaved(sent);
  assert.equal(unsaved(), true);
});
