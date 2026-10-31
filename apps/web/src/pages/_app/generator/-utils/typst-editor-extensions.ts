import {
  type Completion,
  type CompletionContext,
  type CompletionResult,
  snippetCompletion,
} from "@codemirror/autocomplete";
import { type Diagnostic, linter, lintGutter } from "@codemirror/lint";
import {
  EditorState,
  type Extension,
  RangeSetBuilder,
  StateEffect,
  StateField,
} from "@codemirror/state";
import {
  Decoration,
  type DecorationSet,
  EditorView,
  scrollPastEnd,
  ViewPlugin,
  type ViewUpdate,
  WidgetType,
} from "@codemirror/view";
import { applyPickedColor, findColorLiterals } from "./typst-colors";
import { type TypstCompletion, templateCompletions } from "./typst-completions";

function toCompletion(option: TypstCompletion): Completion {
  const base = {
    label: option.label,
    type: option.kind,
    detail: option.detail,
  };
  return option.snippet ? snippetCompletion(option.snippet, base) : base;
}

/** Sugestões do projeto (marcadores `// tailorly:` e receitas de template), somadas às da gramática. */
function templateCompletionSource(
  context: CompletionContext,
): CompletionResult | null {
  const result = templateCompletions(context.state.sliceDoc(0, context.pos));
  if (!result) return null;
  return {
    from: context.pos - result.prefixLength,
    options: result.options.map(toCompletion),
    validFor: /^[\w.-]*$/,
  };
}

/** Marcas de erro vindas da compilação no servidor; o campo as acompanha enquanto a pessoa edita. */
export const setCompilerMarks = StateEffect.define<Diagnostic[]>();

const compilerMarks = StateField.define<Diagnostic[]>({
  create: () => [],
  update(marks, transaction) {
    for (const effect of transaction.effects) {
      if (effect.is(setCompilerMarks)) return effect.value;
    }
    if (!transaction.docChanged || marks.length === 0) return marks;
    return marks.flatMap((mark) => {
      const from = transaction.changes.mapPos(mark.from, 1);
      const to = transaction.changes.mapPos(mark.to, -1);
      return to > from ? [{ ...mark, from, to }] : [];
    });
  },
});

/**
 * Erros do compilador como mais uma fonte do linter. A gramática já registra um linter de sintaxe; o
 * linter junta todas as fontes, então os erros de sintaxe (na hora) e os do compilador convivem.
 */
const compilerLinter: Extension = [
  compilerMarks,
  linter((view) => view.state.field(compilerMarks), {
    needsRefresh: (update) =>
      update.transactions.some((transaction) =>
        transaction.effects.some((effect) => effect.is(setCompilerMarks)),
      ),
  }),
];

/** Amostra de cor ao lado de `rgb("#...")`; escolher outra cor reescreve o texto hexadecimal. */
class SwatchWidget extends WidgetType {
  constructor(
    readonly hex: string,
    readonly from: number,
    readonly to: number,
    readonly original: string,
  ) {
    super();
  }

  eq(other: SwatchWidget) {
    return (
      other.hex === this.hex &&
      other.from === this.from &&
      other.to === this.to &&
      other.original === this.original
    );
  }

  toDOM(view: EditorView) {
    const input = document.createElement("input");
    input.type = "color";
    input.value = this.hex;
    input.className = "cm-color-swatch";
    input.setAttribute("aria-label", `Escolher cor (${this.original})`);
    // `change` (ao fechar o seletor) evita recriar a amostra, e fechar o seletor, a cada ajuste.
    input.addEventListener("change", () => {
      view.dispatch({
        changes: {
          from: this.from,
          to: this.to,
          insert: applyPickedColor(this.original, input.value),
        },
      });
    });
    return input;
  }
}

function buildSwatches(view: EditorView): DecorationSet {
  const builder = new RangeSetBuilder<Decoration>();
  for (const { from, to } of view.visibleRanges) {
    for (const color of findColorLiterals(
      view.state.sliceDoc(from, to),
      from,
    )) {
      builder.add(
        color.exprFrom,
        color.exprFrom,
        Decoration.widget({
          widget: new SwatchWidget(
            color.hex,
            color.from,
            color.to,
            color.original,
          ),
          side: -1,
        }),
      );
    }
  }
  return builder.finish();
}

const colorSwatches = ViewPlugin.fromClass(
  class {
    decorations: DecorationSet;

    constructor(view: EditorView) {
      this.decorations = buildSwatches(view);
    }

    update(update: ViewUpdate) {
      if (update.docChanged || update.viewportChanged) {
        this.decorations = buildSwatches(update.view);
      }
    }
  },
  { decorations: (plugin) => plugin.decorations },
);

/**
 * Extensões do CodeMirror específicas deste editor, além do `basicSetup` e da gramática Typst (que já
 * traz realce, dobra de blocos, comentários, linter de sintaxe e autocompletar da biblioteca padrão):
 * sugestões do template, erros do compilador, amostras de cor, margem de erros, rolagem além do fim e
 * atributos de acessibilidade do campo de código.
 */
export function typstEditorExtensions(): Extension[] {
  return [
    EditorState.languageData.of(() => [
      { autocomplete: templateCompletionSource },
    ]),
    compilerLinter,
    colorSwatches,
    lintGutter(),
    scrollPastEnd(),
    EditorView.contentAttributes.of({
      "aria-label": "Código do template Typst",
      spellcheck: "false",
      autocorrect: "off",
      autocapitalize: "off",
    }),
  ];
}
