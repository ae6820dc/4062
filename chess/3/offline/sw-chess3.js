const PREFIX = 'chess-3:' + self.registration.scope;
const CACHE = PREFIX + 'v1';
const FILES = ['./sample3.html', './manifest-chess3.webmanifest', './icon-chess3-192.png', './icon-chess3-512.png'];
self.addEventListener('install', event => {
  event.waitUntil((async () => {
    const cache = await caches.open(CACHE);
    await cache.addAll(FILES);
    await self.skipWaiting();
  })());
});
self.addEventListener('activate', event => {
  event.waitUntil((async () => {
    for (const key of await caches.keys()) {
      if (key.startsWith(PREFIX) && key !== CACHE) await caches.delete(key);
    }
    await self.clients.claim();
  })());
});
self.addEventListener('fetch', event => {
  const url = new URL(event.request.url);
  if (event.request.method !== 'GET' || url.origin !== self.location.origin) return;
  const page = new URL('./sample3.html', self.registration.scope);
  if (event.request.mode === 'navigate' && url.pathname === page.pathname) {
    event.respondWith((async () => {
      const cache = await caches.open(CACHE);
      return await cache.match(page.href) || fetch(event.request);
    })());
  } else if (FILES.some(file => new URL(file, self.registration.scope).href === url.href)) {
    event.respondWith((async () => {
      const cache = await caches.open(CACHE);
      return await cache.match(event.request) || fetch(event.request);
    })());
  }
});
