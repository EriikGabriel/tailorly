import { motion, useReducedMotion } from "motion/react";

/** Barra de progresso que preenche com animação; sem movimento quando o sistema pede para reduzir. */
export function AnimatedBar({
  value,
  max,
  label,
  color,
  className = "h-1.5",
}: {
  value: number;
  max: number;
  label: string;
  /** Cor do preenchimento; sem ela usa a cor principal do app. */
  color?: string;
  className?: string;
}) {
  const reduceMotion = useReducedMotion();
  const percent = max > 0 ? Math.min(100, Math.max(0, (value / max) * 100)) : 0;

  return (
    <div
      role="progressbar"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={max}
      aria-valuenow={value}
      className={`w-full overflow-hidden rounded-full bg-surface-container-high ${className}`}
    >
      <motion.div
        className={`h-full rounded-full ${color ? "" : "bg-primary-900"}`}
        style={color ? { backgroundColor: color } : undefined}
        initial={reduceMotion ? false : { width: 0 }}
        animate={{ width: `${percent}%` }}
        transition={{ type: "spring", stiffness: 120, damping: 24 }}
      />
    </div>
  );
}
