import { ExecutiveProfileCard } from "./executive-profile-card";
import { GroundTruthIntegrityCard } from "./ground-truth-integrity-card";
import { SecondBrainConnectionsCard } from "./second-brain-connections-card";

export function ProfileSidebar() {
  return (
    <aside className="flex flex-col gap-6 lg:col-span-4">
      <ExecutiveProfileCard />
      <GroundTruthIntegrityCard />
      <SecondBrainConnectionsCard />
    </aside>
  );
}
