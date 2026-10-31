import assert from "node:assert/strict";
import test from "node:test";
import {
  clampRatio,
  DEFAULT_RATIO,
  MAX_RATIO,
  MIN_RATIO,
  parseStoredRatio,
  ratioForKey,
  ratioFromPointer,
} from "../../src/pages/_app/generator/-utils/split-ratio";
import {
  applyPickedColor,
  findColorLiterals,
  toSixDigitHex,
} from "../../src/pages/_app/generator/-utils/typst-colors";
import { templateCompletions } from "../../src/pages/_app/generator/-utils/typst-completions";
import {
  findContentMarkerLine,
  parseCompilerOutput,
  toEditorDiagnostics,
} from "../../src/pages/_app/generator/-utils/typst-diagnostics";
import {
  insertLink,
  toggleLinePrefix,
  wrapSelection,
} from "../../src/pages/_app/generator/-utils/typst-edits";

function apply(
  doc: string,
  edit: { changes: { from: number; to: number; insert: string }[] },
) {
  let result = doc;
  for (const change of [...edit.changes].sort((a, b) => b.from - a.from)) {
    result =
      result.slice(0, change.from) + change.insert + result.slice(change.to);
  }
  return result;
}

// ---------------------------------------------------------------- divisor

test("a proporção do divisor respeita os limites e valores inválidos voltam ao padrão", () => {
  assert.equal(clampRatio(0.1), MIN_RATIO);
  assert.equal(clampRatio(0.99), MAX_RATIO);
  assert.equal(clampRatio(Number.NaN), DEFAULT_RATIO);
  assert.equal(clampRatio(0.4), 0.4);
});

test("a posição do ponteiro vira proporção dentro do contêiner", () => {
  assert.equal(ratioFromPointer(500, 100, 800), 0.5);
  assert.equal(ratioFromPointer(0, 100, 800), MIN_RATIO);
  assert.equal(ratioFromPointer(5000, 100, 800), MAX_RATIO);
  assert.equal(ratioFromPointer(100, 0, 0), DEFAULT_RATIO);
});

test("as setas movem o divisor e Home/End vão aos limites", () => {
  assert.equal(ratioForKey("ArrowRight", 0.5, false), 0.52);
  assert.equal(ratioForKey("ArrowLeft", 0.5, true), 0.4);
  assert.equal(ratioForKey("Home", 0.5, false), MIN_RATIO);
  assert.equal(ratioForKey("End", 0.5, false), MAX_RATIO);
  assert.equal(ratioForKey("a", 0.5, false), null);
});

test("a proporção guardada é lida com segurança", () => {
  assert.equal(parseStoredRatio(null), null);
  assert.equal(parseStoredRatio("abc"), null);
  assert.equal(parseStoredRatio("0.6"), 0.6);
  assert.equal(parseStoredRatio("9"), MAX_RATIO);
});

// ------------------------------------------------------------ diagnósticos

const COMPILER_OUTPUT = [
  "error: unknown variable: accnt",
  "  ┌─ /work/template.typ:12:20",
  "   │",
  "12 │ #text(fill: accnt)[x]",
  "   │             ^^^^^",
  "   = hint: did you mean accent?",
  "warning: unknown font family: sans",
  "  ┌─ template.typ:3:19",
];

test("lê erro, aviso, localização e dica do compilador", () => {
  const parsed = parseCompilerOutput(COMPILER_OUTPUT);
  assert.equal(parsed.length, 2);
  assert.deepEqual(parsed[0], {
    severity: "error",
    message: "unknown variable: accnt (did you mean accent?)",
    line: 12,
    column: 20,
  });
  assert.equal(parsed[1].severity, "warning");
  assert.equal(parsed[1].line, 3);
});

test("saída sem localização vira diagnóstico sem linha", () => {
  const parsed = parseCompilerOutput(["error: failed to compile"]);
  assert.deepEqual(parsed, [
    {
      severity: "error",
      message: "failed to compile",
      line: null,
      column: null,
    },
  ]);
});

test("acha o marcador de conteúdo do template", () => {
  assert.equal(findContentMarkerLine("a\nb\n// tailorly:content\nc"), 3);
  assert.equal(findContentMarkerLine("a\nb"), null);
});

test("diagnósticos viram marcas só nas linhas anteriores ao marcador de conteúdo", () => {
  const source = [
    "#let accent = 1",
    "#text(fill: accnt)[x]",
    "// tailorly:content",
    "depois",
  ].join("\n");
  const parsed = parseCompilerOutput([
    "error: unknown variable: accnt",
    "  ┌─ t.typ:2:13",
    "error: algo no corpo gerado",
    "  ┌─ t.typ:40:1",
    "error: sem linha",
  ]);

  const { marks, unmapped } = toEditorDiagnostics(parsed, source);

  assert.equal(unmapped, 2);
  assert.equal(marks.length, 1);
  assert.equal(source.slice(marks[0].from, marks[0].to), "accnt");
  assert.equal(marks[0].severity, "error");
});

// ------------------------------------------------------------------- cores

test("acha cores rgb com a posição do texto hexadecimal", () => {
  const text =
    '#let accent = rgb("#0f4c5c")\n#line(stroke: 1pt + rgb( "#FFF" ))\n#text(fill: rgb("zzz"))';
  const colors = findColorLiterals(text);

  assert.equal(colors.length, 2);
  assert.equal(text.slice(colors[0].from, colors[0].to), "#0f4c5c");
  assert.equal(colors[0].hex, "#0f4c5c");
  assert.equal(colors[1].hex, "#ffffff");
});

test("converte hexadecimais curtos e com transparência", () => {
  assert.equal(toSixDigitHex("#abc"), "#aabbcc");
  assert.equal(toSixDigitHex("#abcd"), "#aabbcc");
  assert.equal(toSixDigitHex("#11223344"), "#112233");
});

test("a cor escolhida mantém a transparência original", () => {
  assert.equal(applyPickedColor("#0f4c5c", "#112233"), "#112233");
  assert.equal(applyPickedColor("#0f4c5c80", "#112233"), "#11223380");
  assert.equal(applyPickedColor("#abcd", "#112233"), "#112233dd");
});

// ------------------------------------------------------------ autocompletar

test("depois de # sugere as receitas de template que começam com o que foi digitado", () => {
  const result = templateCompletions('#set page(paper: "a4")\n#se');
  assert.ok(result);
  assert.equal(result.prefixLength, 2);
  const labels = result.options.map((option) => option.label);
  assert.deepEqual(labels, ["set text", "set page", "set par", "set list"]);
});

test("a receita de cor de destaque aparece depois de #le", () => {
  const result = templateCompletions("#le");
  assert.deepEqual(
    result?.options.map((option) => option.label),
    ["let accent"],
  );
  assert.ok(result?.options[0].snippet?.includes("rgb("));
});

test("sugere os marcadores tailorly", () => {
  const marker = templateCompletions("// tailorly:co");
  assert.deepEqual(marker?.options.map((option) => option.label).sort(), [
    "compact",
    "content",
  ]);
  assert.equal(marker?.prefixLength, 2);
  assert.equal(templateCompletions("// tailorly:")?.options.length, 4);
});

test("não sugere nada dentro de strings, comentários comuns ou fora de uma chamada com #", () => {
  assert.equal(templateCompletions('#link("#se'), null);
  assert.equal(templateCompletions("// veja #se"), null);
  assert.equal(templateCompletions("texto sem hash se"), null);
  assert.equal(templateCompletions("#zzz"), null);
});

// ----------------------------------------------------------------- edições

test("envolve a seleção e desfaz o envolvimento", () => {
  const doc = "um texto aqui";
  const wrapped = wrapSelection(doc, 3, 8, "*", "*", "negrito");
  assert.equal(apply(doc, wrapped), "um *texto* aqui");
  assert.deepEqual(wrapped.selection, { anchor: 4, head: 9 });

  const again = wrapSelection("um *texto* aqui", 4, 9, "*", "*", "negrito");
  assert.equal(apply("um *texto* aqui", again), "um texto aqui");
});

test("sem seleção insere o texto de exemplo já selecionado", () => {
  const edit = wrapSelection("ab", 1, 1, "_", "_", "itálico");
  assert.equal(apply("ab", edit), "a_itálico_b");
  assert.deepEqual(edit.selection, { anchor: 2, head: 9 });
});

test("liga e desliga o prefixo em todas as linhas selecionadas", () => {
  const doc = "um\ndois\n\ntrês";
  const on = toggleLinePrefix(doc, 0, doc.length, "- ");
  const added = apply(doc, on);
  assert.equal(added, "- um\n- dois\n\n- três");

  const off = toggleLinePrefix(added, 0, added.length, "- ");
  assert.equal(apply(added, off), doc);
});

test("o prefixo não atinge a linha em que a seleção só encosta no começo", () => {
  const doc = "um\ndois";
  const edit = toggleLinePrefix(doc, 0, 3, "// ");
  assert.equal(apply(doc, edit), "// um\ndois");
});

test("insere um link com o endereço selecionado", () => {
  const edit = insertLink("veja aqui", 5, 9);
  assert.equal(apply("veja aqui", edit), 'veja #link("https://")[aqui]');
  assert.equal(
    apply("veja aqui", edit).slice(edit.selection.anchor, edit.selection.head),
    "https://",
  );
});
