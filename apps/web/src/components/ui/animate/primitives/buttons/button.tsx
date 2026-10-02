"use client";

import { Slot, type WithAsChild } from "@animate/primitives/animate/slot";
import { type HTMLMotionProps, motion, useReducedMotion } from "motion/react";

type ButtonProps = WithAsChild<
  HTMLMotionProps<"button"> & {
    hoverScale?: number;
    tapScale?: number;
  }
>;

function Button({
  hoverScale = 1.025,
  tapScale = 0.98,
  asChild = false,
  ...props
}: ButtonProps) {
  const reducedMotion = useReducedMotion();
  const Component = asChild ? Slot : motion.button;

  return (
    <Component
      whileHover={{ scale: reducedMotion ? 1 : hoverScale }}
      whileTap={{ scale: reducedMotion ? 1 : tapScale }}
      {...props}
    />
  );
}

export { Button, type ButtonProps };
