<template>
  <div class="site-shell ops-site">
    <header class="site-header">
      <a class="brand" href="/" aria-label="Maison Vigne ホーム">
        <span class="brand-mark">MV</span>
        <span><strong>Maison Vigne</strong><small>Fine wine merchant</small></span>
      </a>
      <button class="cart-trigger" type="button" aria-label="買い物かごを開く" @click="openCart">
        <ShoppingBag :size="20" aria-hidden="true" />
        <span>買い物かご</span>
        <strong>{{ cartCount }}</strong>
      </button>
    </header>

    <main>
      <section class="page-heading">
        <div>
          <p class="eyebrow">Maison Vigne / Private collection</p>
          <h1>特別な時間に、<br />記憶に残る一本を。</h1>
          <p class="lede">産地と造り手の個性が息づくワインを、私たちのセラーからお届けします。</p>
        </div>
        <div class="summary-stat">
          <span class="summary-rule"></span><span>今季のセレクション</span>
          <strong>{{ wines.length }}</strong><small>curated bottles</small>
        </div>
      </section>

      <div v-if="errorMessage" class="notice error-notice" role="alert">
        <div><strong>商品を読み込めませんでした</strong><p>{{ errorMessage }}</p></div>
        <button class="secondary-button" type="button" @click="loadWines">再読み込み</button>
      </div>

      <section class="inventory-section" aria-labelledby="inventory-title">
        <div class="section-heading">
          <div><p class="section-kicker">The current collection</p><h2 id="inventory-title">セラー・セレクション</h2></div>
          <span class="updated-at">全国一律送料 ¥800 / ¥15,000以上で送料無料</span>
        </div>
        <div v-if="loading" class="loading-state" aria-live="polite">商品を読み込んでいます...</div>
        <div v-else class="wine-grid">
          <article v-for="wine in wines" :key="wine.id" class="wine-card">
            <button class="wine-card-main" type="button" @click="showDetails(wine)">
              <div class="wine-image-wrap">
                <img :src="resolveImageUrl(wine.image)" :alt="wine.name" @error="onImageError" />
                <span class="vintage-badge">{{ wine.vintage }}</span>
              </div>
              <div class="wine-meta">
                <p class="wine-category">{{ wine.category }}</p><h3>{{ wine.name }}</h3>
                <p>{{ wine.region }} / {{ wine.variety }}</p>
                <div class="price-row"><strong>{{ formatPrice(wine.price) }}</strong><span>{{ wine.stock > 0 ? '在庫あり' : '売り切れ' }}</span></div>
              </div>
            </button>
            <button class="card-cart-button" type="button" :disabled="wine.stock < 1" @click="addCardToCart(wine)">
              <ShoppingBag :size="16" aria-hidden="true" />{{ wine.stock > 0 ? '買い物かごに追加' : '売り切れ' }}
            </button>
          </article>
        </div>
      </section>
    </main>

    <dialog ref="detailsDialog" class="product-dialog" @click="closeDialogFromBackdrop">
      <div v-if="selectedWine" class="product-detail">
        <button class="icon-button dialog-close" type="button" aria-label="商品詳細を閉じる" @click="closeDetails"><X :size="20" /></button>
        <div class="detail-image"><img :src="resolveImageUrl(selectedWine.image)" :alt="selectedWine.name" @error="onImageError" /></div>
        <div class="detail-copy">
          <p class="section-kicker">{{ selectedWine.category }} / {{ selectedWine.vintage }}</p>
          <h2>{{ selectedWine.name }}</h2><p class="detail-origin">{{ selectedWine.region }} · {{ selectedWine.variety }}</p>
          <p class="detail-description">{{ wineDescription(selectedWine) }}</p>
          <dl class="wine-facts">
            <div><dt>産地</dt><dd>{{ selectedWine.region }}</dd></div>
            <div><dt>品種</dt><dd>{{ selectedWine.variety }}</dd></div>
            <div><dt>ヴィンテージ</dt><dd>{{ selectedWine.vintage }}</dd></div>
          </dl>
          <div class="detail-purchase">
            <div><span>価格</span><strong>{{ formatPrice(selectedWine.price) }}</strong></div>
            <label>数量<select v-model.number="detailQuantity" :disabled="selectedWine.stock < 1"><option v-for="quantity in Math.min(selectedWine.stock, 12)" :key="quantity" :value="quantity">{{ quantity }}</option></select></label>
          </div>
          <button class="primary-button" type="button" :disabled="selectedWine.stock < 1" @click="addDetailToCart"><ShoppingBag :size="18" />{{ selectedWine.stock > 0 ? '買い物かごに追加' : '売り切れ' }}</button>
        </div>
      </div>
    </dialog>

    <div v-if="cartOpen" class="drawer-layer" @click.self="closeCart">
      <aside class="cart-drawer" aria-label="買い物かご">
        <header class="drawer-header">
          <div><p class="section-kicker">Your selection</p><h2>{{ drawerTitle }}</h2></div>
          <button class="icon-button" type="button" aria-label="買い物かごを閉じる" @click="closeCart"><X :size="20" /></button>
        </header>

        <template v-if="checkoutStep === 'cart'">
          <div v-if="cartItems.length" class="cart-items">
            <article v-for="item in cartItems" :key="item.wine.id" class="cart-item">
              <img :src="resolveImageUrl(item.wine.image)" :alt="item.wine.name" @error="onImageError" />
              <div class="cart-item-copy"><h3>{{ item.wine.name }}</h3><p>{{ formatPrice(item.wine.price) }}</p>
                <div class="quantity-control" :aria-label="`${item.wine.name}の数量`">
                  <button type="button" aria-label="数量を減らす" @click="changeQuantity(item.wine.id, -1)"><Minus :size="14" /></button><span>{{ item.quantity }}</span>
                  <button type="button" aria-label="数量を増やす" :disabled="item.quantity >= item.wine.stock" @click="changeQuantity(item.wine.id, 1)"><Plus :size="14" /></button>
                </div>
              </div>
              <button class="remove-button" type="button" aria-label="商品を削除" @click="removeFromCart(item.wine.id)"><Trash2 :size="16" /></button>
            </article>
          </div>
          <div v-else class="empty-cart"><ShoppingBag :size="34" /><h3>買い物かごは空です</h3><p>セラーからお気に入りの一本をお選びください。</p><button class="secondary-button" type="button" @click="closeCart">商品を見る</button></div>
          <div v-if="cartItems.length" class="cart-footer">
            <div class="total-lines"><p><span>小計</span><strong>{{ formatPrice(cartSubtotal) }}</strong></p><p><span>送料</span><strong>{{ shipping === 0 ? '無料' : formatPrice(shipping) }}</strong></p><p class="grand-total"><span>合計</span><strong>{{ formatPrice(cartTotal) }}</strong></p></div>
            <button class="primary-button" type="button" @click="checkoutStep = 'checkout'">購入手続きへ <ArrowRight :size="18" /></button>
          </div>
        </template>

        <form v-else-if="checkoutStep === 'checkout'" class="checkout-form" @submit.prevent="submitOrder">
          <button class="back-button" type="button" @click="checkoutStep = 'cart'"><ArrowLeft :size="16" /> 買い物かごに戻る</button>
          <label>お名前<input v-model.trim="checkout.customerName" required autocomplete="name" /></label>
          <label>メールアドレス<input v-model.trim="checkout.email" required type="email" autocomplete="email" /></label>
          <label>お届け先住所<textarea v-model.trim="checkout.address" required rows="3" autocomplete="street-address"></textarea></label>
          <p class="checkout-note">お支払いは商品到着時の代金引換となります。</p>
          <div v-if="orderError" class="checkout-error" role="alert">{{ orderError }}</div>
          <div class="checkout-total"><span>お支払い合計</span><strong>{{ formatPrice(cartTotal) }}</strong></div>
          <button class="primary-button" type="submit" :disabled="submitting"><LoaderCircle v-if="submitting" class="spin" :size="18" />{{ submitting ? 'ご注文を確定しています' : '注文を確定する' }}</button>
        </form>

        <div v-else class="order-complete">
          <div class="complete-mark"><Check :size="32" /></div><p class="section-kicker">Order confirmed</p><h3>ご注文ありがとうございます</h3>
          <p>ご注文を承りました。確認メールを {{ checkout.email }} 宛にお送りします。</p>
          <dl><div><dt>注文番号</dt><dd>{{ confirmation.orderId }}</dd></div><div><dt>お支払い合計</dt><dd>{{ formatPrice(confirmation.total) }}</dd></div></dl>
          <button class="primary-button" type="button" @click="finishOrder">お買い物を続ける</button>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { ArrowLeft, ArrowRight, Check, LoaderCircle, Minus, Plus, ShoppingBag, Trash2, X } from '@lucide/vue';
import { getJson, postJson } from '../shared/api';

const wines = ref([]);
const loading = ref(true);
const errorMessage = ref('');
const selectedWine = ref(null);
const detailsDialog = ref(null);
const detailQuantity = ref(1);
const cart = ref([]);
const cartOpen = ref(false);
const checkoutStep = ref('cart');
const submitting = ref(false);
const orderError = ref('');
const confirmation = ref({});
const checkout = reactive({ customerName: '', email: '', address: '' });

const formatPrice = (value) => `¥${Number(value || 0).toLocaleString('ja-JP')}`;
const resolveImageUrl = (imagePath) => !imagePath ? '/assets/wines/placeholder.svg' : (/^https?:\/\//.test(imagePath) || imagePath.startsWith('/')) ? imagePath : `/assets/wines/${imagePath}`;
const onImageError = (event) => { event.target.src = '/assets/wines/placeholder.svg'; };
const wineDescription = (wine) => ({
  Red: '熟した果実の奥にスパイスと樽のニュアンス。静かな余韻が長く続く、食卓の主役となる一本です。',
  White: '瑞々しい果実味と凛とした酸が重なり、ミネラルを感じる端正な余韻へと続きます。',
  Sparkling: '繊細な泡立ちと華やかな香り。乾杯から食事の終わりまで寄り添う上品な味わいです。'
}[wine.category] || '造り手の哲学と土地の個性を映した、メゾン・ヴィーニュ選定の一本です。');

const cartItems = computed(() => cart.value.map((entry) => ({ ...entry, wine: wines.value.find((wine) => wine.id === entry.wineId) })).filter((entry) => entry.wine));
const cartCount = computed(() => cart.value.reduce((total, item) => total + item.quantity, 0));
const cartSubtotal = computed(() => cartItems.value.reduce((total, item) => total + item.wine.price * item.quantity, 0));
const shipping = computed(() => cartSubtotal.value >= 15000 ? 0 : 800);
const cartTotal = computed(() => cartSubtotal.value + shipping.value);
const drawerTitle = computed(() => checkoutStep.value === 'cart' ? '買い物かご' : checkoutStep.value === 'checkout' ? 'お届け先' : 'ご注文完了');

const showDetails = async (wine) => { selectedWine.value = wine; detailQuantity.value = 1; await nextTick(); detailsDialog.value?.showModal(); };
const closeDetails = () => detailsDialog.value?.close();
const closeDialogFromBackdrop = (event) => { if (event.target === detailsDialog.value) closeDetails(); };
const addToCart = (wine, quantity = 1) => {
  const existing = cart.value.find((item) => item.wineId === wine.id);
  if (existing) existing.quantity = Math.min(existing.quantity + quantity, wine.stock);
  else cart.value.push({ wineId: wine.id, quantity: Math.min(quantity, wine.stock) });
};
const addCardToCart = (wine) => { addToCart(wine); openCart(); };
const addDetailToCart = () => { addToCart(selectedWine.value, detailQuantity.value); closeDetails(); openCart(); };
const changeQuantity = (wineId, amount) => {
  const item = cart.value.find((entry) => entry.wineId === wineId);
  const wine = wines.value.find((entry) => entry.id === wineId);
  if (item && wine) item.quantity = Math.max(1, Math.min(item.quantity + amount, wine.stock));
};
const removeFromCart = (wineId) => { cart.value = cart.value.filter((item) => item.wineId !== wineId); };
const openCart = () => { cartOpen.value = true; };
const closeCart = () => { cartOpen.value = false; if (checkoutStep.value !== 'complete') checkoutStep.value = 'cart'; };

const submitOrder = async () => {
  submitting.value = true;
  orderError.value = '';
  try {
    confirmation.value = await postJson('/api/orders', { ...checkout, items: cart.value.map((item) => ({ wineId: item.wineId, quantity: item.quantity })) });
    cart.value = [];
    checkoutStep.value = 'complete';
    await loadWines();
  } catch {
    orderError.value = '注文を確定できませんでした。在庫状況をご確認のうえ、もう一度お試しください。';
  } finally {
    submitting.value = false;
  }
};
const finishOrder = () => { cartOpen.value = false; checkoutStep.value = 'cart'; confirmation.value = {}; };
const loadWines = async () => {
  loading.value = true;
  errorMessage.value = '';
  try {
    wines.value = await getJson('/api/wines');
    cart.value = cart.value.map((item) => {
      const wine = wines.value.find((entry) => entry.id === item.wineId);
      return wine ? { ...item, quantity: Math.min(item.quantity, wine.stock) } : null;
    }).filter((item) => item?.quantity > 0);
  } catch (error) {
    errorMessage.value = error.message;
  } finally {
    loading.value = false;
  }
};

watch(cart, (value) => localStorage.setItem('maison-vigne-cart', JSON.stringify(value)), { deep: true });
onMounted(() => {
  try { cart.value = JSON.parse(localStorage.getItem('maison-vigne-cart')) || []; } catch { cart.value = []; }
  loadWines();
});
</script>