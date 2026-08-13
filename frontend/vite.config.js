import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

const siteConfig = {
  ops: {
    app: './src/apps/OpsApp.vue',
    title: 'Maison Vigne | Fine Wine Merchant',
    description: '厳選されたワインをセラーからお届けする、Maison Vigneのオンラインストア',
    port: 3000
  }
};

export default defineConfig(({ mode }) => {
  const site = siteConfig[mode] || siteConfig.ops;

  return {
    plugins: [
      vue(),
      {
        name: 'site-title',
        transformIndexHtml: (html) => html
          .replace('%SITE_TITLE%', site.title)
          .replace('%SITE_DESCRIPTION%', site.description)
      }
    ],
    resolve: {
      alias: {
        '@site-app': fileURLToPath(new URL(site.app, import.meta.url))
      }
    },
    build: {
      outDir: 'dist/ops',
      emptyOutDir: true
    },
    server: {
      host: '0.0.0.0',
      port: site.port
    }
  };
});
