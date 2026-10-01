import {
  CheckmarkBadge01Icon,
  FileTextIcon,
  Search01Icon,
  ShieldCheckIcon,
} from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";

export function StepsSection() {
  return (
    <section className="flex w-full flex-col gap-6 p-2 px-6">
      <div className="flex flex-col gap-4 md:flex-row md:justify-between">
        <div className="flex flex-col gap-2 font-semibold">
          <span className="text-secondary uppercase text-xs tracking-[0.6px]">
            Metodologia Editorial
          </span>
          <div>
            <h2 className="text-primary text-2xl">
              Como a síntese do Tailorly opera
            </h2>
            <p className="text-body-md w-full font-normal ">
              Não inventamos fatos. Conectamos rigorosamente seu histórico real
              com as demandas de cada comitê de contratação.
            </p>
          </div>
        </div>
      </div>

      <div className="grid w-full grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-4">
        <article className="flex flex-col gap-3 rounded-xl border bg-white p-6 drop-shadow-lg">
          <div className="flex justify-center items-center rounded-lg bg-secondary-container size-12 p-3">
            <HugeiconsIcon
              icon={ShieldCheckIcon}
              className="size-6 shrink-0 text-primary"
            />
          </div>

          <div className="flex flex-col gap-2">
            <span className="text-tertiary uppercase text-xs font-semibold tracking-[0.6px]">
              Etapa 01
            </span>
            <h3 className="text-primary font-semibold text-xl">Ground Truth</h3>
            <p className="text-body-md">
              Seu currículo base salvo é a fonte da verdade oficial. A IA nunca
              alucina experiências: apenas extrai, filtra e reordena seus dados
              reais.
            </p>
          </div>
        </article>

        <article className="flex flex-col gap-3 rounded-xl border bg-white p-6 drop-shadow-lg">
          <div className="flex justify-center items-center rounded-lg bg-secondary-container size-12 p-3">
            <HugeiconsIcon
              icon={Search01Icon}
              className="size-6 shrink-0 text-primary"
            />
          </div>

          <div className="flex flex-col gap-2">
            <span className="text-tertiary uppercase text-xs font-semibold tracking-[0.6px]">
              Etapa 02
            </span>
            <h3 className="text-primary font-semibold text-xl">
              Due diligence da vaga
            </h3>
            <p className="text-body-md">
              Verificamos o contexto da empresa e a consistência do anúncio para
              entender a oportunidade antes de reunir seus requisitos.
            </p>
          </div>
        </article>

        <article className="flex flex-col gap-3 rounded-xl border bg-white p-6 drop-shadow-lg">
          <div className="flex justify-center items-center rounded-lg bg-secondary-container size-12 p-3">
            <HugeiconsIcon
              icon={FileTextIcon}
              className="size-6 shrink-0 text-primary"
            />
          </div>

          <div className="flex flex-col gap-2">
            <span className="text-tertiary uppercase text-xs font-semibold tracking-[0.6px]">
              Etapa 03
            </span>
            <h3 className="text-primary font-semibold text-xl">
              Análise da Vaga e ATS
            </h3>
            <p className="text-body-md">
              Decomposição minuciosa dos requisitos, palavras-chave e
              competências essenciais que a vaga específica exige do candidato.
            </p>
          </div>
        </article>

        <article className="flex flex-col gap-3 rounded-xl border bg-white p-6 drop-shadow-lg">
          <div className="flex justify-center items-center rounded-lg bg-secondary-container size-12 p-3">
            <HugeiconsIcon
              icon={CheckmarkBadge01Icon}
              className="size-6 shrink-0 text-primary"
            />
          </div>

          <div className="flex flex-col gap-2">
            <span className="text-tertiary uppercase text-xs font-semibold tracking-[0.6px]">
              Etapa 04
            </span>
            <h3 className="text-primary font-semibold text-xl">
              Síntese sob medida
            </h3>
            <p className="text-body-md">
              Conectamos os requisitos da vaga às experiências comprovadas no
              seu currículo base e organizamos a versão final.
            </p>
          </div>
        </article>
      </div>
    </section>
  );
}
