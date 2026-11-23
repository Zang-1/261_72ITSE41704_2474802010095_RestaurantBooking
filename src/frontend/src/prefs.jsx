import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import VI from './i18n-vi.js';

const PrefsContext = createContext(null);

function stored(key, fallback) {
  try {
    return localStorage.getItem(key) || fallback;
  } catch {
    return fallback;
  }
}

function store(key, value) {
  try {
    localStorage.setItem(key, value);
  } catch {
    // private window or blocked storage: the choice simply is not remembered
  }
}

/**
 * Light/dark theme and English/Vietnamese text, remembered in this browser.
 * English is the source language: t('Table {n}', { n: 5 }) looks the English text up in the
 * Vietnamese dictionary and falls back to the English when a phrase has no translation.
 */
export function PrefsProvider({ children }) {
  const [theme, setTheme] = useState(() =>
    stored('theme', window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'));
  const [lang, setLang] = useState(() => stored('lang', 'en'));

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    store('theme', theme);
  }, [theme]);

  useEffect(() => {
    document.documentElement.lang = lang;
    store('lang', lang);
  }, [lang]);

  const t = useCallback((text, vars) => {
    let out = (lang === 'vi' && VI[text]) || text;
    if (vars) {
      for (const [name, value] of Object.entries(vars)) out = out.replaceAll(`{${name}}`, value);
    }
    return out;
  }, [lang]);

  const value = useMemo(() => ({
    theme, lang, t,
    toggleTheme: () => setTheme((v) => (v === 'dark' ? 'light' : 'dark')),
    toggleLang: () => setLang((v) => (v === 'vi' ? 'en' : 'vi')),
  }), [theme, lang, t]);

  return <PrefsContext.Provider value={value}>{children}</PrefsContext.Provider>;
}

export function usePrefs() {
  return useContext(PrefsContext);
}

export function useT() {
  return useContext(PrefsContext).t;
}
