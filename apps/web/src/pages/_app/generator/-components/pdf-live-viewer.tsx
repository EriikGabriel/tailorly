import type { PDFDocumentProxy, RenderTask } from "pdfjs-dist";
import { useEffect, useRef, useState } from "react";
import { pdfjs } from "react-pdf";

pdfjs.GlobalWorkerOptions.workerSrc = new URL(
  "pdfjs-dist/build/pdf.worker.min.mjs",
  import.meta.url,
).toString();

/** `fit` ajusta a página à largura disponível; um número é a porcentagem do tamanho real da página. */
export type PreviewZoom = "fit" | number;

export interface ViewerMetrics {
  page: number;
  pages: number;
  /** Escala efetiva em porcentagem do tamanho real, também no modo `fit`. */
  percent: number;
}

const PAGE_GAP = 14;
const PADDING = 20;
/** Pontos do PDF por pixel CSS: 1 pt = 4/3 px a 96 dpi. */
const POINT_TO_PIXEL = 4 / 3;

/**
 * Mostra o PDF da prévia em canvas e o atualiza no lugar. Quando chega um PDF novo, as páginas são
 * desenhadas fora da tela e trocadas de uma vez, então não há tela em branco nem recarga do
 * visualizador, e a posição de rolagem é mantida. A mesma troca vale para mudanças de zoom e de largura.
 */
export function PdfLiveViewer({
  url,
  zoom,
  onMetrics,
}: {
  url: string | null;
  zoom: PreviewZoom;
  onMetrics?: (metrics: ViewerMetrics) => void;
}) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const pagesRef = useRef<HTMLDivElement>(null);
  const docRef = useRef<PDFDocumentProxy | null>(null);
  const lastLayoutRef = useRef<string | null>(null);
  const swapsRef = useRef(0);
  const metricsRef = useRef(onMetrics);
  metricsRef.current = onMetrics;

  const [width, setWidth] = useState(0);
  const [docVersion, setDocVersion] = useState(0);
  const [failed, setFailed] = useState(false);

  // Largura disponível: o modo "ajustar" acompanha o redimensionamento do painel.
  useEffect(() => {
    const element = scrollRef.current;
    if (!element) return;
    let frame = 0;
    const observer = new ResizeObserver((entries) => {
      cancelAnimationFrame(frame);
      frame = requestAnimationFrame(() =>
        setWidth(entries[0].contentRect.width),
      );
    });
    observer.observe(element);
    return () => {
      cancelAnimationFrame(frame);
      observer.disconnect();
    };
  }, []);

  // Carrega cada PDF novo sem tocar no que já está na tela.
  useEffect(() => {
    if (!url) return;
    let cancelled = false;
    let loaded = false;
    const task = pdfjs.getDocument({ url });
    task.promise.then(
      (doc) => {
        loaded = true;
        if (cancelled) {
          void doc.loadingTask.destroy();
          return;
        }
        const previous = docRef.current;
        docRef.current = doc;
        setFailed(false);
        setDocVersion((version) => version + 1);
        void previous?.loadingTask.destroy();
      },
      () => {
        if (!cancelled) setFailed(true);
      },
    );
    return () => {
      cancelled = true;
      if (!loaded) void task.destroy();
    };
  }, [url]);

  useEffect(
    () => () => {
      void docRef.current?.loadingTask.destroy();
      docRef.current = null;
    },
    [],
  );

  // Desenha todas as páginas fora da tela e troca de uma vez.
  // biome-ignore lint/correctness/useExhaustiveDependencies: `docVersion` sinaliza um documento novo em `docRef`
  useEffect(() => {
    const doc = docRef.current;
    const host = pagesRef.current;
    const scroller = scrollRef.current;
    if (!doc || !host || !scroller || width <= 0) return;

    let cancelled = false;
    const tasks: RenderTask[] = [];
    const ratio = window.devicePixelRatio || 1;

    (async () => {
      const wrappers: HTMLElement[] = [];
      let percent = 100;
      for (let number = 1; number <= doc.numPages; number++) {
        const page = await doc.getPage(number);
        if (cancelled) return;
        const natural = page.getViewport({ scale: 1 });
        const naturalWidth = natural.width * POINT_TO_PIXEL;
        const cssWidth =
          zoom === "fit"
            ? Math.max(220, width - PADDING * 2)
            : (naturalWidth * zoom) / 100;
        const scale = cssWidth / natural.width;
        percent = Math.round((cssWidth / naturalWidth) * 100);

        const cssViewport = page.getViewport({ scale });
        const canvas = document.createElement("canvas");
        canvas.width = Math.floor(cssViewport.width * ratio);
        canvas.height = Math.floor(cssViewport.height * ratio);
        canvas.style.width = `${Math.floor(cssViewport.width)}px`;
        canvas.style.height = `${Math.floor(cssViewport.height)}px`;
        canvas.setAttribute("role", "img");
        canvas.setAttribute(
          "aria-label",
          `Página ${number} de ${doc.numPages}`,
        );

        const task = page.render({
          canvas,
          viewport: page.getViewport({ scale: scale * ratio }),
        });
        tasks.push(task);
        await task.promise;
        if (cancelled) return;

        const wrapper = document.createElement("div");
        wrapper.dataset.page = String(number);
        wrapper.className = "bg-white shadow-md";
        wrapper.style.width = canvas.style.width;
        wrapper.style.height = canvas.style.height;
        wrapper.append(canvas);
        wrappers.push(wrapper);
      }

      const layout = `${width}|${zoom}`;
      const layoutChanged =
        lastLayoutRef.current !== null && lastLayoutRef.current !== layout;
      const before = scroller.scrollHeight;
      const fraction = before > 0 ? scroller.scrollTop / before : 0;
      host.replaceChildren(...wrappers);
      host.dataset.swaps = String(++swapsRef.current);
      lastLayoutRef.current = layout;
      // Zoom e largura mudam a altura total: a rolagem acompanha a mesma parte do documento. Um PDF
      // novo com o mesmo layout não mexe na rolagem.
      if (layoutChanged) scroller.scrollTop = fraction * scroller.scrollHeight;
      report(scroller, host, percent);
    })().catch(() => {
      // Renderização cancelada por uma troca mais nova: nada a mostrar.
    });

    return () => {
      cancelled = true;
      for (const task of tasks) task.cancel();
    };
  }, [docVersion, width, zoom]);

  function report(scroller: HTMLElement, host: HTMLElement, percent?: number) {
    const pages = [...host.children] as HTMLElement[];
    if (pages.length === 0) return;
    const probe = scroller.scrollTop + scroller.clientHeight / 3;
    let current = 1;
    for (const page of pages) {
      if (page.offsetTop <= probe) current = Number(page.dataset.page);
    }
    metricsRef.current?.({
      page: current,
      pages: pages.length,
      percent: percent ?? Number(host.dataset.percent ?? 100),
    });
    if (percent !== undefined) host.dataset.percent = String(percent);
  }

  function onScroll() {
    const scroller = scrollRef.current;
    const host = pagesRef.current;
    if (scroller && host) report(scroller, host);
  }

  return (
    <div
      ref={scrollRef}
      onScroll={onScroll}
      className="relative min-h-0 flex-1 overflow-auto bg-surface-container"
    >
      <div
        ref={pagesRef}
        data-testid="pdf-pages"
        className="mx-auto flex w-max min-w-full flex-col items-center"
        style={{ gap: PAGE_GAP, padding: PADDING }}
      />
      {failed && (
        <p
          role="alert"
          className="absolute inset-x-0 top-6 text-center text-sm text-error"
        >
          Não foi possível exibir o PDF da prévia.
        </p>
      )}
    </div>
  );
}
