import { ShieldCheck } from "@react-zero-ui/icon-sprite";

/** Título e explicação da página de auditoria do Currículo Base. */
export function BasePageIntro() {
  return (
    <div className="flex max-w-2xl flex-col gap-1 xl:max-w-[46%]">
      <p className="flex items-center gap-1 text-xs font-semibold leading-4 tracking-[0.6px] text-muted-foreground uppercase">
        <ShieldCheck
          aria-hidden="true"
          className="size-5 shrink-0 text-primary-950"
        />
        Cofre de conhecimento imutável
      </p>
      <h1
        id="base-heading"
        className="text-headline-lg-mobile font-bold tracking-tight sm:text-headline-lg sm:font-bold sm:tracking-[-0.8px]"
      >
        Auditoria &amp; Curadoria dos Fatos do Currículo Base
      </h1>
      <p className="text-body-md text-on-surface-variant">
        Validação estruturada dos dados extraídos do seu currículo base. Revise
        fatos, credenciais e métricas para que ele seja a fonte das próximas
        adaptações, com{" "}
        <strong className="font-semibold text-primary-950">
          informações confirmadas por você
        </strong>
        .
      </p>
    </div>
  );
}
