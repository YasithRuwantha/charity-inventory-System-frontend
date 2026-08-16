export function AppFooter() {
  const year = new Date().getFullYear();
  return (
    <footer className="shrink-0 border-t border-ink-200/80 px-4 py-3 sm:px-6 md:px-7">
      <div className="mx-auto flex max-w-[1400px] flex-wrap items-center justify-between gap-2 text-xs text-ink-500">
        <p>
          Charity Inventory Management System · © {year}
        </p>
        <div className="flex items-center gap-4">
          <span className="hover:text-ink-700">Privacy</span>
          <span className="hover:text-ink-700">Contact</span>
        </div>
      </div>
    </footer>
  );
}
