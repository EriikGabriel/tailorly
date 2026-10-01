import { createFileRoute } from "@tanstack/react-router";
import { HeroSection } from "./-sections/hero-section";
import { RecentCVSection } from "./-sections/recent-cv-section";
import { SecondBrainSection } from "./-sections/second-brain-section";
import { StepsSection } from "./-sections/steps-section";

export const Route = createFileRoute("/_app/_home/")({
  component: App,
});

function App() {
  return (
    <main className="relative isolate flex min-h-[calc(100dvh-4rem)] flex-col gap-12 items-center justify-center overflow-hidden px-4 py-6 sm:px-8 lg:px-16">
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -top-32 left-1/2 h-85 w-210 -translate-x-1/2 bg-linear-to-b from-secondary-container/25 via-surface-container-low/40 to-surface-container-low/0 blur-[32px]"
      />

      <HeroSection />
      <StepsSection />
      <RecentCVSection />
      <SecondBrainSection />
    </main>
  );
}
