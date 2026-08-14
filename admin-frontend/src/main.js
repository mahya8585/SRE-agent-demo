import { createApp } from 'vue';
import AdminApp from './app/AdminApp.vue';
import { trackPageView } from './shared/telemetry.js';
import './shared/styles.css';

trackPageView('Admin Dashboard');
createApp(AdminApp).mount('#app');