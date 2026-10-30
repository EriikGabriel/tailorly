import { CountingNumber } from "@animate/primitives/texts/counting-number";

const RADIUS = 26;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

export function FactualFidelityGauge({ value }: { value: number }) {
  const offset = CIRCUMFERENCE * (1 - value / 100);

  return (
    // biome-ignore lint/a11y/useSemanticElements: native <meter> can't render the overlaid SVG ring + label.
    <div
      className="relative flex size-16 shrink-0 items-center justify-center"
      role="meter"
      aria-valuenow={value}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-label="Taxa de fidelidade factual"
    >
      <svg
        aria-hidden="true"
        viewBox="0 0 64 64"
        className="size-16 -rotate-90"
      >
        <circle
          cx={32}
          cy={32}
          r={RADIUS}
          strokeWidth={6}
          className="fill-none stroke-surface-container-highest"
        />
        <circle
          cx={32}
          cy={32}
          r={RADIUS}
          strokeWidth={6}
          strokeLinecap="round"
          className="fill-none stroke-on-secondary-container transition-[stroke-dashoffset] duration-700 ease-out"
          strokeDasharray={CIRCUMFERENCE}
          strokeDashoffset={offset}
        />
      </svg>
      <span className="absolute text-label-md font-bold text-primary-950">
        <CountingNumber number={value} inView inViewOnce />%
      </span>
    </div>
  );
}
