import assert from "node:assert/strict";
import test from "node:test";
import {
  buildVerificationIndex,
  clearSteps,
  factLocation,
  type VerifiableRecord,
} from "../../src/utils/review-verification";

const fact = (
  recordId: string,
  fieldKey: string,
  state: string,
  value = "v",
  reviewed: unknown = null,
) => ({
  recordId,
  fieldKey,
  extractedValue: value,
  reviewedValue: reviewed,
  reviewState: state,
});

const records: VerifiableRecord[] = [
  {
    id: "id-rec",
    category: "identity",
    facts: [
      fact("id-rec", "name", "confirmado", "Erik"),
      fact("id-rec", "email", "pendente", "a@b.co"),
    ],
  },
  {
    id: "exp",
    category: "experience",
    facts: [
      fact("exp", "0:company", "confirmado", "Acme"),
      fact("exp", "0:role", "corrigido", "Dev", "Tech Lead"),
      fact("exp", "1:company", "confirmado", "Beta"),
      fact("exp", "1:role", "pendente", "QA"),
      fact("exp", "2:company", "omitido", "Gama"),
    ],
  },
];

test("localiza fatos no rascunho como identidade ou linha de coleção", () => {
  assert.deepEqual(
    factLocation({ category: "identity", recordId: "r", fieldKey: "name" }),
    { scope: "identity", key: "name" },
  );
  assert.deepEqual(
    factLocation({
      category: "experience",
      recordId: "exp",
      fieldKey: "1:role",
    }),
    { scope: "exp-1", key: "role" },
  );
});

test("confirmado e corrigido são verificados; pendente e omitido, não", () => {
  const { fields } = buildVerificationIndex(records);

  assert.equal(fields.get("identity|name"), "Erik");
  assert.equal(fields.get("exp-0|role"), "Tech Lead", "usa o valor corrigido");
  assert.equal(fields.has("identity|email"), false);
  assert.equal(fields.has("exp-1|role"), false);
  assert.equal(fields.has("exp-2|company"), false);
});

test("uma linha só é verificada quando todos os seus fatos estão", () => {
  const { rows } = buildVerificationIndex(records);

  assert.equal(rows.has("exp-0"), true);
  assert.equal(rows.has("exp-1"), false);
  assert.equal(
    rows.has("exp-2"),
    false,
    "linha só com fatos omitidos não conta",
  );
  assert.equal(rows.has("identity"), false);
});

test("etapa sem pendência: tem fatos e nenhuma pendência aberta", () => {
  const categories = {
    identity: ["identity"],
    experience: ["experience", "volunteering"],
    projects: ["projects"],
  } as const;

  const clear = clearSteps(
    records,
    { identity: 0, experience: 1, projects: 0 },
    categories,
  );
  assert.equal(clear.identity, true);
  assert.equal(clear.experience, false, "ainda há pendência");
  assert.equal(clear.projects, false, "sem fatos, nada a declarar");

  const allDone = clearSteps(
    records,
    { identity: 0, experience: 0, projects: 0 },
    categories,
  );
  assert.equal(allDone.experience, true);
});
