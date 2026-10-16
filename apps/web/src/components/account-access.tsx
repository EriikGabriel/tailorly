import {
  Dialog,
  DialogDescription,
  DialogHeader,
  DialogPopup,
  DialogTitle,
} from "@animate/base/dialog";
import {
  Tabs,
  TabsList,
  TabsPanel,
  TabsPanels,
  TabsTab,
} from "@animate/base/tabs";
import { Button } from "@animate/buttons/button";
import { DialogBackdrop, DialogPortal } from "@animate/primitives/base/dialog";
import { ApiClientError } from "@lib/api/client";
import { useLogin, useLogout, useRegister } from "@queries/authentication";
import { useCurrentAccount } from "@queries/current-account";
import { UserRound } from "@react-zero-ui/icon-sprite";
import { useAuthStore } from "@stores/auth-store";
import { type SubmitEvent, useState } from "react";

type AuthMode = "login" | "register";

interface AccountAccessProps {
  onProfileClick?: () => void;
}

function getErrorMessage(error: unknown) {
  if (error instanceof ApiClientError || error instanceof Error) {
    return error.message;
  }
  return "Não foi possível concluir a solicitação.";
}

export function AccountAccess({ onProfileClick }: AccountAccessProps) {
  const [open, setOpen] = useState(false);
  const [mode, setMode] = useState<AuthMode>("login");
  const [formError, setFormError] = useState<string | null>(null);
  const accessToken = useAuthStore((state) => state.accessToken);
  const accountQuery = useCurrentAccount();
  const login = useLogin();
  const register = useRegister();
  const logout = useLogout();
  const pending = login.isPending || register.isPending;
  const requestError = mode === "login" ? login.error : register.error;

  function handleOpenChange(nextOpen: boolean) {
    setOpen(nextOpen);
    if (!nextOpen) {
      setFormError(null);
      login.reset();
      register.reset();
    }
  }

  function selectMode(nextMode: AuthMode) {
    setMode(nextMode);
    setFormError(null);
    login.reset();
    register.reset();
  }

  async function handleSubmit(
    event: SubmitEvent<HTMLFormElement>,
    submitMode: AuthMode,
  ) {
    event.preventDefault();
    setFormError(null);

    const form = event.currentTarget;
    const data = new FormData(form);
    const email = String(data.get("email") ?? "").trim();
    const password = String(data.get("password") ?? "");

    if (submitMode === "register") {
      const passwordConfirmation = String(
        data.get("passwordConfirmation") ?? "",
      );
      if (password !== passwordConfirmation) {
        setFormError("As senhas precisam ser iguais.");
        return;
      }
    }

    try {
      if (submitMode === "login") {
        await login.mutateAsync({ email, password });
      } else {
        await register.mutateAsync({ email, password });
      }
      form.reset();
      handleOpenChange(false);
    } catch {
      // A mutation mantém o erro da API para exibição no formulário.
    }
  }

  function handleLogout() {
    logout();
    handleOpenChange(false);
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <Button
        aria-label={accessToken ? "Abrir conta" : "Entrar ou criar conta"}
        type="button"
        className="size-10 rounded-xl"
        onClick={() => {
          onProfileClick?.();
          setOpen(true);
        }}
      >
        <UserRound className="size-6" />
      </Button>
      <DialogPortal>
        <DialogBackdrop className="fixed inset-0 z-50 bg-primary-950/65 backdrop-blur-sm" />
        <DialogPopup className="sm:max-w-md">
          {accessToken ? (
            <AuthenticatedAccount
              email={accountQuery.data?.user.email}
              loading={accountQuery.isPending}
              error={accountQuery.error}
              onLogout={handleLogout}
            />
          ) : (
            <>
              <DialogHeader>
                <DialogTitle>
                  {mode === "login" ? "Entrar no Tailorly" : "Criar sua conta"}
                </DialogTitle>
                <DialogDescription>
                  {mode === "login"
                    ? "Acesse seus currículos e continue de onde parou."
                    : "Crie uma conta para manter seu histórico e seus currículos."}
                </DialogDescription>
              </DialogHeader>

              <Tabs
                value={mode}
                onValueChange={(value) => selectMode(value as AuthMode)}
                className="gap-3"
              >
                <TabsList
                  aria-label="Acesso à conta"
                  className="h-auto min-h-11 w-full gap-1 bg-surface-container p-1 **:data-[slot=motion-highlight]:border-0 **:data-[slot=motion-highlight]:bg-card"
                >
                  <TabsTab
                    value="login"
                    type="button"
                    className="h-9 text-on-surface-variant data-selected:font-semibold data-selected:text-primary-900"
                  >
                    Entrar
                  </TabsTab>
                  <TabsTab
                    value="register"
                    type="button"
                    className="h-9 text-on-surface-variant data-selected:font-semibold data-selected:text-primary-900"
                  >
                    Cadastrar
                  </TabsTab>
                </TabsList>

                <TabsPanels>
                  <TabsPanel value="login" className="px-1 py-1">
                    <AuthForm
                      mode="login"
                      pending={pending}
                      error={requestError}
                      formError={formError}
                      onSubmit={(event) => handleSubmit(event, "login")}
                    />
                  </TabsPanel>
                  <TabsPanel value="register" className="px-1 py-1">
                    <AuthForm
                      mode="register"
                      pending={pending}
                      error={requestError}
                      formError={formError}
                      onSubmit={(event) => handleSubmit(event, "register")}
                    />
                  </TabsPanel>
                </TabsPanels>
              </Tabs>
            </>
          )}
        </DialogPopup>
      </DialogPortal>
    </Dialog>
  );
}

interface AuthFormProps {
  error: unknown;
  formError: string | null;
  mode: AuthMode;
  onSubmit: (event: SubmitEvent<HTMLFormElement>) => void;
  pending: boolean;
}

function AuthForm({
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

interface AuthenticatedAccountProps {
  email: string | undefined;
  error: unknown;
  loading: boolean;
  onLogout: () => void;
}

function AuthenticatedAccount({
  email,
  error,
  loading,
  onLogout,
}: AuthenticatedAccountProps) {
  return (
    <>
      <DialogHeader>
        <DialogTitle>Sua conta</DialogTitle>
        <DialogDescription>
          Sua sessão fica disponível até a página ser recarregada.
        </DialogDescription>
      </DialogHeader>

      <div className="rounded-lg border bg-surface-container-low p-4">
        <p className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
          Email
        </p>
        <p className="mt-1 break-all font-medium">
          {loading ? "Carregando..." : (email ?? "Conta autenticada")}
        </p>
        {error != null && (
          <p className="mt-2 text-sm text-destructive" role="alert">
            {getErrorMessage(error)}
          </p>
        )}
      </div>

      <Button type="button" variant="outline" onClick={onLogout}>
        Sair
      </Button>
    </>
  );
}
