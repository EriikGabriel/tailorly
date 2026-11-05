import { Button } from "@animate/buttons/button";
import { Save } from "@react-zero-ui/icon-sprite";

type TemplateActionsBarProps = {
  dirty: boolean;
  saving: boolean;
  /** Template salvo que a geração aberta ainda não usa. */
  saved: boolean;
  canRestore: boolean;
  /** "Nome vN" do template em uso, mostrado quando não há alterações pendentes. */
  templateLabel: string;
  onSave: () => void;
  onRestore: () => void;
};

/** Ações do template Typst: salvar (sem gerar de novo) ou restaurar o padrão. */
export function TemplateActionsBar({
  dirty,
  saving,
  saved,
  canRestore,
  templateLabel,
  onSave,
  onRestore,
}: TemplateActionsBarProps) {
  return (
    <div className="flex flex-wrap items-center gap-2 rounded-lg bg-card px-3 py-2 shadow-xs">
      <Button
        type="button"
        onClick={onSave}
        disabled={!dirty || saving}
        className="h-9 gap-1.5 rounded-lg bg-primary-900 px-3 text-xs font-semibold text-primary-foreground disabled:opacity-40"
      >
        <Save className="size-3.5" aria-hidden="true" />
        {saving ? "Salvando..." : "Salvar template"}
      </Button>
      <Button
        type="button"
        variant="outline"
        onClick={onRestore}
        disabled={!canRestore}
        className="h-9 rounded-lg px-3 text-xs font-semibold text-on-surface disabled:opacity-40"
      >
        Restaurar template padrão
      </Button>
      <span className="text-xs text-on-surface-variant">
        {dirty
          ? "Alterações não salvas: o PDF acima é só uma prévia."
          : saved
            ? "Template salvo. Use “Gerar currículo sob medida” para aplicá-lo em um novo currículo."
            : templateLabel}
      </span>
    </div>
  );
}
