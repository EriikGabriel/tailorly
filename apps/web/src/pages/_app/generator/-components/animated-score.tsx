import { animate, useReducedMotion } from "motion/react";
import { useEffect, useState } from "react";

/** Número que sobe até o valor final; mostra o valor direto quando o sistema pede menos movimento. */
export function AnimatedScore({ value }: { value: number }) {
  const reduceMotion = useReducedMotion();
  const [shown, setShown] = useState(reduceMotion ? value : 0);

  useEffect(() => {
    if (reduceMotion) {
      setShown(value);
      return;
    }
    const controls = animate(0, value, {
      duration: 0.9,
      ease: "easeOut",
      onUpdate: (latest) => setShown(Math.round(latest)),
    });
    return () => controls.stop();
  }, [value, reduceMotion]);

  return <>{shown}</>;
}
