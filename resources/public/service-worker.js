/*
 Copyright 2016 Google Inc. All Rights Reserved.
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
     http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

// Names of the two caches used in this version of the service worker.
// Change to v2, etc. when you update any of the local resources, which will
// in turn trigger the install event again.
const PRECACHE = 'precache-v-79a18ac';
const RUNTIME = 'runtime';

// A list of local resources we always want to be cached.
const PRECACHE_URLS = [
  '/qrv2/',
  '/qrv2/index.html',
  '/qrv2/css/style.css',
  '/qrv2/css/fontawesome/5.11.2/css/all.css',
  '/qrv2/cljs-out/dev-main.js',
  '/qrv2/css/fontawesome/5.11.2/webfonts/fa-solid-900.woff2',
  '/qrv2/css/fonts/RobotoMono-Regular.ttf',
  '/qrv2/css/fonts/TitilliumWeb-Regular.ttf',
  '/qrv2/favicon/spartan-harriers-logo-150x150.png'
];


// The install handler takes care of precaching the resources we always need.
self.addEventListener('install', event => {
  console.info('installing service worker');
  event.waitUntil(
    caches.open(PRECACHE)
      .then(cache => cache.addAll(PRECACHE_URLS))
      .then(self.skipWaiting())
      .catch(err => {
        console.error('error opening and adding all caches', err);
      })
  );
});


// The activate handler takes care of cleaning up old caches.
self.addEventListener('activate', event => {
  console.info('activating service worker');
  const currentCaches = [PRECACHE, RUNTIME];
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return cacheNames.filter(cacheName => !currentCaches.includes(cacheName));
    }).then(cachesToDelete => {
      return Promise.all(cachesToDelete.map(cacheToDelete => {
        console.info('purging old cache ', cacheToDelete);
        return caches.delete(cacheToDelete);
      }));
    }).then(() => self.clients.claim())
  );

self.clients.matchAll().then(clients => {
  //clients.forEach(client => console.info('client: ', client));

  //console.info('posting message to client...');
  clients.forEach(client => client.postMessage({cacheVersion: PRECACHE}));

  clients.forEach(client => {
    new Promise(function(resolve, reject) {
      var channel = new MessageChannel();
      channel.port1.onmessage = function(e) {
        if (e.data.error) {
          reject(e.data.error);
        } else {
          resolve(e.data);
        }
      };
    });
  });
});

});


// The fetch handler serves responses for same-origin resources from a cache.
// If no response is found, it populates the runtime cache with the response
// from the network before returning it to the page.
self.addEventListener('fetch', event => {
  console.info('request worker intercepting fetch: ', event.request.url);
  // Skip cross-origin requests, like those for Google Analytics.

  if (event.request.url.indexOf('init-download') > -1) {
    event.respondWith(event.request.formData().then(function (formdata){
      var filename = formdata.get("filename");
      var body = formdata.get("filebody");
      var response = new Response(body);
      response.headers.append('Content-Disposition', 'attachment; filename="' + filename + '"');
      response.headers.append('Content-Type', 'application/octet-stream');
      return response;
    }));
 
  }

  if (null && event.request.url.startsWith(self.location.origin)) {
    console.info('requesting url from service-worker: ', event.request.url);
    event.respondWith(
      caches.match(event.request).then(cachedResponse => {
        if (cachedResponse) {
	  console.info('returning cached response');
          return cachedResponse;
        }

        return caches.open(RUNTIME).then(cache => {
	  console.info('fetching uncached resource: ', event.request.url);
          return fetch(event.request).then(response => {
            // Put a copy of the response in the runtime cache.
            return cache.put(event.request, response.clone()).then(() => {
              return response;
            });
          });
        });
      })
    );
  }
});

