/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_ENV?: string; // optional if you want custom variables
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
