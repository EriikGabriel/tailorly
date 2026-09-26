import {
  Tabs,
  TabsList,
  TabsPanel,
  TabsPanels,
  TabsTab,
} from "@animate/base/tabs";
import {
  ArrowRight01Icon,
  BoltIcon,
  BrainCircuitIcon,
  CheckmarkBadge01Icon,
  ClipboardPasteIcon,
  FileTextIcon,
  FileUploadIcon,
  Link01Icon,
  Refresh01Icon,
  Search01Icon,
  ShieldCheckIcon,
} from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { Link } from "@tanstack/react-router";
import { useState } from "react";

type InputMode = "url" | "text" | "cv";

const inputModes = [
  { value: "url", label: "Inserir URL da Vaga", icon: Link01Icon },
  { value: "text", label: "Colar Texto da Vaga", icon: FileTextIcon },
  { value: "cv", label: "CV Base / Direto", icon: FileUploadIcon },
] as const;

const quickTests = [
  "Senior Tech Lead @ Nubank",
  "Staff Engineer @ Inter",
  "Product Designer @ Nubank",
] as const;

export function HeroIngestionCard() {
  const [mode, setMode] = useState<InputMode>("url");
  const [jobUrl, setJobUrl] = useState("");
  const [jobText, setJobText] = useState("");
  const [cvName, setCvName] = useState("");

  async function pasteUrl() {
    try {
      const text = await navigator.clipboard.readText();
      setJobUrl(text.trim());
    } catch {
      // The URL field remains available when clipboard permission is denied.
    }
  }

  return (
    <article className="mt-4 w-full max-w-5xl overflow-hidden border border-primary-200 rounded-xl bg-card shadow-[0_12px_16px_rgb(62_39_35/8%),0_2px_3px_rgb(62_39_35/4%)]">
      <div className="px-5 pt-6 sm:px-10 sm:pt-10">
        <div className="flex min-h-8.5 flex-wrap items-center justify-between gap-x-3 gap-y-2 rounded-lg border border-outline-variant/30 bg-surface-container px-4 py-2 text-xs tracking-[0.3px]">
          <div className="flex min-w-0 items-center gap-1 text-primary-900">
            <HugeiconsIcon
              icon={ShieldCheckIcon}
              className="size-3.75 shrink-0"
            />
            <span className="min-w-0 truncate">
              <strong className="font-semibold">
                Ground Truth Selecionado:
              </strong>
              <span className="text-on-surface">
                <HugeiconsIcon
                  icon={FileTextIcon}
                  className="mr-0.5 inline size-3 align-[-2px]"
                />
                Currículo Base - Tech Lead / Staff (Atualizado há 3 dias)
              </span>
            </span>
          </div>
          <Link
            className="inline-flex shrink-0 items-center gap-0.5 font-semibold text-secondary-700 hover:text-primary-900 focus-visible:rounded-sm focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-900"
            to="/cvs"
          >
            <HugeiconsIcon icon={Refresh01Icon} className="size-3" />
            Gerenciar / Trocar
          </Link>
        </div>

        <Tabs
          value={mode}
          onValueChange={(value) => setMode(value as InputMode)}
          className="gap-0"
        >
          <TabsList
            aria-label="Forma de informar a vaga"
            className="mt-4 h-auto min-h-13 max-w-full flex-wrap gap-1.5 bg-surface-container p-1.5 **:data-[slot=motion-highlight]:border-0 **:data-[slot=motion-highlight]:bg-card"
          >
            {inputModes.map(({ value, label, icon }) => (
              <TabsTab
                key={value}
                value={value}
                type="button"
                className="h-10 gap-1 px-5 py-2 text-sm tracking-[0.14px] text-on-surface-variant data-selected:font-semibold data-selected:text-primary-900"
              >
                <HugeiconsIcon icon={icon} className="size-3.75" />
                {label}
              </TabsTab>
            ))}
          </TabsList>

          <div className="mt-4 flex flex-wrap items-center gap-1 text-xs tracking-[0.3px]">
            <HugeiconsIcon
              icon={BrainCircuitIcon}
              className="mr-0.5 size-4.5 text-secondary-700"
            />
            <span className="font-medium text-primary-950">
              Second Brain Opcional:
            </span>
            <span className="font-semibold text-on-surface-variant">
              conecte ou use modo direto
            </span>
          </div>

          <div className="mt-6 pb-6">
            <TabsPanels className="-mx-1">
              <TabsPanel value="url" className="p-1">
                <div className="relative flex min-h-12.75 items-center rounded-lg bg-surface-container-low shadow-xs focus-within:ring-2 focus-within:ring-primary-300">
                  <HugeiconsIcon
                    icon={Search01Icon}
                    className="pointer-events-none absolute left-4.5 size-4.75 text-secondary-700"
                  />
                  <input
                    type="url"
                    aria-label="Link da vaga"
                    placeholder="Cole o link da vaga (LinkedIn, Gupy, Greenhouse, Lever...)"
                    value={jobUrl}
                    onChange={(event) => setJobUrl(event.target.value)}
                    className="min-h-12.75 w-full rounded-lg bg-transparent pl-12 pr-21 text-[15px] text-on-surface outline-none placeholder:text-on-surface-variant/60"
                  />
                  <button
                    type="button"
                    onClick={pasteUrl}
                    className="absolute right-2 inline-flex items-center gap-1 rounded-md px-2 py-1.5 text-xs font-semibold tracking-[0.3px] text-secondary-700 hover:bg-surface-container focus-visible:outline-2 focus-visible:outline-primary-900"
                  >
                    <HugeiconsIcon
                      icon={ClipboardPasteIcon}
                      className="size-3.25"
                    />
                    Colar
                  </button>
                </div>
                <div className="mt-4 flex flex-wrap items-center gap-1 pt-1 text-xs tracking-[0.3px]">
                  <span className="mr-1 inline-flex items-center gap-1 font-medium text-on-surface-variant">
                    <HugeiconsIcon icon={BoltIcon} className="size-3.25" />
                    Testar com 1 clique:
                  </span>
                  {quickTests.map((title) => (
                    <button
                      key={title}
                      type="button"
                      onClick={() => {
                        setJobText(title);
                        setMode("text");
                      }}
                      className="rounded-full bg-surface-container px-2 py-1 font-semibold text-primary-900 hover:bg-primary-100 focus-visible:outline-2 focus-visible:outline-primary-900"
                    >
                      {title}
                    </button>
                  ))}
                </div>
              </TabsPanel>

              <TabsPanel value="text" className="p-1">
                <textarea
                  aria-label="Texto da vaga"
                  placeholder="Cole aqui a descrição da vaga..."
                  value={jobText}
                  onChange={(event) => setJobText(event.target.value)}
                  className="min-h-29.75 w-full resize-y rounded-lg bg-surface-container-low p-4 text-[15px] text-on-surface shadow-xs outline-none placeholder:text-on-surface-variant/60 focus:ring-2 focus:ring-primary-300"
                />
              </TabsPanel>

              <TabsPanel value="cv" className="p-1">
                <label className="flex min-h-29.75 cursor-pointer flex-col items-center justify-center gap-2 rounded-lg border border-dashed border-outline-variant bg-surface-container-low p-4 text-center text-sm text-on-surface-variant hover:bg-surface-container focus-within:ring-2 focus-within:ring-primary-300">
                  <HugeiconsIcon
                    icon={FileUploadIcon}
                    className="size-5 text-secondary-700"
                  />
                  <span>
                    {cvName || "Selecione um currículo base em PDF ou DOCX"}
                  </span>
                  <input
                    type="file"
                    accept=".pdf,.doc,.docx"
                    className="sr-only"
                    onChange={(event) =>
                      setCvName(event.target.files?.[0]?.name ?? "")
                    }
                  />
                </label>
              </TabsPanel>
            </TabsPanels>
          </div>
        </Tabs>
      </div>

      <div className="flex min-h-26 flex-col gap-4 bg-surface-container-low/70 px-5 py-4 sm:px-10 xl:flex-row xl:items-center xl:justify-between">
        <div className="flex flex-wrap items-center gap-2 text-xs tracking-[0.3px] text-on-surface-variant">
          <HugeiconsIcon
            icon={CheckmarkBadge01Icon}
            className="size-4.5 shrink-0 text-secondary-700"
          />
          <span className="font-semibold leading-4">
            Fonte
            <br />
            Primária:
          </span>
          <span className="rounded bg-surface-container px-2 py-0.5 font-semibold leading-4 text-primary-950">
            Currículo Base Ativo
            <br />
            (Ground Truth)
          </span>
          <span aria-hidden="true">•</span>
          <span className="text-[13px] leading-5">
            Second Brain:
            <br />
            Opcional
          </span>
        </div>
        <Link
          to="/generator"
          className="inline-flex min-h-18 w-full items-center justify-center gap-2 rounded-lg bg-primary-900 px-6 py-3 text-center text-base font-semibold leading-6 text-primary-foreground shadow-[0_4px_6px_rgb(62_39_35/15%)] transition-colors hover:bg-primary-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-900 xl:w-77.5 xl:shrink-0"
        >
          <span>
            Gerar Currículo Sob Medida
            <br />
            (Baseado no CV Base)
          </span>
          <HugeiconsIcon
            icon={ArrowRight01Icon}
            className="size-3.5 shrink-0"
          />
        </Link>
      </div>
    </article>
  );
}
