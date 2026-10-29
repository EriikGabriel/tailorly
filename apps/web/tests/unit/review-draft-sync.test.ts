import assert from "node:assert/strict";
import test from "node:test";
import { createReviewDraft } from "../../src/types/review-model";
import { applyAnswerToDraft } from "../../src/utils/review-draft-sync";

function draftWithExperience() {
  const draft = createReviewDraft();
  draft.identity = { name: "Erik Teste", email: "a@b.co" };
  draft.experience = [
    { id: "rec-1-0", fields: { company: "Acme", role: "Dev" } },
    { id: "rec-1-1", fields: { company: "Beta" } },
  ];
  return draft;
}

test("confirmar não altera o rascunho", () => {
  const draft = draftWithExperience();
  assert.equal(
    applyAnswerToDraft(
      draft,
      { category: "identity", recordId: "r", fieldKey: "name" },
      "confirmed",
      "",
    ),
    draft,
  );
});

test("corrigir um fato de identidade troca o valor no rascunho", () => {
  const next = applyAnswerToDraft(
    draftWithExperience(),
    { category: "identity", recordId: "r", fieldKey: "email" },
    "corrected",
    "novo@b.co",
  );
  assert.equal(next.identity.email, "novo@b.co");
  assert.equal(next.identity.name, "Erik Teste");
});

test("corrigir e omitir fatos de itens de lista atingem só o item certo", () => {
  const corrected = applyAnswerToDraft(
    draftWithExperience(),
    { category: "experience", recordId: "rec-1", fieldKey: "0:role" },
    "corrected",
    "Tech Lead",
  );
  assert.equal(corrected.experience[0].fields.role, "Tech Lead");
  assert.equal(corrected.experience[1].fields.company, "Beta");

  const omitted = applyAnswerToDraft(
    corrected,
    { category: "experience", recordId: "rec-1", fieldKey: "1:company" },
    "omitted",
    "",
  );
  assert.deepEqual(omitted.experience[1].fields, {});
  assert.equal(omitted.experience[0].fields.company, "Acme");
});

test("linha ou categoria desconhecida não altera nada", () => {
  const draft = draftWithExperience();
  assert.equal(
    applyAnswerToDraft(
      draft,
      { category: "experience", recordId: "outro", fieldKey: "0:role" },
      "corrected",
      "x",
    ),
    draft,
  );
  assert.equal(
    applyAnswerToDraft(
      draft,
      { category: "desconhecida", recordId: "r", fieldKey: "0:a" },
      "omitted",
      "",
    ),
    draft,
  );
});
