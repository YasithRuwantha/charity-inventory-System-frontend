import { create } from "zustand";

export type ThemeMode = "light" | "dark";

const COOKIE = "themeMode";

function readCookie(): ThemeMode {
  if (typeof document === "undefined") return "light";
  const match = document.cookie.match(/(?:^|; )themeMode=(light|dark)/);
  return match?.[1] === "dark" ? "dark" : "light";
}

function writeCookie(mode: ThemeMode) {
  document.cookie = `${COOKIE}=${mode};path=/;max-age=31536000;SameSite=Lax`;
}

function applyDom(mode: ThemeMode) {
  document.documentElement.classList.toggle("dark", mode === "dark");
}

interface ThemeState {
  mode: ThemeMode;
  setMode: (mode: ThemeMode) => void;
  toggle: () => void;
  hydrate: () => void;
}

export const useThemeStore = create<ThemeState>((set, get) => ({
  mode: "light",
  hydrate: () => {
    const mode = readCookie();
    applyDom(mode);
    set({ mode });
  },
  setMode: (mode) => {
    writeCookie(mode);
    applyDom(mode);
    set({ mode });
  },
  toggle: () => {
    const next = get().mode === "dark" ? "light" : "dark";
    get().setMode(next);
  },
}));
