import { PageHeading } from "@components/page-heading";
import { History } from "@react-zero-ui/icon-sprite";
import { createFileRoute } from "@tanstack/react-router";
import { CvLibrary } from "./-components/cv-library";

export const Route = createFileRoute("/_app/cvs/")({
  component: CVsPage,
});

function CVsPage() {
  return (
    <main className="min-h-[calc(100dvh-4rem)] px-4 py-6 sm:px-8 lg:px-16">
      <section
        aria-labelledby="cvs-heading"
        className="mx-auto flex w-full max-w-384 flex-col gap-4 rounded-2xl p-4 shadow-xs sm:p-6"
      >
        <PageHeading
          headingId="cvs-heading"
          breadcrumb="Meus Currículos"
          eyebrowIcon={History}
          eyebrow="Histórico de gerações"
          title="Meus Currículos"
          description={
            <>
              Cada currículo registra o Ground Truth, a versão da vaga e o
              template usados. Visitantes veem aqui{" "}
              <strong className="font-semibold text-primary-950">
                só o que geraram nesta sessão
              </strong>
              , que expira.
            </>
          }
        />
        <CvLibrary />
      </section>
    </main>
  );
}
