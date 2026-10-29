import {
  Tabs,
  TabsList,
  TabsPanel,
  TabsPanels,
  TabsTab,
} from "@animate/base/tabs";
import {
  JobUrlFeedback,
  useSettledUrlCheck,
} from "@components/job-url-feedback";
import {
  ClipboardPaste,
  FileText,
  Link as LinkIcon,
  Sparkles,
} from "@react-zero-ui/icon-sprite";
import { type JobInputMode, useJobDraftStore } from "@stores/job-draft-store";
import { checkJobUrl, MAX_JOB_URL_LENGTH } from "@/utils/job-url";
import { JobAnalysisCard } from "./job-analysis-card";
import { SectionHeading } from "./section-heading";

const inputModes = [
  { value: "url", label: "Inserir URL da Vaga", icon: LinkIcon },
  { value: "text", label: "Colar Texto da Vaga", icon: FileText },
] as const;

const quickTests = [
  "Senior Tech Lead @ Nubank",
  "Staff Software Engineer @ Inter",
] as const;

export function JobTargetCard() {
  const { mode, jobUrl, jobText, setMode, setJobUrl, setJobText } =
    useJobDraftStore();

  const urlCheck = checkJobUrl(jobUrl);
  const shownUrlCheck = useSettledUrlCheck(urlCheck);
  const urlInvalid = shownUrlCheck.status === "invalid";

  async function pasteUrl() {
    try {
      const text = await navigator.clipboard.readText();
      setJobUrl(text.trim());
    } catch {
      // O campo continua disponível quando a permissão de clipboard é negada.
    }
  }

  return (
    <div className="flex flex-col gap-3 border-b border-outline-variant/60 pb-4">
      <SectionHeading
        step={2}
        title="Detalhes da Vaga Alvo"
        trailing="Entrada do anúncio"
      />

      <Tabs
        value={mode}
        onValueChange={(value) => setMode(value as JobInputMode)}
        className="gap-0"
      >
        <TabsList
          aria-label="Forma de informar a vaga"
          className="h-auto min-h-11 w-full gap-1 bg-surface-container p-1 **:data-[slot=motion-highlight]:border-0 **:data-[slot=motion-highlight]:bg-card"
        >
          {inputModes.map(({ value, label, icon: Icon }) => (
            <TabsTab
              key={value}
              value={value}
              type="button"
              className="h-9 gap-1.5 px-3 py-1.5 text-sm tracking-[0.14px] text-on-surface-variant data-selected:font-semibold data-selected:text-on-surface"
            >
              <Icon className="size-3.75" aria-hidden="true" />
              {label}
            </TabsTab>
          ))}
        </TabsList>

        <div className="mt-4">
          <TabsPanels className="-mx-1">
            <TabsPanel value="url" className="flex flex-col gap-1.5 p-1">
              <div className="flex items-center justify-between">
                <label
                  htmlFor="generator-job-url"
                  className="text-xs font-medium tracking-[0.3px] text-on-surface"
                >
                  Link da Vaga
                </label>
                <span className="text-xs font-medium tracking-[0.3px] text-on-surface-variant">
                  Gupy, Lever, Greenhouse
                </span>
              </div>
              <div className="relative flex items-center">
                <LinkIcon
                  aria-hidden="true"
                  className="pointer-events-none absolute left-3.5 size-4 text-on-surface-variant"
                />
                <input
                  id="generator-job-url"
                  type="url"
                  placeholder="https://linkedin.com/jobs/view/..."
                  value={jobUrl}
                  maxLength={MAX_JOB_URL_LENGTH + 1}
                  autoComplete="off"
                  spellCheck={false}
                  aria-invalid={urlInvalid}
                  aria-describedby="generator-job-url-feedback"
                  onChange={(event) => setJobUrl(event.target.value)}
                  className={`min-h-11 w-full rounded-lg bg-surface-container-low pl-10 pr-20 text-[15px] text-on-surface shadow-xs outline-none placeholder:text-on-surface-variant/60 focus:ring-2 ${
                    urlInvalid
                      ? "ring-2 ring-error focus:ring-error"
                      : "focus:ring-primary-300"
                  }`}
                />
                <button
                  type="button"
                  onClick={pasteUrl}
                  className="absolute right-2.5 inline-flex items-center gap-1 rounded-md px-1.5 py-1 text-xs font-semibold text-secondary-700 hover:bg-surface-container focus-visible:outline-2 focus-visible:outline-ring"
                >
                  <ClipboardPaste className="size-3.25" aria-hidden="true" />
                  Colar
                </button>
              </div>
              <JobUrlFeedback
                id="generator-job-url-feedback"
                check={shownUrlCheck}
              />
              <p className="flex items-start gap-1 pt-0.5 text-[13px] leading-5 text-on-surface-variant">
                Lemos a página pública da vaga e separamos requisitos,
                benefícios e dados da empresa. Sites que exigem login, como o
                LinkedIn, precisam do texto colado.
              </p>
            </TabsPanel>

            <TabsPanel value="text" className="p-1">
              <textarea
                aria-label="Texto da vaga"
                placeholder="Cole aqui a descrição completa da vaga..."
                value={jobText}
                maxLength={50000}
                onChange={(event) => setJobText(event.target.value)}
                className="min-h-28 w-full resize-y rounded-lg bg-surface-container-low p-3.5 text-[15px] text-on-surface shadow-xs outline-none placeholder:text-on-surface-variant/60 focus:ring-2 focus:ring-primary-300"
              />
            </TabsPanel>
          </TabsPanels>
        </div>
      </Tabs>

      <JobAnalysisCard />

      <div className="flex flex-col gap-1.5">
        <p className="text-xs font-medium tracking-[0.3px] text-on-surface-variant">
          Testar com exemplos reais:
        </p>
        <div className="flex flex-wrap gap-1">
          {quickTests.map((title) => (
            <button
              key={title}
              type="button"
              onClick={() => {
                setJobText(title);
                setMode("text");
              }}
              className="inline-flex items-center gap-1.5 rounded-full bg-surface-container px-3 py-1.5 text-xs font-semibold text-on-surface-variant shadow-xs hover:bg-surface-container-high focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
            >
              <Sparkles className="size-3" aria-hidden="true" />
              {title}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
