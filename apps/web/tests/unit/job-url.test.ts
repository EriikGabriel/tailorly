import assert from "node:assert/strict";
import test from "node:test";
import {
  checkJobUrl,
  FULLY_COMPATIBLE_PLATFORMS,
  MAX_JOB_URL_LENGTH,
} from "@utils/job/job-url";

test("campo vazio não é erro", () => {
  assert.equal(checkJobUrl("").status, "empty");
  assert.equal(checkJobUrl("   ").status, "empty");
});

test("aceita links públicos e reconhece plataformas", () => {
  assert.deepEqual(checkJobUrl(" https://acme.gupy.io/jobs/1?x=1 "), {
    status: "valid",
    message: "Link da Gupy: plataforma totalmente compatível.",
    platform: "Gupy",
    fullyCompatible: true,
  });
  assert.equal(
    checkJobUrl("https://boards.greenhouse.io/acme/jobs/1").status,
    "valid",
  );
  assert.equal(checkJobUrl("http://acme.com.br/vagas/dev:2").status, "valid");
  assert.equal(checkJobUrl("https://acme.com:443/vaga").status, "valid");
});

test("rejeita link sem esquema, com espaço ou malformado", () => {
  assert.equal(checkJobUrl("acme.gupy.io/jobs/1").status, "invalid");
  assert.equal(checkJobUrl("https://acme .com/x").status, "invalid");
  assert.equal(checkJobUrl("https://").status, "invalid");
});

test("rejeita esquemas que não são http e credenciais no link", () => {
  for (const value of [
    "ftp://acme.com/x",
    "file:///etc/passwd",
    "javascript:alert(1)",
    "https://user:pass@acme.com/",
  ]) {
    assert.equal(checkJobUrl(value).status, "invalid", value);
  }
});

test("rejeita destinos internos e portas não padrão", () => {
  for (const value of [
    "http://localhost/",
    "http://127.0.0.1/",
    "http://10.0.0.5/x",
    "http://172.20.1.1/x",
    "http://192.168.0.10/x",
    "http://169.254.169.254/latest/meta-data/",
    "http://100.64.0.1/",
    "http://[::1]/",
    "http://intranet/vaga",
    "http://servico.internal/vaga",
    "https://acme.com:8443/x",
    "http://acme.com:22/",
  ]) {
    assert.equal(checkJobUrl(value).status, "invalid", value);
  }
  assert.equal(checkJobUrl("http://172.32.0.1/x").status, "valid");
});

test("avisa que o LinkedIn exige o texto colado", () => {
  assert.equal(
    checkJobUrl("https://www.linkedin.com/jobs/view/123").status,
    "warning",
  );
});

test("rejeita links acima do limite do servidor", () => {
  assert.equal(
    checkJobUrl(`https://acme.com/${"a".repeat(MAX_JOB_URL_LENGTH)}`).status,
    "invalid",
  );
});

test("marca Gupy e Trakstar Hire como totalmente compatíveis", () => {
  for (const [value, platform] of [
    ["https://empresa.gupy.io/jobs/123", "Gupy"],
    ["https://sensedia.hire.trakstar.com/jobs/fk0zc1c/", "Trakstar Hire"],
  ]) {
    const check = checkJobUrl(value);
    assert.equal(check.status, "valid", value);
    assert.ok(check.status === "valid" && check.fullyCompatible, value);
    assert.ok(check.status === "valid" && check.platform === platform, value);
  }
  assert.deepEqual(
    FULLY_COMPATIBLE_PLATFORMS.map(({ name }) => name),
    ["Gupy", "Trakstar Hire"],
  );
});

test("Greenhouse e Lever são reconhecidas, mas sem o selo de compatibilidade total", () => {
  for (const value of [
    "https://boards.greenhouse.io/acme/jobs/1",
    "https://jobs.lever.co/acme/abc",
    "https://exemplo.com/vaga/1",
  ]) {
    const check = checkJobUrl(value);
    assert.equal(check.status, "valid", value);
    assert.ok(check.status === "valid" && !check.fullyCompatible, value);
  }
});

test("um domínio parecido não ganha o selo", () => {
  const check = checkJobUrl("https://hire.trakstar.com.evil.test/jobs/1");
  assert.ok(check.status === "valid" && !check.fullyCompatible);
});
