import { Button } from "@animate/buttons/button";
import { useLogout } from "@queries/authentication";
import {
  Eye,
  House,
  LogOut,
  Save,
  ShieldCheck,
} from "@react-zero-ui/icon-sprite";
import { Link } from "@tanstack/react-router";

export function ProfilePageHeader() {
  const logout = useLogout();

  return (
    <div className="flex flex-wrap items-end justify-between gap-6">
      <div className="flex flex-col gap-1">
        <nav aria-label="Caminho de navegação">
          <ol className="flex flex-wrap items-center gap-1 text-label-sm text-on-surface-variant">
            <li>
              <Link
                to="/"
                className="inline-flex items-center gap-1 rounded-sm font-semibold hover:text-primary focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring"
              >
                <House aria-hidden="true" className="size-3.5" />
                Início
              </Link>
            </li>
            <li aria-hidden="true" className="text-outline-variant">
              /
            </li>
            <li className="font-medium text-primary-950">
              Minha Conta &amp; Perfil Profissional
            </li>
          </ol>
        </nav>

        <div className="flex flex-wrap items-center gap-2 pt-1">
          <h1 className="text-headline-lg-mobile font-semibold tracking-tight text-primary-950 sm:text-headline-lg sm:tracking-[-0.8px]">
            Perfil Profissional &amp; Configurações
          </h1>
          <span className="inline-flex items-center gap-1 rounded-full bg-secondary-container px-2.5 py-0.5 text-label-sm text-on-secondary-container">
            <ShieldCheck aria-hidden="true" className="size-3" />
            Ground Truth Ativo
          </span>
        </div>

        <p className="max-w-3xl text-body-sm text-on-surface-variant">
          Gerencie seu Currículo Base inalterável (Ground Truth), conexões com
          Second Brain, preferências de síntese algorítmica e histórico de
          candidaturas.
        </p>
      </div>

      <div className="flex shrink-0 items-center gap-2">
        <Button
          type="button"
          variant="outline"
          size="lg"
          aria-label="Sair da conta"
          onClick={logout}
        >
          <LogOut aria-hidden="true" className="size-4" />
          Sair
        </Button>
        <Button variant="accent" size="lg" className="text-primary-950">
          <Eye aria-hidden="true" className="size-4" />
          Pré-visualizar Perfil
        </Button>
        <Button size="lg">
          <Save aria-hidden="true" className="size-4" />
          Salvar Alterações
        </Button>
      </div>
    </div>
  );
}
