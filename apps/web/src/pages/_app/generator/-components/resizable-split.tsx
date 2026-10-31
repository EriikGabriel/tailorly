import { GripVertical } from "@react-zero-ui/icon-sprite";
import {
  type CSSProperties,
  type KeyboardEvent,
  type PointerEvent,
  type ReactNode,
  useEffect,
  useRef,
  useState,
} from "react";
import {
  DEFAULT_RATIO,
  MAX_RATIO,
  MIN_RATIO,
  parseStoredRatio,
  ratioForKey,
  ratioFromPointer,
} from "../-utils/split-ratio";

function readStored(key: string): number | null {
  try {
    return parseStoredRatio(window.localStorage.getItem(key));
  } catch {
    return null;
  }
}

/**
 * Dois painéis lado a lado com um divisor que a pessoa arrasta (ou move com as setas) para decidir
 * quanto cada um ocupa. Duplo clique restaura 50/50. A proporção fica guardada no navegador — é só
 * estado de interface. Em telas estreitas os painéis ficam empilhados e o divisor some.
 */
export function ResizableSplit({
  storageKey,
  first,
  second,
  separatorLabel,
}: {
  storageKey: string;
  first: ReactNode;
  second: ReactNode;
  separatorLabel: string;
}) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [ratio, setRatio] = useState(
    () => readStored(storageKey) ?? DEFAULT_RATIO,
  );
  const [dragging, setDragging] = useState(false);

  useEffect(() => {
    try {
      window.localStorage.setItem(storageKey, String(ratio));
    } catch {
      // Sem armazenamento disponível, a proporção só vale nesta sessão.
    }
  }, [storageKey, ratio]);

  function onPointerDown(event: PointerEvent<HTMLDivElement>) {
    event.currentTarget.setPointerCapture(event.pointerId);
    setDragging(true);
  }

  function onPointerMove(event: PointerEvent<HTMLDivElement>) {
    if (!dragging || !containerRef.current) return;
    const rect = containerRef.current.getBoundingClientRect();
    setRatio(ratioFromPointer(event.clientX, rect.left, rect.width));
  }

  function onPointerUp(event: PointerEvent<HTMLDivElement>) {
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
    setDragging(false);
  }

  function onKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    const next = ratioForKey(event.key, ratio, event.shiftKey);
    if (next === null) return;
    event.preventDefault();
    setRatio(next);
  }

  return (
    <div
      ref={containerRef}
      data-testid="resizable-split"
      className={`flex h-full min-h-0 w-full flex-col gap-4 lg:flex-row lg:gap-0 ${dragging ? "select-none" : ""}`}
      style={{ "--split": `${ratio * 100}%` } as CSSProperties}
    >
      <div
        data-testid="split-first"
        className={`min-h-0 min-w-0 flex-1 lg:flex-none lg:basis-[calc(var(--split)-0.375rem)] ${dragging ? "pointer-events-none" : ""}`}
      >
        {first}
      </div>

      {/* biome-ignore lint/a11y/useSemanticElements: o separador redimensionável segue o padrão WAI-ARIA "window splitter" */}
      <div
        role="separator"
        tabIndex={0}
        aria-orientation="vertical"
        aria-label={separatorLabel}
        aria-valuemin={Math.round(MIN_RATIO * 100)}
        aria-valuemax={Math.round(MAX_RATIO * 100)}
        aria-valuenow={Math.round(ratio * 100)}
        data-testid="split-handle"
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={onPointerUp}
        onPointerCancel={onPointerUp}
        onDoubleClick={() => setRatio(DEFAULT_RATIO)}
        onKeyDown={onKeyDown}
        className="group hidden w-3 shrink-0 cursor-col-resize touch-none items-center justify-center outline-none lg:flex"
      >
        <span
          className={`flex h-14 w-2 items-center justify-center rounded-full transition-colors group-hover:bg-primary-900 group-focus-visible:bg-primary-900 group-focus-visible:ring-2 group-focus-visible:ring-ring ${
            dragging ? "bg-primary-900" : "bg-outline-variant"
          }`}
        >
          <GripVertical
            className={`size-3 ${dragging ? "text-primary-foreground" : "text-on-surface-variant group-hover:text-primary-foreground"}`}
            aria-hidden="true"
          />
        </span>
      </div>

      <div
        data-testid="split-second"
        className={`min-h-0 min-w-0 flex-1 ${dragging ? "pointer-events-none" : ""}`}
      >
        {second}
      </div>
    </div>
  );
}
