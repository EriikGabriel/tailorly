import { Button } from "@animate/buttons/button";
import {
  Highlight,
  HighlightItem,
} from "@animate/primitives/effects/highlight";
import tailorlyLogo from "@assets/tailorly-logo.svg";
import { Bell, UserRound } from "@react-zero-ui/icon-sprite";
import { Link, useLocation } from "@tanstack/react-router";
import { useReducedMotion } from "motion/react";

const navigation = [
  { label: "Início", to: "/" },
  { label: "Currículo Base", to: "/base" },
  { label: "Gerador", to: "/generator" },
  { label: "Meus Currículos", to: "/cvs" },
] as const;

type HeaderProps = {
  onNotificationsClick?: () => void;
  onProfileClick?: () => void;
};

export function Header({ onNotificationsClick, onProfileClick }: HeaderProps) {
  const activePath = useLocation({ select: (location) => location.pathname });
  const reduceMotion = useReducedMotion();

  return (
    <header className="relative z-10 w-full bg-surface/90 shadow-[0_1px_8px_rgb(62_39_35/6%)] backdrop-blur-md">
      <div className="flex min-h-16 items-center gap-4 px-gutter">
        <Link
          className="flex shrink-0 items-center gap-2 text-primary no-underline"
          to="/"
          aria-label="Tailorly, início"
        >
          <img
            alt=""
            className="size-8 shrink-0"
            height="64"
            width="64"
            src={tailorlyLogo}
          />
          <span className="text-2xl font-bold">
            Tailor<span className="text-secondary">ly</span>
          </span>
        </Link>

        <nav
          aria-label="Navegação principal"
          className="flex flex-1 justify-center"
        >
          <Highlight
            mode="parent"
            controlledItems
            click={false}
            enabled={!reduceMotion}
            value={activePath}
            className="rounded-md bg-primary shadow-xs"
            transition={{ type: "spring", stiffness: 380, damping: 32 }}
          >
            <ul className="flex items-center gap-3">
              {navigation.map(({ label, to }) => {
                const active = activePath === to;
                return (
                  <HighlightItem as="li" key={to} value={to}>
                    <Link
                      aria-current={active ? "page" : undefined}
                      className={`inline-flex h-9 items-center justify-center whitespace-nowrap rounded-md px-4 text-sm font-medium outline-none transition-colors focus-visible:ring-3 focus-visible:ring-ring/50 ${
                        active
                          ? "text-primary-foreground"
                          : "text-foreground hover:bg-accent hover:text-accent-foreground"
                      } ${reduceMotion && active ? "bg-primary" : ""}`}
                      preload="intent"
                      to={to}
                    >
                      {label}
                    </Link>
                  </HighlightItem>
                );
              })}
            </ul>
          </Highlight>
        </nav>

        <div className="flex shrink-0 items-center gap-4">
          <Button
            type="button"
            variant="ghost"
            aria-label="Notificações"
            onClick={onNotificationsClick}
          >
            <Bell className="size-5" />
          </Button>
          <Button
            aria-label="Perfil"
            type="button"
            className="size-10 rounded-xl"
            onClick={onProfileClick}
          >
            <UserRound className="size-6" />
          </Button>
        </div>
      </div>
    </header>
  );
}
