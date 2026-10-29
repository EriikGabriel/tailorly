import assert from "node:assert/strict";
import test from "node:test";
import { checkJobUrl, MAX_JOB_URL_LENGTH } from "../../src/utils/job-url";

test("campo vazio não é erro", () => {
  assert.equal(checkJobUrl("").status, "empty");
  assert.equal(checkJobUrl("   ").status, "empty");
});

test("aceita links públicos e reconhece plataformas", () => {
  assert.deepEqual(checkJobUrl(" https://acme.gupy.io/jobs/1?x=1 "), {
    status: "valid",
    message: "Link da Gupy reconhecido.",
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
