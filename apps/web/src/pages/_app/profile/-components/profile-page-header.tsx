import { Button } from "@animate/buttons/button";
import { PageHeading } from "@components/page-heading";
import { useLogout } from "@queries/authentication";
import { LogOut, UserRound } from "@react-zero-ui/icon-sprite";

export function ProfilePageHeader() {
  const logout = useLogout();

  return (
    <PageHeading
      headingId="profile-heading"
      breadcrumb="Minha Conta & Perfil Profissional"
      status="Ground Truth Ativo"
      eyebrowIcon={UserRound}
      eyebrow="Conta & configurações"
      title="Perfil Profissional & Configurações"
      description={
        <>
          Gerencie seu Currículo Base inalterável (Ground Truth), conexões com
          Second Brain, preferências de síntese algorítmica e{" "}
          <strong className="font-semibold text-primary-950">
            histórico de candidaturas
          </strong>
          .
        </>
      }
      actions={
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
      }
    />
  );
}
