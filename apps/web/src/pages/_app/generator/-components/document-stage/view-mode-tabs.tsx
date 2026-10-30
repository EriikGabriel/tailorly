import { Tabs, TabsList, TabsTab } from "@animate/base/tabs";
import { Code2, Columns2, Eye } from "@react-zero-ui/icon-sprite";
import type { ViewMode } from "../../-types/view-mode";

const TAB_CLASS =
  "h-7.5 gap-1.5 px-2.5 text-xs tracking-[0.3px] text-on-surface-variant data-selected:font-semibold data-selected:text-on-surface";

/** Alternância entre PDF, código Typst e visão dividida. */
export function ViewModeTabs({
  value,
  onChange,
}: {
  value: ViewMode;
  onChange: (mode: ViewMode) => void;
}) {
  return (
    <Tabs value={value} onValueChange={(next) => onChange(next as ViewMode)}>
      <TabsList
        aria-label="Modo de visualização do currículo"
        className="h-auto min-h-9.5 gap-1 bg-surface-container p-1 **:data-[slot=motion-highlight]:border-0 **:data-[slot=motion-highlight]:bg-card"
      >
        <TabsTab value="visual" type="button" className={TAB_CLASS}>
          <Eye className="size-3.5" aria-hidden="true" />
          PDF
        </TabsTab>
        <TabsTab value="typst" type="button" className={TAB_CLASS}>
          <Code2 className="size-3.5" aria-hidden="true" />
          Typst
        </TabsTab>
        <TabsTab value="split" type="button" className={TAB_CLASS}>
          <Columns2 className="size-3.5" aria-hidden="true" />
          Dividido
        </TabsTab>
      </TabsList>
    </Tabs>
  );
}
