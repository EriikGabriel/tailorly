import assert from "node:assert/strict";
import test from "node:test";
import {
  firstStepWithPending,
  pendingByStep,
  stepForReviewCategory,
} from "../../src/utils/review-step-map";

test("cada categoria da pendência cai na etapa onde ela é editada", () => {
  assert.equal(stepForReviewCategory("identity"), "identity");
  assert.equal(stepForReviewCategory("employment"), "experience");
  assert.equal(stepForReviewCategory("volunteering"), "experience");
  assert.equal(stepForReviewCategory("education"), "education");
  assert.equal(stepForReviewCategory("languages"), "skills");
  assert.equal(stepForReviewCategory("skills"), "skills");
  assert.equal(stepForReviewCategory("projects"), "projects");
});

test("conta pendências por etapa separando as ligadas à vaga", () => {
  const pending = pendingByStep([
    { category: "identity" },
    { category: "employment", jobRelevance: { importance: "required" } },
    { category: "employment" },
    { category: "skills", jobRelevance: { importance: "preferred" } },
  ]);

  assert.deepEqual(pending.identity, { total: 1, relevant: 0 });
  assert.deepEqual(pending.experience, { total: 2, relevant: 1 });
  assert.deepEqual(pending.skills, { total: 1, relevant: 1 });
  assert.deepEqual(pending.projects, { total: 0, relevant: 0 });
});

test("abre primeiro a etapa com pendências da vaga, senão a primeira com pendências", () => {
  assert.equal(
    firstStepWithPending(
      pendingByStep([
        { category: "identity" },
        { category: "skills", jobRelevance: {} },
      ]),
    ),
    "skills",
  );
  assert.equal(
    firstStepWithPending(
      pendingByStep([{ category: "education" }, { category: "projects" }]),
    ),
    "projects",
  );
  assert.equal(firstStepWithPending(pendingByStep([])), null);
});
