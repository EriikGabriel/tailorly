import { format, isValid, parseISO } from "date-fns";
import { ptBR } from "date-fns/locale";

/** Formata uma data ISO como `dd/MM/yyyy` (ou com hora); devolve `null` se vazia ou inválida. */
export function formatDate(
  value: string | undefined,
  withTime = false,
): string | null {
  if (!value) return null;
  const date = parseISO(value);
  return isValid(date)
    ? format(date, withTime ? "dd/MM/yyyy HH:mm" : "dd/MM/yyyy", {
        locale: ptBR,
      })
    : null;
}
