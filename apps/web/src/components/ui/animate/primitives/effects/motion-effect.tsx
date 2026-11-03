"use client";

import {
  type HTMLMotionProps,
  motion,
  type TargetAndTransition,
  type Transition,
  useReducedMotion,
} from "motion/react";
import type * as React from "react";
import { type UseIsInViewOptions, useIsInView } from "@/hooks/use-is-in-view";

type Direction = "up" | "down" | "left" | "right";

type MotionEffectTag = "div" | "section" | "article" | "li" | "ul" | "span";

type MotionEffectProps = Omit<
  HTMLMotionProps<"div">,
  "initial" | "animate" | "transition"
> &
  UseIsInViewOptions & {
    children?: React.ReactNode;
    /** Elemento renderizado; `div` por padrão. */
    as?: MotionEffectTag;
    /** Aparece com opacidade; ligado por padrão. */
    fade?: boolean;
    /** Desliza a partir de uma direção (`offset` em px). */
    slide?: boolean | { direction?: Direction; offset?: number };
    /** Cresce a partir de uma escala menor. */
    zoom?: boolean | { initialScale?: number };
    /** Começa desfocado (`px`). */
    blur?: boolean | number;
    /** Atraso em segundos; use `index * 0.08` para escalonar listas. */
    delay?: number;
    transition?: Transition;
  };

const DEFAULT_TRANSITION: Transition = {
  type: "spring",
  stiffness: 220,
  damping: 28,
  mass: 0.9,
};

function slideOffset(slide: MotionEffectProps["slide"]) {
  if (!slide) return { x: 0, y: 0 };
  const { direction = "up", offset = 24 } = slide === true ? {} : slide;
  switch (direction) {
    case "up":
      return { x: 0, y: offset };
    case "down":
      return { x: 0, y: -offset };
    case "left":
      return { x: offset, y: 0 };
    case "right":
      return { x: -offset, y: 0 };
  }
}

/**
 * Entrada animada de um bloco (estilo `MotionEffect` do Animate UI): fade, slide, zoom e blur
 * combináveis, disparados ao montar ou ao entrar na viewport (`inView`). Com "reduzir movimento"
 * no sistema, o conteúdo aparece sem animar.
 */
function MotionEffect({
  children,
  as = "div",
  fade = true,
  slide = false,
  zoom = false,
  blur = false,
  delay = 0,
  transition,
  inView = false,
  inViewOnce = true,
  inViewMargin = "0px",
  ref,
  ...props
}: MotionEffectProps & { ref?: React.Ref<HTMLElement> }) {
  const reduceMotion = useReducedMotion();
  const { ref: localRef, isInView } = useIsInView(
    ref as React.Ref<HTMLElement>,
    { inView, inViewOnce, inViewMargin },
  );

  const { x, y } = slideOffset(slide);
  const initialScale = zoom
    ? zoom === true
      ? 0.94
      : (zoom.initialScale ?? 0.94)
    : 1;
  const blurPx = blur ? (blur === true ? 8 : blur) : 0;

  const hidden: TargetAndTransition = {
    opacity: fade ? 0 : 1,
    x,
    y,
    scale: initialScale,
    // `filter` só entra quando há blur: um filtro ativo num ancestral prende elementos fixos.
    ...(blurPx > 0 && { filter: `blur(${blurPx}px)` }),
  };
  const shown: TargetAndTransition = {
    opacity: 1,
    x: 0,
    y: 0,
    scale: 1,
    ...(blurPx > 0 && { filter: "blur(0px)" }),
  };

  const Component = motion[as] as typeof motion.div;

  if (reduceMotion) {
    return (
      <Component ref={localRef as React.Ref<HTMLDivElement>} {...props}>
        {children}
      </Component>
    );
  }

  return (
    <Component
      ref={localRef as React.Ref<HTMLDivElement>}
      initial={hidden}
      animate={isInView ? shown : hidden}
      transition={{ ...(transition ?? DEFAULT_TRANSITION), delay }}
      {...props}
    >
      {children}
    </Component>
  );
}

export { MotionEffect, type MotionEffectProps };
