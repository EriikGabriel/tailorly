import {
  Highlight,
  HighlightItem,
} from "@animate/primitives/effects/highlight";
import {
  Cpu,
  Network,
  ShieldCheck,
  UserRound,
} from "@react-zero-ui/icon-sprite";
import { useReducedMotion } from "motion/react";
import { type ComponentType, useEffect, useState } from "react";

type Tab = {
  id: string;
  label: string;
  icon: ComponentType<{ className?: string; "aria-hidden"?: boolean }>;
  dot?: true;
  badge?: string;
  sectionId?: string;
};

const tabs: Tab[] = [
  {
    id: "overview",
    label: "Visão Geral & Perfil",
    icon: UserRound,
    sectionId: "section-overview",
  },
  {
    id: "ground-truth",
    label: "Currículo Base (Ground Truth)",
    icon: ShieldCheck,
    dot: true,
    sectionId: "section-ground-truth",
  },
  {
    id: "connections",
    label: "Conexões & Second Brain",
    icon: Network,
    badge: "2 Conectados",
    sectionId: "section-connections",
  },

  {
    id: "ai-preferences",
    label: "Preferências de IA & Provedores de LLM",
    icon: Cpu,
    sectionId: "section-ai-preferences",
  },
];

export function ProfileTabs() {
  const [active, setActive] = useState("overview");
  const reduceMotion = useReducedMotion();

  useEffect(() => {
    const sectionMap = new Map(
      tabs
        .filter((t): t is Tab & { sectionId: string } => t.sectionId != null)
        .map((t) => [t.sectionId, t.id]),
    );

    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            const tabId = sectionMap.get(entry.target.id);
            if (tabId) setActive(tabId);
          }
        }
      },
      { rootMargin: "-40% 0px -55% 0px" },
    );

    for (const [sectionId] of sectionMap) {
      const el = document.getElementById(sectionId);
      if (el) observer.observe(el);
    }

    return () => observer.disconnect();
  }, []);

  function handleTabClick(tab: Tab) {
    setActive(tab.id);
    if (tab.sectionId) {
      document.getElementById(tab.sectionId)?.scrollIntoView({
        behavior: reduceMotion ? "auto" : "smooth",
        block: "start",
      });
    }
  }

  return (
    <div
      role="tablist"
      aria-label="Seções do perfil profissional"
      className="sticky top-2 border z-20 flex mx-auto overflow-x-auto rounded-xl bg-surface-container-low/90 p-1.5 shadow-sm backdrop-blur-md"
    >
      <Highlight
        mode="parent"
        controlledItems
        click={false}
        enabled={!reduceMotion}
        value={active}
        className="rounded-lg bg-surface-container-lowest shadow-xs"
        transition={{ type: "spring", stiffness: 380, damping: 32 }}
      >
        <div className="flex w-max items-center gap-1">
          {tabs.map((tab) => {
            const { id, label, icon: Icon } = tab;
            const isActive = active === id;
            return (
              <HighlightItem as="div" key={id} value={id}>
                <button
                  type="button"
                  role="tab"
                  aria-selected={isActive}
                  onClick={() => handleTabClick(tab)}
                  className={`inline-flex items-center gap-2 whitespace-nowrap rounded-lg px-4 py-2 text-label-md outline-none transition-colors focus-visible:ring-3 focus-visible:ring-ring/50 ${
                    isActive
                      ? "font-medium text-primary-950"
                      : "text-on-surface-variant hover:text-primary-950"
                  }`}
                >
                  <Icon aria-hidden className="size-4 shrink-0" />
                  {label}
                  {tab.dot && (
                    <span
                      aria-hidden="true"
                      className="size-2 shrink-0 rounded-full bg-on-secondary-container"
                    />
                  )}
                  {tab.badge && (
                    <span className="shrink-0 rounded-full bg-surface-container-high px-1.5 py-0.5 text-label-sm text-on-surface-variant">
                      {tab.badge}
                    </span>
                  )}
                </button>
              </HighlightItem>
            );
          })}
        </div>
      </Highlight>
    </div>
  );
}
