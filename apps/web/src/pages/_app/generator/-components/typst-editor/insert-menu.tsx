import {
  Menu,
  MenuGroup,
  MenuGroupLabel,
  MenuItem,
  MenuPanel,
  MenuSeparator,
  MenuTrigger,
} from "@animate/base/menu";
import { ChevronDown, Plus } from "@react-zero-ui/icon-sprite";
import { useRef } from "react";
import { TYPST_SNIPPET_GROUPS } from "../../-constants/typst-snippets";

/** Menu "Inserir": trechos de Typst prontos (cor de destaque, margens, estilo de títulos, colunas...). */
export function InsertMenu({
  onInsert,
}: {
  onInsert: (template: string) => void;
}) {
  const pickedRef = useRef(false);

  return (
    <Menu>
      <MenuTrigger
        aria-label="Inserir trecho de Typst"
        className="flex h-7 items-center gap-1 rounded-md px-1.5 text-xs font-medium text-on-surface-variant outline-none transition-colors hover:bg-surface-container hover:text-on-surface focus-visible:ring-2 focus-visible:ring-ring data-popup-open:bg-surface-container"
      >
        <Plus className="size-3.75" aria-hidden="true" />
        Inserir
        <ChevronDown className="size-3" aria-hidden="true" />
      </MenuTrigger>
      {/* Ao escolher um trecho, o foco fica no editor (para continuar digitando); ao fechar sem escolher, volta ao botão. */}
      <MenuPanel
        align="start"
        className="max-h-96 min-w-64"
        finalFocus={() => {
          const picked = pickedRef.current;
          pickedRef.current = false;
          return !picked;
        }}
      >
        {TYPST_SNIPPET_GROUPS.map((group, index) => (
          <MenuGroup key={group.title}>
            {index > 0 && <MenuSeparator />}
            <MenuGroupLabel className="text-xs text-on-surface-variant">
              {group.title}
            </MenuGroupLabel>
            {group.snippets.map((snippet) => (
              <MenuItem
                key={snippet.label}
                onClick={() => {
                  pickedRef.current = true;
                  onInsert(snippet.template);
                }}
              >
                {snippet.label}
              </MenuItem>
            ))}
          </MenuGroup>
        ))}
      </MenuPanel>
    </Menu>
  );
}
