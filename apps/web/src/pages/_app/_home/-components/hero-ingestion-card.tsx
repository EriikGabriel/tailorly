import {
  Tabs,
  TabsList,
  TabsPanel,
  TabsPanels,
  TabsTab,
} from "@animate/base/tabs";
import { RippleButton } from "@components/ui/animate/buttons/ripple";
import {
  ArrowRight01Icon,
  BoltIcon,
  Briefcase01Icon,
  ClipboardPasteIcon,
  FileTextIcon,
  Link01Icon,
  Search01Icon,
  ShieldCheckIcon,
} from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { useEffect, useState } from "react";
import { BaseCvPicker } from "./base-cv-picker";
import { ValidateDialog } from "./validate-dialog";

type InputMode = "url" | "text";

const inputModes = [
  { value: "url", label: "Inserir URL da Vaga", icon: Link01Icon },
  { value: "text", label: "Colar Texto da Vaga", icon: FileTextIcon },
] as const;

const quickTests = [
  "Senior Tech Lead @ Nubank",
  "Staff Engineer @ Inter",
] as const;

export function HeroIngestionCard() {
  const [mode, setMode] = useState<InputMode>("url");
  const [jobUrl, setJobUrl] = useState("");
  const [jobText, setJobText] = useState("");
  const [baseCv, setBaseCv] = useState<File | null>(null);
  const [reviewOpen, setReviewOpen] = useState(false);

  useEffect(() => {
    const saved = sessionStorage.getItem("tailorly:job-draft");
    if (!saved) return;
    try {
      const draft = JSON.parse(saved) as {
        mode?: InputMode;
        jobUrl?: string;
        jobText?: string;
      };
      if (draft.mode === "url" || draft.mode === "text") setMode(draft.mode);
      if (typeof draft.jobUrl === "string") setJobUrl(draft.jobUrl);
      if (typeof draft.jobText === "string") setJobText(draft.jobText);
    } catch {
      sessionStorage.removeItem("tailorly:job-draft");
    }
  }, []);

  const hasJob =
    mode === "url" ? jobUrl.trim().length > 0 : jobText.trim().length > 0;

  function saveDraft() {
    sessionStorage.setItem(
      "tailorly:job-draft",
      JSON.stringify({ mode, jobUrl, jobText }),
    );
  }

  function openReview() {
    if (!hasJob || !baseCv) return;
    saveDraft();
    setReviewOpen(true);
  }

  async function pasteUrl() {
    try {
      const text = await navigator.clipboard.readText();
      setJobUrl(text.trim());
    } catch {
      // The URL field remains available when clipboard permission is denied.
    }
  }

  return (
    <article className="w-full overflow-hidden rounded-xl border border-primary-200 bg-card shadow-[0_12px_16px_rgb(62_39_35/8%),0_2px_3px_rgb(62_39_35/4%)]">
      <div className="px-5 pt-5 sm:px-7">
        <div className="p">
          <div className="flex flex-col flex-wrap items-start justify-between">
            <h2
              id="base-cv-heading"
              className="flex items-center gap-2 text-sm font-semibold text-primary-950"
            >
              <HugeiconsIcon
                icon={ShieldCheckIcon}
                className="size-4.5 text-secondary-700"
              />
              Currículo base (Ground Truth)
            </h2>
            <p className="mt-1 mb-2 text-xs leading-5 text-on-surface-variant">
              Adicione o currículo base ser usado como ground truth.
            </p>
          </div>

          <BaseCvPicker file={baseCv} onFileChange={setBaseCv} />
        </div>
        <div className="mt-4 border-t border-outline-variant/30 pt-4">
          <h2 className="flex items-center gap-2 text-sm font-semibold text-primary-950">
            <HugeiconsIcon
              icon={Briefcase01Icon}
              className="size-4.5 text-secondary-700"
            />
            Vaga desejada
          </h2>
          <p className="mt-1 text-xs leading-5 text-on-surface-variant">
            Adicione o link ou a descrição da vaga.
          </p>
        </div>

        <Tabs
          value={mode}
          onValueChange={(value) => setMode(value as InputMode)}
          className="gap-0"
        >
          <TabsList
            aria-label="Forma de informar a vaga"
            className="mt-2 h-auto min-h-12 max-w-full flex-wrap gap-1 bg-surface-container p-1 px-1.5 **:data-[slot=motion-highlight]:border-0 **:data-[slot=motion-highlight]:bg-card"
          >
            {inputModes.map(({ value, label, icon }) => (
              <TabsTab
                key={value}
                value={value}
                type="button"
                className="h-9 gap-1 px-3 py-1.5 text-sm tracking-[0.14px] text-on-surface-variant data-selected:font-semibold data-selected:text-primary-900"
              >
                <HugeiconsIcon icon={icon} className="size-3.75" />
                {label}
              </TabsTab>
            ))}
          </TabsList>

          <div className="mt-3 pb-4">
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
                <div className="mt-2 flex flex-wrap items-center gap-1 text-xs tracking-[0.3px]">
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
            </TabsPanels>
          </div>
        </Tabs>
      </div>

      <div className="flex flex-col gap-2 bg-surface-container-low/70 px-5 py-3 sm:px-7">
        <RippleButton
          type="button"
          onClick={openReview}
          disabled={!hasJob || !baseCv}
          hoverScale={1.03}
          className="inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-lg bg-primary-900 px-4 py-2 text-center text-sm font-semibold text-primary-foreground shadow-[0_4px_6px_rgb(62_39_35/15%)] transition-colors hover:bg-primary-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-900 disabled:cursor-not-allowed disabled:opacity-50"
        >
          <span>Gerar currículo sob medida</span>
          <HugeiconsIcon
            icon={ArrowRight01Icon}
            className="size-3.5 shrink-0"
          />
        </RippleButton>
      </div>

      <ValidateDialog
        open={reviewOpen}
        onOpenChange={setReviewOpen}
        onSaveDraft={saveDraft}
        file={baseCv}
      />
    </article>
  );
}
