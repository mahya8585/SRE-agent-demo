<template>
  <div class="admin-shell">
    <aside class="sidebar">
      <div class="brand-mark" aria-label="Maison Vigne">
        <span>MV</span>
      </div>
      <nav class="primary-nav" aria-label="管理メニュー">
        <button class="nav-button" :class="{ 'is-active': view === 'overview' }" type="button" title="ダッシュボード" @click="changeView('overview')">
          <LayoutDashboard :size="20" aria-hidden="true" />
          <span>概要</span>
        </button>
        <button class="nav-button" :class="{ 'is-active': view === 'orders' }" type="button" title="注文管理" @click="changeView('orders')">
          <ClipboardList :size="20" aria-hidden="true" />
          <span>注文</span>
        </button>
        <button class="nav-button" :class="{ 'is-active': view === 'inventory' }" type="button" title="在庫管理" @click="changeView('inventory')">
          <Wine :size="20" aria-hidden="true" />
          <span>在庫</span>
        </button>
        <button class="nav-button" :class="{ 'is-active': view === 'purchases' }" type="button" title="発注管理" @click="changeView('purchases')">
          <PackagePlus :size="20" aria-hidden="true" />
          <span>発注</span>
        </button>
      </nav>
      <div class="sidebar-footer">
        <div class="environment-dot" aria-hidden="true"></div>
        <span>Operations</span>
      </div>
    </aside>

    <main class="workspace">
      <header class="topbar">
        <div>
          <p class="eyebrow">MAISON VIGNE / OPERATIONS</p>
          <h1>{{ viewTitle }}</h1>
        </div>
        <button class="icon-button" type="button" title="最新情報に更新" :disabled="loading" @click="loadCurrentView">
          <RefreshCw :size="18" :class="{ spinning: loading }" aria-hidden="true" />
        </button>
      </header>

      <section v-if="error" class="status-message is-error" role="alert">
        <AlertTriangle :size="20" aria-hidden="true" />
        <div>
          <strong>データを取得できませんでした</strong>
          <p>APIの稼働状況を確認して、もう一度お試しください。</p>
        </div>
        <button type="button" @click="loadCurrentView">再試行</button>
      </section>

      <template v-if="view === 'overview'">
      <section class="metrics-grid" aria-label="主要指標" :aria-busy="loading">
        <article v-for="metric in metrics" :key="metric.label" class="metric">
          <div class="metric-heading">
            <span>{{ metric.label }}</span>
            <component :is="metric.icon" :size="18" aria-hidden="true" />
          </div>
          <div v-if="loading" class="metric-skeleton" aria-label="読み込み中"></div>
          <p v-else class="metric-value">{{ metric.value }}</p>
          <p class="metric-caption">{{ metric.caption }}</p>
        </article>
      </section>

      <section class="operations-panel">
        <div class="panel-heading">
          <div>
            <p class="section-label">INVENTORY SIGNAL</p>
            <h2>在庫状況</h2>
          </div>
          <span class="live-status"><i></i> Live</span>
        </div>
        <div class="stock-overview">
          <div class="stock-figure">
            <span>総在庫数</span>
            <strong>{{ loading ? '---' : formatNumber(summary.totalStock) }}</strong>
            <small>本</small>
          </div>
          <div class="stock-alert" :class="{ 'has-warning': summary.lowStockCount > 0 }">
            <PackageSearch :size="24" aria-hidden="true" />
            <div>
              <span>補充確認が必要な商品</span>
              <strong>{{ loading ? '--' : summary.lowStockCount }} SKU</strong>
            </div>
          </div>
        </div>
      </section>
      </template>

      <section v-else-if="view === 'orders'" class="data-panel" :aria-busy="loading">
        <div class="panel-heading">
          <div><p class="section-label">ORDER FLOW</p><h2>注文一覧</h2></div>
          <span class="record-count">{{ orders.length }} 件</span>
        </div>
        <div class="table-scroll">
          <table>
            <thead><tr><th>注文番号</th><th>顧客</th><th>注文日時</th><th>点数</th><th>合計</th><th>ステータス</th></tr></thead>
            <tbody>
              <tr v-for="order in orders" :key="order.id">
                <td class="mono">{{ order.orderNumber }}</td>
                <td>{{ order.customerName }}</td>
                <td>{{ formatDate(order.createdAt) }}</td>
                <td>{{ order.itemCount }}</td>
                <td>{{ formatCurrency(order.total) }}</td>
                <td>
                  <select :value="order.status" :disabled="savingId === order.id" @change="saveOrderStatus(order, $event.target.value)">
                    <option v-for="status in orderStatuses" :key="status" :value="status">{{ statusLabel(status) }}</option>
                  </select>
                </td>
              </tr>
              <tr v-if="!loading && orders.length === 0"><td colspan="6" class="empty-state">注文はありません</td></tr>
            </tbody>
          </table>
        </div>
      </section>

      <section v-else-if="view === 'inventory'" class="data-panel" :aria-busy="loading">
        <div class="panel-heading">
          <div><p class="section-label">STOCK CONTROL</p><h2>在庫一覧</h2></div>
          <div class="panel-actions">
            <span class="record-count">{{ inventory.length }} SKU</span>
            <button class="primary-button" type="button" :aria-expanded="showCreateWineForm" @click="openWineForm">
              <Plus :size="17" aria-hidden="true" />新規追加
            </button>
          </div>
        </div>
        <form v-if="showCreateWineForm" class="wine-form" @submit.prevent="submitWine">
          <label class="field-span-2">
            <span>商品名 *</span>
            <input v-model.trim="wineForm.name" type="text" maxlength="255" required :disabled="savingId === 'create-wine'">
          </label>
          <label>
            <span>カテゴリ *</span>
            <select v-model="wineForm.category" required :disabled="savingId === 'create-wine'">
              <option disabled value="">選択してください</option>
              <option v-for="category in wineCategories" :key="category" :value="category">{{ category }}</option>
            </select>
          </label>
          <label>
            <span>価格 *</span>
            <input v-model.number="wineForm.price" type="number" min="0" step="1" required :disabled="savingId === 'create-wine'">
          </label>
          <label>
            <span>産地</span>
            <input v-model.trim="wineForm.region" type="text" maxlength="255" :disabled="savingId === 'create-wine'">
          </label>
          <label>
            <span>品種</span>
            <input v-model.trim="wineForm.variety" type="text" maxlength="255" :disabled="savingId === 'create-wine'">
          </label>
          <label>
            <span>ヴィンテージ</span>
            <input v-model.trim="wineForm.vintage" type="text" maxlength="32" placeholder="2024" :disabled="savingId === 'create-wine'">
          </label>
          <label>
            <span>初期在庫 *</span>
            <input v-model.number="wineForm.stock" type="number" min="0" step="1" required :disabled="savingId === 'create-wine'">
          </label>
          <label class="field-span-2">
            <span>商品説明</span>
            <textarea v-model.trim="wineForm.description" rows="4" maxlength="2000" :disabled="savingId === 'create-wine'"></textarea>
          </label>
          <label class="field-span-2">
            <span>商品画像</span>
            <input ref="wineImageInput" class="file-input" type="file" accept="image/jpeg,image/png,image/webp" :disabled="savingId === 'create-wine'" @change="selectWineImage">
            <small>JPEG、PNG、WebP / 最大5MB</small>
            <small v-if="wineImageError" class="field-error" role="alert">{{ wineImageError }}</small>
          </label>
          <div class="form-actions field-span-2">
            <button class="form-cancel-button" type="button" :disabled="savingId === 'create-wine'" @click="closeWineForm">キャンセル</button>
            <button class="primary-button" type="submit" :disabled="savingId === 'create-wine' || !wineForm.name || !wineForm.category || wineForm.price < 0 || wineForm.stock < 0">
              <Save :size="17" aria-hidden="true" />登録
            </button>
          </div>
        </form>
        <div class="table-scroll">
          <table class="inventory-table">
            <thead><tr><th>商品名</th><th>カテゴリ</th><th>価格</th><th>商品説明</th><th>在庫数</th><th><span class="sr-only">操作</span></th></tr></thead>
            <tbody>
              <tr v-for="item in inventory" :key="item.id">
                <td><strong>{{ item.name }}</strong></td>
                <td>{{ item.category }}</td>
                <td>{{ formatCurrency(item.price) }}</td>
                <td><textarea v-model.trim="item.description" rows="3" maxlength="2000" :disabled="savingId === item.id" :aria-label="`${item.name}の商品説明`"></textarea></td>
                <td><input v-model.number="item.stock" type="number" min="0" :disabled="savingId === item.id" aria-label="在庫数"></td>
                <td><button class="save-button" type="button" :disabled="savingId === item.id || item.stock < 0" @click="saveInventory(item)"><Save :size="16" aria-hidden="true" />保存</button></td>
              </tr>
              <tr v-if="!loading && inventory.length === 0"><td colspan="6" class="empty-state">在庫商品はありません</td></tr>
            </tbody>
          </table>
        </div>
      </section>

      <section v-else class="data-panel" :aria-busy="loading">
        <div class="panel-heading">
          <div><p class="section-label">PROCUREMENT</p><h2>発注一覧</h2></div>
          <span class="record-count">{{ purchaseOrders.length }} 件</span>
        </div>
        <form class="purchase-form" @submit.prevent="submitPurchaseOrder">
          <label>
            <span>ワイン</span>
            <select v-model.number="purchaseForm.wineId" required :disabled="savingId === 'create'">
              <option disabled value="">商品を選択</option>
              <option v-for="item in inventory" :key="item.id" :value="item.id">{{ item.name }}（在庫 {{ item.stock }}本）</option>
            </select>
          </label>
          <label>
            <span>発注数</span>
            <input v-model.number="purchaseForm.quantity" type="number" min="1" required :disabled="savingId === 'create'">
          </label>
          <button class="primary-button" type="submit" :disabled="savingId === 'create' || !purchaseForm.wineId || purchaseForm.quantity < 1">
            <PackagePlus :size="17" aria-hidden="true" />発注する
          </button>
        </form>
        <div class="delivery-note"><CalendarClock :size="17" aria-hidden="true" />納品予定日は発注日から5営業日後です</div>
        <div class="table-scroll">
          <table>
            <thead><tr><th>商品名</th><th>発注日</th><th>納品予定日</th><th>発注数</th><th><span class="sr-only">操作</span></th></tr></thead>
            <tbody>
              <tr v-for="purchaseOrder in purchaseOrders" :key="purchaseOrder.id">
                <td><strong>{{ purchaseOrder.wineName }}</strong></td>
                <td>{{ formatDateOnly(purchaseOrder.orderedDate) }}</td>
                <td class="delivery-date">{{ formatDateOnly(purchaseOrder.deliveryDate) }}</td>
                <td class="mono">{{ formatNumber(purchaseOrder.quantity) }} 本</td>
                <td>
                  <button class="receive-button" type="button" :disabled="savingId === purchaseOrder.id" @click="receiveOrder(purchaseOrder)">
                    <PackageCheck :size="17" aria-hidden="true" />受け取り
                  </button>
                </td>
              </tr>
              <tr v-if="!loading && purchaseOrders.length === 0"><td colspan="5" class="empty-state">発注中の商品はありません</td></tr>
            </tbody>
          </table>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import {
  AlertTriangle,
  Banknote,
  CalendarClock,
  ClipboardList,
  LayoutDashboard,
  PackageCheck,
  PackagePlus,
  PackageSearch,
  Plus,
  RefreshCw,
  Save,
  ShoppingCart,
  Wine
} from '@lucide/vue';
import {
  createWine,
  createPurchaseOrder,
  getDashboardSummary,
  getInventory,
  getOrders,
  getPurchaseOrders,
  receivePurchaseOrder,
  updateInventory,
  updateOrderStatus
} from '../shared/api.js';
import { trackEvent, trackPageView } from '../shared/telemetry.js';

const summary = ref({ orderCount: 0, revenue: 0, lowStockCount: 0, totalStock: 0 });
const orders = ref([]);
const inventory = ref([]);
const purchaseOrders = ref([]);
const purchaseForm = ref({ wineId: '', quantity: 1 });
const emptyWineForm = () => ({ name: '', category: '', region: '', variety: '', vintage: '', image: '', description: '', price: 0, stock: 0 });
const wineForm = ref(emptyWineForm());
const wineImage = ref(null);
const wineImageInput = ref(null);
const wineImageError = ref('');
const showCreateWineForm = ref(false);
const view = ref('overview');
const loading = ref(true);
const error = ref(false);
const savingId = ref(null);
const orderStatuses = ['CONFIRMED', 'PROCESSING', 'SHIPPED'];
const wineCategories = ['Red', 'White', 'Rosé', 'Sparkling', 'Dessert'];

const viewTitle = computed(() => ({
  overview: '営業ダッシュボード',
  orders: '注文管理',
  inventory: '在庫管理',
  purchases: '発注管理'
}[view.value]));

const formatNumber = (value) => new Intl.NumberFormat('ja-JP').format(value);
const formatCurrency = (value) => new Intl.NumberFormat('ja-JP', {
  style: 'currency',
  currency: 'JPY',
  maximumFractionDigits: 0
}).format(value);
const formatDate = (value) => new Intl.DateTimeFormat('ja-JP', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
const formatDateOnly = (value) => new Intl.DateTimeFormat('ja-JP', { dateStyle: 'long', timeZone: 'UTC' }).format(new Date(`${value}T00:00:00Z`));
const statusLabel = (status) => ({ CONFIRMED: '受付済み', PROCESSING: '処理中', SHIPPED: '発送済み' }[status] || status);

const metrics = computed(() => [
  { label: '受注件数', value: formatNumber(summary.value.orderCount), caption: '累計注文', icon: ShoppingCart },
  { label: '売上高', value: formatCurrency(summary.value.revenue), caption: '確定済み売上', icon: Banknote },
  { label: '在庫アラート', value: `${formatNumber(summary.value.lowStockCount)} SKU`, caption: '発注点以下', icon: AlertTriangle }
]);

const loadSummary = async () => {
  loading.value = true;
  error.value = false;

  try {
    summary.value = await getDashboardSummary();
  } catch {
    error.value = true;
  } finally {
    loading.value = false;
  }
};

const loadCurrentView = async () => {
  if (view.value === 'overview') return loadSummary();
  loading.value = true;
  error.value = false;
  try {
    if (view.value === 'orders') orders.value = await getOrders();
    else if (view.value === 'inventory') inventory.value = await getInventory();
    else {
      const [ordersResult, inventoryResult] = await Promise.all([getPurchaseOrders(), getInventory()]);
      purchaseOrders.value = ordersResult;
      inventory.value = inventoryResult;
    }
  } catch {
    error.value = true;
  } finally {
    loading.value = false;
  }
};

const changeView = (nextView) => {
  if (view.value === nextView) return;
  if (view.value === 'inventory') closeWineForm();
  view.value = nextView;
  trackPageView(`Admin ${viewTitle.value}`);
  loadCurrentView();
};

const saveOrderStatus = async (order, status) => {
  const previousStatus = order.status;
  savingId.value = order.id;
  error.value = false;
  try {
    Object.assign(order, await updateOrderStatus(order.id, status));
    trackEvent('AdminOrderStatusUpdated', { previousStatus, newStatus: status });
  } catch {
    order.status = previousStatus;
    error.value = true;
  } finally {
    savingId.value = null;
  }
};

const saveInventory = async (item) => {
  savingId.value = item.id;
  error.value = false;
  try {
    Object.assign(item, await updateInventory(item.id, item.stock, item.threshold, item.description));
    trackEvent('AdminInventoryUpdated', { stockState: item.stock <= item.threshold ? 'low' : 'normal' });
  } catch {
    error.value = true;
  } finally {
    savingId.value = null;
  }
};

const closeWineForm = () => {
  wineForm.value = emptyWineForm();
  wineImage.value = null;
  wineImageError.value = '';
  if (wineImageInput.value) wineImageInput.value.value = '';
  showCreateWineForm.value = false;
};

const openWineForm = () => {
  showCreateWineForm.value = true;
};

const selectWineImage = (event) => {
  const [file] = event.target.files;
  wineImage.value = null;
  wineImageError.value = '';
  if (!file) return;
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    wineImageError.value = 'JPEG、PNG、WebP形式の画像を選択してください。';
    event.target.value = '';
    return;
  }
  if (file.size > 5 * 1024 * 1024) {
    wineImageError.value = '画像サイズは5MB以下にしてください。';
    event.target.value = '';
    return;
  }
  wineImage.value = file;
};

const submitWine = async () => {
  savingId.value = 'create-wine';
  error.value = false;
  try {
    const created = await createWine(wineForm.value, wineImage.value);
    inventory.value.push(created);
    inventory.value.sort((left, right) => left.name.localeCompare(right.name, 'ja'));
    trackEvent('AdminWineCreated', { initialStockBand: created.stock === 0 ? 'empty' : created.stock < 10 ? 'small' : 'standard' });
    closeWineForm();
  } catch {
    error.value = true;
  } finally {
    savingId.value = null;
  }
};

const submitPurchaseOrder = async () => {
  savingId.value = 'create';
  error.value = false;
  try {
    const created = await createPurchaseOrder(purchaseForm.value.wineId, purchaseForm.value.quantity);
    purchaseOrders.value.push(created);
    purchaseOrders.value.sort((left, right) => left.deliveryDate.localeCompare(right.deliveryDate) || left.id - right.id);
    trackEvent('AdminPurchaseOrderCreated', { quantityBand: created.quantity < 10 ? 'small' : created.quantity < 50 ? 'medium' : 'large' });
    purchaseForm.value.quantity = 1;
  } catch {
    error.value = true;
  } finally {
    savingId.value = null;
  }
};

const receiveOrder = async (purchaseOrder) => {
  savingId.value = purchaseOrder.id;
  error.value = false;
  try {
    await receivePurchaseOrder(purchaseOrder.id);
    purchaseOrders.value = purchaseOrders.value.filter((item) => item.id !== purchaseOrder.id);
    inventory.value = await getInventory();
    trackEvent('AdminPurchaseOrderReceived', { quantityBand: purchaseOrder.quantity < 10 ? 'small' : purchaseOrder.quantity < 50 ? 'medium' : 'large' });
  } catch {
    error.value = true;
  } finally {
    savingId.value = null;
  }
};

onMounted(loadSummary);
</script>