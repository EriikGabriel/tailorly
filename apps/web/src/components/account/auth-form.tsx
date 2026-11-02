import type { AuthMode } from "@@types/auth";
import { Button } from "@animate/buttons/button";
import { ApiClientError } from "@api/client";
import { Eye, EyeOff } from "@react-zero-ui/icon-sprite";
import { type SubmitEvent, useId, useState } from "react";

function getErrorMessage(error: unknown) {
  if (error instanceof ApiClientError || error instanceof Error) {
    return error.message;
  }
  return "Não foi possível concluir a solicitação.";
}

interface AuthFormProps {
  error: unknown;
  formError: string | null;
  mode: AuthMode;
  onSubmit: (event: SubmitEvent<HTMLFormElement>) => void;
  pending: boolean;
}

/** Formulário de login ou cadastro; o envio e a validação das senhas ficam com quem o usa. */
export function AuthForm({
  error,
  formError,
  mode,
  onSubmit,
  pending,
}: AuthFormProps) {
  return (
    <form className="grid gap-4" onSubmit={onSubmit}>
      <AuthField autoComplete="email" label="Email" name="email" type="email" />
      <AuthField
        autoComplete={mode === "login" ? "current-password" : "new-password"}
        label="Senha"
        minLength={8}
        maxLength={128}
        name="password"
        type="password"
      />
      {mode === "register" && (
        <AuthField
          autoComplete="new-password"
          label="Confirme a senha"
          minLength={8}
          maxLength={128}
          name="passwordConfirmation"
          type="password"
        />
      )}

      {(formError != null || error != null) && (
        <p
          className="rounded-md bg-error-container px-3 py-2 text-sm text-on-error-container"
          role="alert"
        >
          {formError ?? getErrorMessage(error)}
        </p>
      )}

      <Button type="submit" size="lg" disabled={pending}>
        {pending ? "Aguarde..." : mode === "login" ? "Entrar" : "Criar conta"}
      </Button>
    </form>
  );
}

interface AuthFieldProps {
  autoComplete: string;
  label: string;
  maxLength?: number;
  minLength?: number;
  name: string;
  type: "email" | "password";
}

/** Campo do formulário; senhas ganham o botão para mostrar ou ocultar o texto digitado. */
function AuthField({ label, type, ...inputProps }: AuthFieldProps) {
  const id = useId();
  const [visible, setVisible] = useState(false);
  const isPassword = type === "password";
  const Icon = visible ? EyeOff : Eye;

  return (
    <div className="grid gap-1.5 text-sm font-medium">
      <label htmlFor={id}>{label}</label>
      <div className="relative">
        <input
          {...inputProps}
          id={id}
          type={isPassword && visible ? "text" : type}
          required
          className={`h-11 w-full rounded-md border border-input bg-background px-3 text-base text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:ring-3 focus:ring-ring/20 ${isPassword ? "pr-11" : ""}`}
        />
        {isPassword && (
          <button
            type="button"
            aria-label={visible ? "Ocultar senha" : "Mostrar senha"}
            aria-pressed={visible}
            aria-controls={id}
            onClick={() => setVisible((current) => !current)}
            className="absolute top-1/2 right-1 flex size-9 -translate-y-1/2 items-center justify-center rounded-md text-on-surface-variant outline-none hover:bg-surface-container hover:text-primary-950 focus-visible:ring-3 focus-visible:ring-ring/50"
          >
            <Icon aria-hidden="true" className="size-4" />
          </button>
        )}
      </div>
    </div>
  );
}
