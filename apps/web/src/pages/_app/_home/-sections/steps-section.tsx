import { MotionEffect } from "@animate/primitives/effects/motion-effect";
import {
  BadgeCheck,
  FileText,
  Search,
  ShieldCheck,
} from "@react-zero-ui/icon-sprite";

const steps = [
  {
    icon: ShieldCheck,
    title: "Ground Truth",
    description:
      "Seu currículo base salvo é a fonte da verdade oficial. A IA nunca alucina experiências: apenas extrai, filtra e reordena seus dados reais.",
  },
  {
    icon: Search,
    title: "Due diligence da vaga",
    description:
      "Verificamos o contexto da empresa e a consistência do anúncio para entender a oportunidade antes de reunir seus requisitos.",
  },
  {
    icon: FileText,
    title: "Análise da Vaga e ATS",
    description:
      "Decomposição minuciosa dos requisitos, palavras-chave e competências essenciais que a vaga específica exige do candidato.",
  },
  {
    icon: BadgeCheck,
    title: "Síntese sob medida",
    description:
      "Conectamos os requisitos da vaga às experiências comprovadas no seu currículo base e organizamos a versão final.",
  },
];

export function StepsSection() {
  return (
    <section className="flex w-full flex-col gap-6 p-2 px-6">
      <MotionEffect
        inView
        slide={{ offset: 16 }}
        className="flex flex-col gap-4 md:flex-row md:justify-between"
      >
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
      </MotionEffect>

      <div className="grid w-full grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-4">
        {steps.map(({ icon: Icon, title, description }, index) => (
          <MotionEffect
            key={title}
            as="article"
            inView
            slide={{ offset: 28 }}
            zoom={{ initialScale: 0.97 }}
            delay={index * 0.1}
            whileHover={{ y: -4 }}
            className="flex flex-col gap-3 rounded-xl border bg-white p-6 drop-shadow-lg"
          >
            <div className="flex justify-center items-center rounded-lg bg-secondary-container size-12 p-3">
              <Icon className="size-6 shrink-0 text-primary" />
            </div>

            <div className="flex flex-col gap-2">
              <span className="text-tertiary uppercase text-xs font-semibold tracking-[0.6px]">
                Etapa {String(index + 1).padStart(2, "0")}
              </span>
              <h3 className="text-primary font-semibold text-xl">{title}</h3>
              <p className="text-body-md">{description}</p>
            </div>
          </MotionEffect>
        ))}
      </div>
    </section>
  );
}
