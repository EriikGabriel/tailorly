import type { DraftFields, ReviewField } from "@@types/review-model";
import {
  Briefcase,
  ExternalLink,
  Github,
  Globe,
  Linkedin,
  Mail,
  MapPin,
  Phone,
  UserRound,
} from "@react-zero-ui/icon-sprite";
import { describeLink, initials } from "@/lib/review-format";
import { ReviewDraftFields } from "../-components/review-draft-fields";
import { ReviewStepShell } from "../-components/review-step-shell";

const identityFields: ReviewField[] = [
  {
    key: "name",
    label: "Nome completo",
    icon: UserRound,
    wide: true,
    hint: "Use o nome presente no documento.",
  },
  {
    key: "headline",
    label: "Cargo ou título profissional",
    icon: Briefcase,
    hint: "Título exibido no topo do currículo, se houver.",
  },
  {
    key: "location",
    label: "Localização",
    icon: MapPin,
    placeholder: "Cidade, estado e país",
  },
  {
    key: "email",
    label: "E-mail",
    type: "email",
    hint: "Canal de contato direto.",
  },
  {
    key: "phone",
    label: "Telefone / WhatsApp",
    type: "tel",
    hint: "Formatado como número brasileiro; números internacionais ficam como estão.",
  },
  {
    key: "summary",
    label: "Resumo profissional",
    type: "textarea",
    wide: true,
    maxLength: 800,
    hint: "Apenas o texto de apresentação que consta no documento.",
  },
];
const linkFields: ReviewField[] = [
  {
    key: "linkedin",
    label: "LinkedIn",
    type: "url",
    placeholder: "linkedin.com/in/usuario",
  },
  {
    key: "github",
    label: "GitHub",
    type: "url",
    placeholder: "github.com/usuario",
  },
  {
    key: "website",
    label: "Site pessoal",
    type: "url",
    wide: true,
    placeholder: "seusite.com",
  },
];

const linkIcons = { github: Github, linkedin: Linkedin, other: Globe } as const;

function IdentityPreview({ values }: { values: DraftFields }) {
  const name = (values.name ?? "").trim();
  const contacts = [
    { icon: Mail, text: values.email },
    { icon: Phone, text: values.phone },
    { icon: MapPin, text: values.location },
  ].filter((contact) => (contact.text ?? "").trim() !== "");
  const links = [values.linkedin, values.github, values.website]
    .map((value) => describeLink(value))
    .filter((link) => link !== null);

  return (
    <div className="flex items-start gap-4 rounded-xl bg-surface-container-low p-4">
      <span
        aria-hidden="true"
        className="flex size-14 shrink-0 items-center justify-center rounded-full bg-primary text-lg font-bold text-primary-foreground"
      >
        {initials(name) || <UserRound className="size-6" />}
      </span>
      <div className="min-w-0 flex-1 space-y-2">
        <div>
          <p className="wrap-break-word text-lg font-semibold text-primary-950">
            {name || "Nome não informado"}
          </p>
          {values.headline?.trim() && (
            <p className="wrap-break-word text-sm text-on-surface-variant">
              {values.headline}
            </p>
          )}
        </div>
        {(contacts.length > 0 || links.length > 0) && (
          <ul className="flex flex-wrap gap-1.5">
            {contacts.map(({ icon: Icon, text }) => (
              <li
                key={text}
                className="flex items-center gap-1.5 rounded-full bg-card px-2.5 py-1 text-xs font-medium"
              >
                <Icon
                  aria-hidden="true"
                  className="size-3.5 text-secondary-700"
                />
                {text}
              </li>
            ))}
            {links.map((link) => {
              const Icon = linkIcons[link.kind];
              return (
                <li key={link.href}>
                  <a
                    href={link.href}
                    target="_blank"
                    rel="noreferrer noopener"
                    className="flex items-center gap-1.5 rounded-full bg-card px-2.5 py-1 text-xs font-medium hover:bg-surface-container focus-visible:outline-2 focus-visible:outline-primary-900"
                  >
                    <Icon
                      aria-hidden="true"
                      className="size-3.5 text-secondary-700"
                    />
                    {link.display}
                    <ExternalLink
                      aria-hidden="true"
                      className="size-3 opacity-60"
                    />
                  </a>
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </div>
  );
}

export function IdentityReviewStep({
  values,
  onChange,
}: {
  values: DraftFields;
  onChange: (values: DraftFields) => void;
}) {
  return (
    <ReviewStepShell
      number={1}
      title="Identificação e Contato"
      description="Dados do candidato e links públicos"
      rule="Nome e ao menos um canal de contato válidos são obrigatórios antes de exportar."
    >
      <IdentityPreview values={values} />
      <h3 className="text-sm font-semibold text-primary-950">
        Dados de contato
      </h3>
      <ReviewDraftFields
        scope="identity"
        fields={identityFields}
        values={values}
        onChange={onChange}
      />
      <h3 className="text-sm font-semibold text-primary-950">
        Presença online
      </h3>
      <ReviewDraftFields
        scope="identity"
        fields={linkFields}
        values={values}
        onChange={onChange}
      />
    </ReviewStepShell>
  );
}
