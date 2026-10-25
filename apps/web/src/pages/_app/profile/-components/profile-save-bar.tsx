import { Button } from "@animate/buttons/button";
import { Info } from "@react-zero-ui/icon-sprite";

export function ProfileSaveBar() {
  return (
    <div className="sticky bottom-4 z-10 flex flex-wrap items-center justify-between gap-3 rounded-xl bg-surface-container px-4 py-4 shadow-2">
      <p className="flex items-center gap-2 text-label-sm font-semibold text-on-surface-variant">
        <Info aria-hidden="true" className="size-4 shrink-0" />
        Todas as alterações de configurações respeitam os limites da
        criptografia local.
      </p>
      <div className="flex items-center gap-2">
        <Button variant="ghost">Descartar</Button>
        <Button>Salvar Alterações</Button>
      </div>
    </div>
  );
}
