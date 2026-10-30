import { Button } from "@animate/buttons/button";
import { ApiClientError } from "@lib/api/client";
import type { SubmitEvent } from "react";
import type { AuthMode } from "@/types/auth";

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

function AuthField({ label, ...inputProps }: AuthFieldProps) {
  return (
    <label className="grid gap-1.5 text-sm font-medium">
      {label}
      <input
        {...inputProps}
        required
        className="h-11 rounded-md border border-input bg-background px-3 text-base text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:ring-3 focus:ring-ring/20"
      />
    </label>
  );
}
