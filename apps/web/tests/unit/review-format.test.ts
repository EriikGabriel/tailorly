import assert from "node:assert/strict";
import { test } from "node:test";
import {
  describeLink,
  durationLabel,
  formatPartialDate,
  formatPeriod,
  formatPhone,
  initials,
  isCurrentStatus,
  parsePartialDate,
  proficiencyIndex,
  toPartialValue,
} from "../../src/lib/review-format";

test("datas parciais aceitam mês e ano ou só o ano", () => {
  assert.deepEqual(parsePartialDate("2022-01"), { year: 2022, month: 1 });
  assert.deepEqual(parsePartialDate("2015"), { year: 2015, month: null });
  assert.equal(parsePartialDate("2022-13"), null);
  assert.equal(parsePartialDate("jan/2022"), null);
  assert.equal(toPartialValue(2022, 3), "2022-03");
  assert.equal(toPartialValue(2015, null), "2015");
});

test("datas são exibidas por extenso curto em português", () => {
  assert.equal(formatPartialDate("2022-01"), "jan 2022");
  assert.equal(formatPartialDate("2021-12"), "dez 2021");
  assert.equal(formatPartialDate("2015"), "2015");
  assert.equal(formatPartialDate("jan/2022"), "jan/2022");
  assert.equal(formatPartialDate(""), "");
});

test("o período combina início e fim ou 'atual'", () => {
  assert.equal(formatPeriod("2022-01", "", true), "jan 2022 — atual");
  assert.equal(formatPeriod("2015", "2019", false), "2015 — 2019");
  assert.equal(
    formatPeriod("2019-03", "2021-12", false),
    "mar 2019 — dez 2021",
  );
  assert.equal(formatPeriod("2019-03", "", false), "mar 2019");
  assert.equal(formatPeriod("", "", false), "");
  assert.equal(
    formatPeriod("2020", "", true, "em andamento"),
    "2020 — em andamento",
  );
});

test("a duração é inclusiva e cai para anos quando falta o mês", () => {
  const now = new Date(Date.UTC(2026, 9, 27));
  assert.equal(durationLabel("2022-01", "", true, now), "4 anos e 10 meses");
  assert.equal(
    durationLabel("2019-03", "2021-12", false, now),
    "2 anos e 10 meses",
  );
  assert.equal(durationLabel("2022-01", "2022-01", false, now), "1 mês");
  assert.equal(durationLabel("2020-01", "2021-12", false, now), "2 anos");
  assert.equal(durationLabel("2015", "2019", false, now), "4 anos");
  assert.equal(durationLabel("2022-05", "2022-01", false, now), null);
  assert.equal(durationLabel("texto", "2022-01", false, now), null);
});

test("status atual é reconhecido em português e inglês", () => {
  for (const status of [
    "Atual",
    "atual",
    "Presente",
    "Current",
    "Em andamento",
  ]) {
    assert.equal(isCurrentStatus(status), true, status);
  }
  for (const status of ["Encerrado", "Concluído", "", undefined]) {
    assert.equal(isCurrentStatus(status), false, String(status));
  }
});

test("a máscara de telefone é progressiva e preserva números internacionais", () => {
  assert.equal(formatPhone("11"), "(11");
  assert.equal(formatPhone("119"), "(11) 9");
  assert.equal(formatPhone("1198765"), "(11) 9876-5");
  assert.equal(formatPhone("11987654321"), "(11) 98765-4321");
  assert.equal(formatPhone("1133334444"), "(11) 3333-4444");
  assert.equal(formatPhone("+5511987654321"), "+55 (11) 98765-4321");
  assert.equal(formatPhone("(11) 98765-4321"), "(11) 98765-4321");
  assert.equal(formatPhone("+1 415 555 0100"), "+1 415 555 0100");
  assert.equal(formatPhone(""), "");
});

test("links são interpretados como escritos no documento", () => {
  assert.deepEqual(describeLink("github.com/EriikGabriel/ufscar-planner"), {
    href: "https://github.com/EriikGabriel/ufscar-planner",
    display: "github.com/EriikGabriel/ufscar-planner",
    kind: "github",
  });
  assert.equal(
    describeLink("https://www.linkedin.com/in/pessoa/")?.kind,
    "linkedin",
  );
  assert.equal(
    describeLink("https://www.linkedin.com/in/pessoa/")?.display,
    "linkedin.com/in/pessoa",
  );
  assert.equal(describeLink("erikgabriel.vercel.app")?.kind, "other");
  assert.equal(describeLink("sem espaço não"), null);
  assert.equal(describeLink("semponto"), null);
  assert.equal(describeLink(""), null);
});

test("iniciais usam o primeiro e o último nome", () => {
  assert.equal(initials("Erik Gabriel"), "EG");
  assert.equal(initials("  maria da Silva Souza "), "MS");
  assert.equal(initials("Madonna"), "M");
  assert.equal(initials(""), "");
});

test("a proficiência reconhece níveis com ou sem acento e em inglês", () => {
  assert.equal(proficiencyIndex("Avançado"), 2);
  assert.equal(proficiencyIndex("avancado"), 2);
  assert.equal(proficiencyIndex("básico"), 0);
  assert.equal(proficiencyIndex("Native"), 4);
  assert.equal(proficiencyIndex("B2"), -1);
});
