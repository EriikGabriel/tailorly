import { type ChangeEvent, useRef } from "react";

/**
 * Seletor de PDF por input oculto. `open` abre o diálogo do sistema (também usado por botões
 * em outros componentes) e `onChange` repassa o arquivo escolhido, limpando o input para que
 * escolher o mesmo arquivo de novo dispare a seleção.
 */
export function usePdfFilePicker(onSelect: (file: File) => void) {
  const inputRef = useRef<HTMLInputElement>(null);

  return {
    inputRef,
    open: () => inputRef.current?.click(),
    onChange(event: ChangeEvent<HTMLInputElement>) {
      const selected = event.target.files?.[0];
      event.target.value = "";
      if (selected) onSelect(selected);
    },
  };
}
