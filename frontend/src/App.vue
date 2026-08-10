<template>
  <div class="page-shell">
    <header class="hero">
      <div>
        <p class="eyebrow">現代のセラーに相応しい上質なワイン在庫体験</p>
        <h1>Maison Vigne • ワイン卸売オペレーション</h1>
        <p class="intro">SREチーム向けの洗練されたデモ体験で、在庫配分・在庫状況・配送 commitments を一目で把握できます。</p>
      </div>
      <div class="hero-card">
        <h3>現在の状態</h3>
        <ul>
          <li><span class="dot good"></span> API 正常</li>
          <li><span class="dot good"></span> 在庫同期オンライン</li>
          <li><span class="dot warning"></span> {{ incidents.length }} 件のアクティブインシデント</li>
        </ul>
      </div>
    </header>

    <main class="content-grid">
      <section class="panel">
        <div class="panel-title">
          <h2>注目のボトル</h2>
          <p>ヴィンテージ、産地、在庫配分を含む厳選されたラインナップです。</p>
        </div>
        <div class="wine-grid">
          <article v-for="wine in wines" :key="wine.id" class="wine-card">
            <img :src="resolveImageUrl(wine.image)" :alt="wine.name" @error="onImageError" />
            <div class="wine-meta">
              <h3>{{ wine.name }}</h3>
              <p>{{ wine.region }} • {{ wine.vintage }}</p>
              <p>{{ wine.variety }} • {{ wine.category }}</p>
              <div class="price-row">
                <strong>{{ formatPrice(wine.price) }}</strong>
                <span>在庫 {{ wine.stock }}</span>
              </div>
            </div>
          </article>
        </div>
      </section>

      <aside class="panel side-panel">
        <div class="panel-title">
          <h2>オペレーション・パルス</h2>
          <p>デモ向けのシグナルとインシデント概要です。</p>
        </div>
        <div class="metric-card">
          <p>応答時間</p>
          <h3>{{ responseTime }}</h3>
        </div>
        <div class="metric-card">
          <p>保留中の注文</p>
          <h3>{{ pendingOrders }}</h3>
        </div>
        <div class="metric-card">
          <p>在庫不足アラート</p>
          <h3>{{ lowStockAlerts }}</h3>
        </div>
        <div class="incident-card">
          <h3>最新インシデント</h3>
          <div v-for="incident in incidents" :key="incident.id" class="incident-item">
            <strong>{{ incident.title }}</strong>
            <p>{{ incident.summary }}</p>
            <span class="status-pill">{{ incident.status }}</span>
          </div>
        </div>
        <div class="incident-card" v-if="scenarioReport">
          <h3>SREレポート</h3>
          <p><strong>{{ scenarioReport.scenario }}</strong> • {{ scenarioReport.severity }}</p>
          <p>{{ scenarioReport.report }}</p>
          <span class="status-pill">{{ scenarioReport.status }}</span>
        </div>
        <div class="panel-actions">
          <button @click="triggerScenario('latency')">遅延を発生</button>
          <button @click="triggerScenario('db')">DB障害を発生</button>
        </div>
      </aside>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';

const wines = ref([]);
const incidents = ref([]);
const scenarioReport = ref(null);
const responseTime = ref('182 ms');
const pendingOrders = ref('14');
const lowStockAlerts = ref('3');

const formatPrice = (value) => {
  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? `¥${numericValue.toLocaleString('ja-JP')}` : '¥0';
};

const resolveImageUrl = (imagePath) => {
  if (!imagePath) return '/assets/wines/placeholder.svg';
  if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
    return imagePath;
  }
  if (imagePath.startsWith('/assets/')) {
    return imagePath;
  }
  if (imagePath.startsWith('/')) {
    return imagePath;
  }
  return `/assets/wines/${imagePath}`;
};

const onImageError = (event) => {
  event.target.src = '/assets/wines/placeholder.svg';
};

const loadData = async () => {
  const [wineResponse, incidentResponse] = await Promise.all([
    fetch('http://localhost:8081/api/wines'),
    fetch('http://localhost:8081/api/demo/incidents')
  ]);
  wines.value = await wineResponse.json();
  incidents.value = await incidentResponse.json();
};

const triggerScenario = async (scenario) => {
  const response = await fetch(`http://localhost:8081/api/demo/scenarios/${scenario}`, {
    method: 'POST'
  });
  const report = await response.json();
  scenarioReport.value = report;
  incidents.value = report.activeIncidents;
  responseTime.value = scenario === 'latency' ? '1.8 s' : '2.4 s';
  pendingOrders.value = scenario === 'latency' ? '21' : '29';
  lowStockAlerts.value = scenario === 'latency' ? '4' : '5';
};

onMounted(() => {
  loadData();
});
</script>

<style>
:root {
  color-scheme: dark;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  background: #120c0a;
  color: #f6ebda;
}
body {
  margin: 0;
  background: linear-gradient(135deg, #1d120c, #2f1b13 60%, #0d0907);
}
* { box-sizing: border-box; }
.page-shell { max-width: 1320px; margin: 0 auto; padding: 32px 24px 64px; }
.hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: center;
  padding: 24px 0 40px;
}
.eyebrow { text-transform: uppercase; letter-spacing: .3em; color: #d4b36d; font-size: .75rem; }
h1 { font-size: 2.4rem; margin: 6px 0 12px; color: #fff3d7; }
.intro { max-width: 600px; color: #dcc8aa; line-height: 1.6; }
.hero-card, .panel, .metric-card, .incident-card { background: rgba(34, 22, 17, 0.88); border: 1px solid rgba(212,179,109,0.25); border-radius: 20px; box-shadow: 0 12px 40px rgba(0,0,0,.18); }
.hero-card { padding: 20px 24px; min-width: 260px; }
.hero-card ul { list-style: none; padding: 0; margin: 10px 0 0; }
.hero-card li { display: flex; align-items: center; gap: 8px; margin: 8px 0; }
.dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
.dot.good { background: #4acb78; }
.dot.warning { background: #f0b24b; }
.content-grid { display: grid; gap: 24px; grid-template-columns: 2fr 1fr; }
.panel { padding: 24px; }
.panel-title h2 { margin: 0 0 6px; }
.panel-title p { margin: 0 0 18px; color: #c8ad85; }
.wine-grid { display: grid; gap: 16px; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); }
.wine-card { overflow: hidden; border-radius: 16px; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,.09); }
.wine-card img { width: 100%; height: 220px; object-fit: cover; display: block; }
.wine-meta { padding: 16px; }
.wine-meta h3 { margin: 0 0 8px; font-size: 1rem; }
.wine-meta p { margin: 4px 0; color: #d7c4a3; font-size: .95rem; }
.price-row { display: flex; justify-content: space-between; align-items: center; margin-top: 12px; color: #f6ebda; }
.side-panel { display: flex; flex-direction: column; gap: 14px; }
.metric-card, .incident-card { padding: 18px; }
.metric-card h3 { margin: 6px 0 0; font-size: 1.5rem; }
.incident-card p { color: #d7c4a3; margin: 8px 0 0; }
.incident-item { margin-top: 10px; padding-top: 10px; border-top: 1px solid rgba(255,255,255,.08); }
.status-pill { display: inline-block; margin-top: 4px; background: rgba(212,179,109,0.22); color: #f4e0ae; padding: 4px 8px; border-radius: 999px; font-size: .8rem; }
.panel-actions { display: flex; gap: 10px; }
button { background: #7a2f22; color: white; border: none; border-radius: 999px; padding: 10px 14px; cursor: pointer; }
@media (max-width: 900px) { .content-grid { grid-template-columns: 1fr; } .hero { flex-direction: column; align-items: flex-start; } }
</style>
