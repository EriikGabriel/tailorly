import assert from "node:assert/strict";
import test from "node:test";
import type { JobAnalysis } from "../../src/lib/api/generated/model";
import {
  type MatchableFact,
  type MatchableRecord,
  matchJobToFacts,
  normalizeForMatch,
  relevanceForFact,
  verifiableMatches,
} from "../../src/utils/job-fact-match";

function fact(
  id: string,
  recordId: string,
  value: unknown,
  reviewState = "pendente",
  fieldKey = "0:description",
): MatchableFact {
  return {
    id,
    recordId,
    fieldKey,
    extractedValue: value,
    reviewedValue: null,
    reviewState,
  };
}

function record(
  id: string,
  _category: string,
  facts: MatchableFact[],
): MatchableRecord {
  return { id, facts };
}

const records = [
  record("r-exp", "experience", [
    fact("f-company", "r-exp", "Acme", "pendente", "0:company"),
    fact(
      "f-desc",
      "r-exp",
      "Desenvolvi APIs em Java e Spring Boot com Kubernetes.",
      "pendente",
      "0:description",
    ),
    fact("f-start", "r-exp", "2021-03", "pendente", "0:start"),
    fact("f-company-2", "r-exp", "Beta", "pendente", "1:company"),
    fact(
      "f-desc-2",
      "r-exp",
      "Atendimento ao cliente.",
      "pendente",
      "1:description",
    ),
  ]),
  record("r-tech", "technologies", [
    fact("f-react", "r-tech", "ReactJS", "confirmado", "0:name"),
    fact("f-js", "r-tech", "JavaScript", "confirmado", "1:name"),
    fact("f-cobol", "r-tech", "COBOL", "omitido", "2:name"),
  ]),
];

const analysis: JobAnalysis = {
  technologies: ["Java", "Node.js", "COBOL", "Terraform"],
  requirements: [
    {
      text: "Java e Spring Boot",
      importance: "required",
      category: "technology",
      technologies: ["Java", "Spring Boot"],
      evidence: "x",
    },
    {
      text: "Kubernetes",
      importance: "preferred",
      category: "technology",
      technologies: ["Kubernetes"],
      evidence: "x",
    },
    {
      text: "Terraform",
      importance: "required",
      category: "technology",
      technologies: ["Terraform"],
      evidence: "x",
    },
    {
      text: "Graduação",
      importance: "required",
      category: "education",
      technologies: [],
      evidence: "x",
    },
  ],
};

test("normaliza acentos, caixa e pontuação sem perder nomes de tecnologias", () => {
  assert.equal(
    normalizeForMatch("Experiência com Node.js, C++ e C#."),
    "experiencia com node.js c++ e c#",
  );
  assert.equal(normalizeForMatch("CI/CD"), "ci cd");
});

test("classifica requisitos como confirmados, a confirmar ou ausentes", () => {
  const match = matchJobToFacts(analysis, records);
  const byText = Object.fromEntries(
    match.matches.map((item) => [item.requirement.text, item]),
  );

  assert.equal(byText["Java e Spring Boot"].status, "unconfirmed");
  assert.deepEqual(byText["Java e Spring Boot"].factIds, ["f-desc"]);
  assert.equal(byText.Kubernetes.status, "unconfirmed");
  assert.equal(byText.Terraform.status, "missing");
});

test("'Java' não casa com 'JavaScript'", () => {
  const match = matchJobToFacts(
    {
      requirements: [
        {
          text: "Java",
          importance: "required",
          technologies: ["Java"],
          evidence: "x",
        },
      ],
    },
    [record("r", "technologies", [fact("f", "r", "JavaScript", "confirmado")])],
  );
  assert.equal(match.matches[0].status, "missing");
});

test("fatos omitidos não são evidência", () => {
  const match = matchJobToFacts(
    {
      requirements: [
        {
          text: "COBOL",
          importance: "required",
          technologies: ["COBOL"],
          evidence: "x",
        },
      ],
    },
    records,
  );
  assert.equal(match.matches[0].status, "missing");
});

test("requisito com fato confirmado é 'supported'", () => {
  const match = matchJobToFacts(
    {
      requirements: [
        {
          text: "React",
          importance: "required",
          technologies: ["ReactJS"],
          evidence: "x",
        },
      ],
    },
    records,
  );
  assert.equal(match.matches[0].status, "supported");
});

test("casa variações de escrita como Node.js e NodeJS", () => {
  const match = matchJobToFacts(
    {
      requirements: [
        {
          text: "Node",
          importance: "required",
          technologies: ["Node.js"],
          evidence: "x",
        },
      ],
    },
    [record("r", "technologies", [fact("f", "r", "NodeJS", "confirmado")])],
  );
  assert.equal(match.matches[0].status, "supported");
});

test("tecnologias fora dos requisitos entram como desejáveis, sem duplicar as já cobertas", () => {
  const match = matchJobToFacts(analysis, records);
  const extra = match.matches.filter(
    (item) => item.requirement.text === "Node.js",
  );

  assert.equal(extra.length, 1);
  assert.equal(extra[0].importance, "preferred");
  assert.equal(
    match.matches.filter((item) => item.requirement.text === "Java").length,
    0,
  );
});

test("a relevância vale para o fato e para o resto do item, com o requisito mais forte", () => {
  const match = matchJobToFacts(analysis, records);

  assert.equal(
    relevanceForFact(match, records[0].facts[1])?.importance,
    "required",
  );
  assert.match(
    relevanceForFact(match, records[0].facts[1])?.label ?? "",
    /Java, Spring Boot/,
  );
  // Empresa e data não citam nada, mas pertencem ao registro que cita.
  assert.equal(
    relevanceForFact(match, records[0].facts[0])?.importance,
    "required",
  );
  assert.match(
    relevanceForFact(match, records[0].facts[2])?.label ?? "",
    /item/,
  );
  // Outro item da mesma categoria não herda a relevância.
  assert.equal(relevanceForFact(match, records[0].facts[3]), undefined);
  assert.equal(relevanceForFact(match, records[0].facts[4]), undefined);
  // Registro sem relação com a vaga.
  assert.equal(
    relevanceForFact(match, {
      id: "x",
      recordId: "r-none",
      fieldKey: "0:name",
    }),
    undefined,
  );
});

test("tecnologias vizinhas no mesmo registro não ganham relevância umas das outras", () => {
  const match = matchJobToFacts(
    {
      requirements: [
        {
          text: "React",
          importance: "required",
          technologies: ["ReactJS"],
          evidence: "x",
        },
      ],
    },
    records,
  );

  assert.equal(
    relevanceForFact(match, records[1].facts[0])?.importance,
    "required",
  );
  assert.equal(relevanceForFact(match, records[1].facts[1]), undefined);
});

test("fatos de identidade, sem índice, só são relevantes se citarem o requisito", () => {
  const identity = record("r-id", "identity", [
    fact("f-name", "r-id", "Erik Java", "pendente", "name"),
  ]);
  const match = matchJobToFacts(
    {
      requirements: [
        {
          text: "Java",
          importance: "required",
          technologies: ["Java"],
          evidence: "x",
        },
      ],
    },
    [
      identity,
      record("r-x", "experience", [
        fact("f-x", "r-x", "Java", "pendente", "0:description"),
      ]),
    ],
  );

  assert.equal(
    relevanceForFact(match, identity.facts[0])?.importance,
    "required",
  );
  assert.equal(match.byItem.has("r-id#undefined"), false);
});

test("requisitos sem nome identificável não são verificáveis nem geram relevância", () => {
  const match = matchJobToFacts(analysis, records);

  assert.ok(
    !verifiableMatches(match).some(
      (item) => item.requirement.text === "Graduação",
    ),
  );
  assert.deepEqual(verifiableMatches(match)[0].importance, "required");
});

test("sem análise não há relevância", () => {
  const match = matchJobToFacts(undefined, records);

  assert.equal(match.matches.length, 0);
  assert.equal(match.byFact.size, 0);
});
