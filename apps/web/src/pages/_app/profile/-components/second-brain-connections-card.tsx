import { Gem, Network, Notebook } from "@react-zero-ui/icon-sprite";
import { previewSecondBrainConnections } from "@/data/profile-preview";

export function SecondBrainConnectionsCard() {
  const { notion, obsidian } = previewSecondBrainConnections;

  return (
    <div className="flex w-full flex-col gap-4 rounded-xl bg-card p-6 shadow-xs">
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-surface-container text-on-surface-variant">
            <Network aria-hidden="true" className="size-4" />
          </span>
          <h2 className="text-title-md font-semibold text-primary-950">
            Second Brain (Opcional)
          </h2>
        </div>
        <span className="text-label-sm font-medium text-on-secondary-container">
          2 Conectados
        </span>
      </div>

      <div className="flex flex-col gap-2">
        <ConnectionRow
          icon={Notebook}
          name={notion.name}
          detail={notion.detail}
        />
        <ConnectionRow
          icon={Gem}
          name={obsidian.name}
          detail={obsidian.detail}
        />
      </div>

      <p className="text-body-sm text-on-surface-variant">
        O Tailorly opera perfeitamente sem o Second Brain. Quando conectado, ele
        apenas arquiva relatórios de tailoring e puxa anotações adicionais de
        projetos.
      </p>
    </div>
  );
}

function ConnectionRow({
  icon: Icon,
  name,
  detail,
}: {
  icon: typeof Notebook;
  name: string;
  detail: string;
}) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-lg bg-surface-container-low p-3">
      <div className="flex min-w-0 items-center gap-3">
        <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-card">
          <Icon aria-hidden="true" className="size-4 text-on-surface-variant" />
        </span>
        <div className="min-w-0">
          <p className="truncate text-label-md font-semibold text-primary-950">
            {name}
          </p>
          <p className="truncate text-label-sm text-on-surface-variant">
            {detail}
          </p>
        </div>
      </div>
      <span className="inline-flex shrink-0 items-center gap-1 text-label-sm font-medium text-on-secondary-container">
        <span
          aria-hidden="true"
          className="size-2 rounded-full bg-on-secondary-container"
        />
        Ativo
      </span>
    </div>
  );
}
