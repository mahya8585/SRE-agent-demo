import { trackApiFailure } from './telemetry.js';

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081').replace(/\/$/, '');

const request = async (path, operation, options = {}) => {
  let response;
  try {
    const hasFormDataBody = typeof FormData !== 'undefined' && options.body instanceof FormData;
    response = await fetch(`${apiBaseUrl}${path}`, {
      cache: 'no-store',
      ...options,
      headers: {
        Accept: 'application/json',
        ...(options.body && !hasFormDataBody ? { 'Content-Type': 'application/json' } : {}),
        ...options.headers
      }
    });
  } catch (error) {
    trackApiFailure(operation, 'network_error');
    throw error;
  }

  if (!response.ok) {
    trackApiFailure(operation, response.status);
    throw new Error(`${operation} failed (${response.status})`);
  }

  if (response.status === 204) return null;
  return response.json();
};

export const getDashboardSummary = () => request('/api/admin/dashboard', 'GetDashboardSummary');

export const getOrders = () => request('/api/admin/orders', 'GetAdminOrders');

export const updateOrderStatus = (id, status) => request(`/api/admin/orders/${id}/status`, 'UpdateOrderStatus', {
  method: 'PUT',
  body: JSON.stringify({ status })
});

export const getInventory = () => request('/api/admin/inventory', 'GetAdminInventory');

export const createWine = (wine, image) => {
  if (!image) {
    return request('/api/admin/inventory', 'CreateWine', {
      method: 'POST',
      body: JSON.stringify(wine)
    });
  }

  const formData = new FormData();
  formData.append('wine', new Blob([JSON.stringify(wine)], { type: 'application/json' }));
  formData.append('image', image);
  return request('/api/admin/inventory', 'CreateWine', {
    method: 'POST',
    body: formData
  });
};

export const updateInventory = (id, stock, threshold) => request(`/api/admin/inventory/${id}`, 'UpdateInventory', {
  method: 'PUT',
  body: JSON.stringify({ stock, threshold })
});

export const getPurchaseOrders = () => request('/api/admin/purchase-orders', 'GetPurchaseOrders');

export const createPurchaseOrder = (wineId, quantity) => request('/api/admin/purchase-orders', 'CreatePurchaseOrder', {
  method: 'POST',
  body: JSON.stringify({ wineId, quantity })
});

export const receivePurchaseOrder = (id) => request(`/api/admin/purchase-orders/${id}/receive`, 'ReceivePurchaseOrder', {
  method: 'POST'
});