import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

const siteConfig = {
  ops: {
    app: './src/apps/OpsApp.vue',
    title: 'Maison Vigne | Wholesale Operations',
    port: 3000
  },
  pulse: {
    app: './src/apps/PulseApp.vue',
    title: 'Operation Pulse | SRE Console',
    port: 3001
  }
};

export default defineConfig(({ mode }) => {
  const site = siteConfig[mode] || siteConfig.ops;

  return {
    plugins: [
      vue(),
      {
        name: 'site-title',
        transformIndexHtml: (html) => html.replace('%SITE_TITLE%', site.title)
      }
    ],
    resolve: {
      alias: {
        '@site-app': fileURLToPath(new URL(site.app, import.meta.url))
      }
    },
    build: {
      outDir: `dist/${mode === 'pulse' ? 'pulse' : 'ops'}`,
      emptyOutDir: true
    },
    server: {
      host: '0.0.0.0',
      port: site.port
    }
  };
});
