const LIST_CLASS =
  "flex flex-col gap-1 pl-5 text-[13px] leading-5 text-on-surface";

/** Lista de textos da vaga, com marcadores ou numerada (etapas do processo seletivo). */
export function BulletList({
  items,
  ordered = false,
}: {
  items: readonly string[];
  ordered?: boolean;
}) {
  const Tag = ordered ? "ol" : "ul";
  return (
    <Tag className={`${ordered ? "list-decimal" : "list-disc"} ${LIST_CLASS}`}>
      {items.map((item) => (
        <li key={item}>{item}</li>
      ))}
    </Tag>
  );
}
