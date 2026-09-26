type PagePlaceholderProps = {
  title: string;
};

export function PagePlaceholder({ title }: PagePlaceholderProps) {
  return (
    <main className="min-h-[calc(100dvh-4rem)] px-gutter py-12">
      <h1 className="text-3xl font-semibold text-foreground">{title}</h1>
      <p className="mt-3 text-on-surface-variant">
        Esta seção está em construção.
      </p>
    </main>
  );
}
