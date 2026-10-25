import { IdCard } from "@react-zero-ui/icon-sprite";
import { useState } from "react";
import { previewIdentityFields } from "@/data/profile-preview";
import { SectionCard, TextField } from "./section-card";

export function IdentitySection() {
  const [fields, setFields] = useState(previewIdentityFields);

  function update<K extends keyof typeof fields>(key: K, value: string) {
    setFields((prev) => ({ ...prev, [key]: value }));
  }

  return (
    <SectionCard
      icon={IdCard}
      title="1. Identidade & Dados Profissionais"
      description="Informações cadastrais exibidas no cabeçalho dos seus currículos gerados e no perfil executivo."
      trailing={
        <span className="rounded-full bg-surface-container px-2.5 py-1 text-label-sm font-medium text-on-secondary-container">
          Sincronizado com ATS
        </span>
      }
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <TextField
          label="Nome Completo"
          value={fields.fullName}
          onChange={(value) => update("fullName", value)}
        />
        <TextField
          label="E-mail Profissional"
          type="email"
          value={fields.email}
          onChange={(value) => update("email", value)}
        />
        <TextField
          label="Telefone / WhatsApp"
          type="tel"
          value={fields.phone}
          onChange={(value) => update("phone", value)}
        />
        <TextField
          label="Cargo Atual / Alvo Principal"
          value={fields.headline}
          onChange={(value) => update("headline", value)}
        />
        <div className="sm:col-span-2">
          <TextField
            label="Localização & Mobilidade"
            value={fields.location}
            onChange={(value) => update("location", value)}
          />
        </div>
        <TextField
          label="LinkedIn URL"
          type="url"
          value={fields.linkedinUrl}
          onChange={(value) => update("linkedinUrl", value)}
        />
        <TextField
          label="GitHub / Portfólio de Código"
          type="url"
          value={fields.githubUrl}
          onChange={(value) => update("githubUrl", value)}
        />
      </div>
    </SectionCard>
  );
}
