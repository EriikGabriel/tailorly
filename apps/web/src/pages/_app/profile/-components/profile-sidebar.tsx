import { MotionEffect } from "@animate/primitives/effects/motion-effect";
import { ExecutiveProfileCard } from "./executive-profile-card";
import { GroundTruthIntegrityCard } from "./ground-truth-integrity-card";
import { SecondBrainConnectionsCard } from "./second-brain-connections-card";

export function ProfileSidebar() {
  return (
    <aside className="flex flex-col gap-6 lg:col-span-4">
      <MotionEffect
        className="empty:hidden"
        slide={{ direction: "right", offset: 20 }}
        delay={0.0}
      >
        <ExecutiveProfileCard />
      </MotionEffect>
      <MotionEffect
        className="empty:hidden"
        slide={{ direction: "right", offset: 20 }}
        delay={0.1}
      >
        <GroundTruthIntegrityCard />
      </MotionEffect>
      <MotionEffect
        className="empty:hidden"
        slide={{ direction: "right", offset: 20 }}
        delay={0.2}
      >
        <SecondBrainConnectionsCard />
      </MotionEffect>
    </aside>
  );
}
