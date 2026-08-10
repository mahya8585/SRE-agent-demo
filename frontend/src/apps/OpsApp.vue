<template>
  <div class="site-shell ops-site">
    <header class="site-header">
      <a class="brand" href="/" aria-label="Maison Vigne ホーム">
        <span class="brand-mark">MV</span>
        <span>
          <strong>Maison Vigne</strong>
          <small>Wholesale Operations</small>
        </span>
      </a>
      <div class="service-state" :class="{ 'is-error': errorMessage }">
        <span class="state-dot"></span>
        {{ errorMessage ? 'データ接続エラー' : '在庫同期オンライン' }}
      </div>
    </header>

    <main>
      <section class="page-heading">
        <div>
          <p class="eyebrow">Maison Vigne / Cellar inventory</p>
          <h1>ワイン卸売<br />オペレーション</h1>
          <p class="lede">選び抜かれたヴィンテージの背景と現在庫を、静かに見渡せるプライベート・セラーです。</p>
        </div>
        <div class="summary-stat">
          <span class="summary-rule"></span>
          <span>取扱商品</span>
          <strong>{{ wines.length }}</strong>
          <small>curated labels</small>
        </div>
      </section>

      <div v-if="errorMessage" class="notice error-notice" role="alert">
        <div>
          <strong>在庫データを取得できませんでした</strong>
          <p>{{ errorMessage }}</p>
        </div>
        <button class="secondary-button" type="button" @click="loadWines">再読み込み</button>
      </div>

      <section class="inventory-section" aria-labelledby="inventory-title">
        <div class="section-heading">
          <div>
            <p class="section-kicker">The current collection</p>
            <h2 id="inventory-title">セラー・セレクション</h2>
          </div>
          <span class="updated-at">{{ updatedLabel }}</span>
        </div>

        <div v-if="loading" class="loading-state" aria-live="polite">在庫を読み込んでいます...</div>
        <div v-else class="wine-grid">
          <article v-for="wine in wines" :key="wine.id" class="wine-card">
            <div class="wine-image-wrap">
              <img :src="resolveImageUrl(wine.image)" :alt="wine.name" @error="onImageError" />
              <span class="vintage-badge">{{ wine.vintage }}</span>
            </div>
            <div class="wine-meta">
              <p class="wine-category">{{ wine.category }}</p>
              <h3>{{ wine.name }}</h3>
              <p>{{ wine.region }} / {{ wine.variety }}</p>
              <div class="price-row">
                <strong>{{ formatPrice(wine.price) }}</strong>
                <span>在庫 {{ wine.stock }}</span>
              </div>
            </div>
          </article>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { getJson } from '../shared/api';

const wines = ref([]);
const loading = ref(true);
const errorMessage = ref('');
const updatedLabel = ref('未同期');

const formatPrice = (value) => {
  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? `¥${numericValue.toLocaleString('ja-JP')}` : '¥0';
};

const resolveImageUrl = (imagePath) => {
  if (!imagePath) return '/assets/wines/placeholder.svg';
  if (/^https?:\/\//.test(imagePath) || imagePath.startsWith('/')) return imagePath;
  return `/assets/wines/${imagePath}`;
};

const onImageError = (event) => {
  event.target.src = '/assets/wines/placeholder.svg';
};

const loadWines = async () => {
  loading.value = true;
  errorMessage.value = '';

  try {
    wines.value = await getJson('/api/wines');
    updatedLabel.value = `${new Intl.DateTimeFormat('ja-JP', {
      hour: '2-digit',
      minute: '2-digit'
    }).format(new Date())} 更新`;
  } catch (error) {
    errorMessage.value = error.message;
  } finally {
    loading.value = false;
  }
};

onMounted(loadWines);
</script>