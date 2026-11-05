import { MAX_JOB_TEXT_LENGTH } from "@utils/job/job-posting-input";

/**
 * Área do texto colado da vaga com contador de caracteres usados em relação ao limite do servidor.
 * O contador fica abaixo do campo e fica em alerta perto do limite, para a pessoa não perder o fim
 * da vaga sem saber (o analisador só lê até o limite).
 */
export function JobTextField({
  value,
  onChange,
  placeholder,
  className,
}: {
  value: string;
  onChange: (value: string) => void;
  placeholder: string;
  className: string;
}) {
  const used = value.length;
  const nearLimit = used >= MAX_JOB_TEXT_LENGTH * 0.9;
  const counterId = "job-text-counter";

  return (
    <div className="flex flex-col gap-1.5">
      <textarea
        aria-label="Texto da vaga"
        aria-describedby={counterId}
        placeholder={placeholder}
        value={value}
        maxLength={MAX_JOB_TEXT_LENGTH}
        onChange={(event) => onChange(event.target.value)}
        className={className}
      />
      <p
        id={counterId}
        className={`self-end px-1 text-xs tabular-nums ${
          nearLimit ? "font-semibold text-error" : "text-on-surface-variant"
        }`}
      >
        {used.toLocaleString("pt-BR")} /{" "}
        {MAX_JOB_TEXT_LENGTH.toLocaleString("pt-BR")} caracteres
      </p>
    </div>
  );
}
