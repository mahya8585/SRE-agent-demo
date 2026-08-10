<template>
  <div class="site-shell pulse-site">
    <header class="site-header pulse-header">
      <div class="brand pulse-brand">
        <span class="pulse-symbol" aria-hidden="true"></span>
        <span>
          <strong>Operation Pulse</strong>
          <small>Maison Vigne / SRE Console</small>
        </span>
      </div>
      <div class="service-state" :class="{ 'is-error': errorMessage }">
        <span class="state-dot"></span>
        {{ errorMessage ? 'API unavailable' : 'Systems connected' }}
      </div>
    </header>

    <main>
      <section class="pulse-heading">
        <div>
          <p class="eyebrow">Live service signals</p>
          <h1>オペレーション・パルス</h1>
          <p class="lede">サービス状態を観測し、デモ用障害シナリオからSREレスポンスを確認します。</p>
        </div>
        <p class="clock" aria-label="現在時刻">{{ currentTime }}</p>
      </section>

      <div v-if="errorMessage" class="notice error-notice" role="alert">
        <div>
          <strong>監視データを取得できませんでした</strong>
          <p>{{ errorMessage }}</p>
        </div>
        <button class="secondary-button" type="button" @click="loadIncidents">再接続</button>
      </div>

      <section class="metric-grid" aria-label="主要メトリクス">
        <article class="metric-card">
          <p>応答時間</p>
          <strong>{{ responseTime }}</strong>
          <span>Inventory API</span>
        </article>
        <article class="metric-card">
          <p>保留中の注文</p>
          <strong>{{ pendingOrders }}</strong>
          <span>Fulfillment queue</span>
        </article>
        <article class="metric-card">
          <p>在庫不足アラート</p>
          <strong>{{ lowStockAlerts }}</strong>
          <span>Stock threshold</span>
        </article>
      </section>

      <section class="pulse-grid">
        <div class="incident-panel">
          <div class="section-heading">
            <div>
              <p class="section-kicker">Incident stream</p>
              <h2>インシデント</h2>
            </div>
            <span class="count-badge">{{ incidents.length }}</span>
          </div>
          <div v-if="loading" class="loading-state" aria-live="polite">シグナルを読み込んでいます...</div>
          <div v-else class="incident-list">
            <article v-for="incident in incidents" :key="incident.id" class="incident-item">
              <div class="incident-id">{{ incident.id }}</div>
              <div>
                <h3>{{ incident.title }}</h3>
                <p>{{ incident.summary }}</p>
              </div>
              <span class="status-pill">{{ incident.status }}</span>
            </article>
          </div>
        </div>

        <aside class="scenario-panel">
          <div>
            <p class="section-kicker">Demo controls</p>
            <h2>シナリオ注入</h2>
            <p class="panel-copy">デモ環境へ障害シグナルを送り、生成されるSREレポートを確認します。</p>
          </div>
          <div class="scenario-actions">
            <button type="button" :disabled="scenarioPending" @click="triggerScenario('latency')">
              <span>LAT</span> 遅延を発生
            </button>
            <button type="button" :disabled="scenarioPending" @click="triggerScenario('db')">
              <span>DB</span> DB障害を発生
            </button>
          </div>
          <article v-if="scenarioReport" class="report-card" aria-live="polite">
            <div class="report-heading">
              <span>{{ scenarioReport.severity }}</span>
              <strong>{{ scenarioReport.status }}</strong>
            </div>
            <h3>{{ scenarioReport.scenario }} scenario</h3>
            <p>{{ scenarioReport.report }}</p>
          </article>
        </aside>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { getJson, postJson } from '../shared/api';

const incidents = ref([]);
const scenarioReport = ref(null);
const responseTime = ref('182 ms');
const pendingOrders = ref('14');
const lowStockAlerts = ref('3');
const loading = ref(true);
const scenarioPending = ref(false);
const errorMessage = ref('');
const currentTime = ref('');
let clockTimer;

const updateClock = () => {
  currentTime.value = new Intl.DateTimeFormat('ja-JP', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).format(new Date());
};

const loadIncidents = async () => {
  loading.value = true;
  errorMessage.value = '';
  try {
    incidents.value = await getJson('/api/demo/incidents');
  } catch (error) {
    errorMessage.value = error.message;
  } finally {
    loading.value = false;
  }
};

const triggerScenario = async (scenario) => {
  scenarioPending.value = true;
  errorMessage.value = '';
  try {
    const report = await postJson(`/api/demo/scenarios/${scenario}`);
    scenarioReport.value = report;
    incidents.value = report.activeIncidents;
    responseTime.value = scenario === 'latency' ? '1.8 s' : '2.4 s';
    pendingOrders.value = scenario === 'latency' ? '21' : '29';
    lowStockAlerts.value = scenario === 'latency' ? '4' : '5';
  } catch (error) {
    errorMessage.value = error.message;
  } finally {
    scenarioPending.value = false;
  }
};

onMounted(() => {
  updateClock();
  clockTimer = window.setInterval(updateClock, 1000);
  loadIncidents();
});

onBeforeUnmount(() => window.clearInterval(clockTimer));
</script>