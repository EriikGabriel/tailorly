import { Button } from "@animate/buttons/button";
import tailorlyLogo from "@assets/tailorly-logo.svg";
import { Notification01Icon, UserIcon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { cn } from "@lib/utils";

type NavigationItem = {
  label: string;
  href?: string;
  icon?: string;
};

const navigation = [
  { label: "Início", href: "/" },
  { label: "Gerador", href: "/gerador" },
  { label: "Meus Currículos", href: "/meus-curriculos" },
  { label: "Configurações", href: "/configuracoes" },
] as NavigationItem[];

type HeaderProps = {
  activeHref?: string;
  onNotificationsClick?: () => void;
  onProfileClick?: () => void;
};

import { useLocation } from "@tanstack/react-router";

export function Header({ onNotificationsClick, onProfileClick }: HeaderProps) {
  const location = useLocation();
  const activeHref = location.pathname;

  return (
    <header className="relative z-10 w-full bg-surface/90 shadow-[0_1px_8px_rgb(62_39_35/6%)] backdrop-blur-md">
      <div className="flex min-h-16 items-center gap-4 px-gutter">
        <a
          className="flex shrink-0 items-center gap-2 text-primary no-underline"
          href="/"
          aria-label="Tailorly, início"
        >
          <img
            alt=""
            className="size-8 shrink-0"
            height="64"
            width="64"
            src={tailorlyLogo}
          />
          <h1 className="text-2xl font-bold">
            Tailor<span className="text-secondary">ly</span>
          </h1>
        </a>

        <nav
          aria-label="Navegação principal"
          className="flex-1 justify-center flex"
        >
          <ul className="flex items-center gap-3">
            {navigation.map(({ label, href }) => (
              <li key={label}>
                <Button
                  aria-current={activeHref === href ? "page" : undefined}
                  className={cn("transition-all duration-300 ease-in-out")}
                  variant={activeHref === href ? "default" : "ghost"}
                  onClick={() => {
                    if (href) window.location.href = href;
                  }}
                >
                  {label}
                </Button>
              </li>
            ))}
          </ul>
        </nav>

        <div className="flex shrink-0 items-center gap-4">
          <Button
            type="button"
            variant="ghost"
            aria-label="Notificações"
            onClick={onNotificationsClick}
          >
            <HugeiconsIcon icon={Notification01Icon} className="size-5 " />
          </Button>
          <Button
            aria-label="Perfil"
            type="button"
            className="rounded-xl size-10"
            onClick={onProfileClick}
          >
            <HugeiconsIcon icon={UserIcon} className="size-6" />
          </Button>
        </div>
      </div>
    </header>
  );
}
