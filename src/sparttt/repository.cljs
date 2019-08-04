(ns sparttt.repository
  (:require
    [cljs.reader]))

(def empty-repo
  {:scans []
   :journal []})

(defonce repo
  (atom empty-repo))

(def ^:dynamic storage-key "tt-repo")

(defn- write-to-local-storage [col-key val]
  (aset js/localStorage storage-key
    (swap! repo update col-key
      (fn [col] (conj col val)))))

(defn- read-repo []
  (-> js/localStorage
    (aget storage-key)
    cljs.reader/read-string))

(defn- read-from-local-storage [col-key]
  (->
    (read-repo)
    (get col-key)))

(defn batch-write
  "Accepts a collection of entries {:col-key entry}.

  Eg: (batch-write
         [{:journal {:on-scan {:content 1}}},
          ...])"
  [records]
  (doseq [[col-key entry] (map first records)]
    (swap! repo update col-key
      (fn [col] (conj col entry))))

  (aset js/localStorage storage-key
    (deref repo)))

(defn purge
  "Clears the localStorage at the bound `storage-key` (tt-repo by default)"
  [& [confirmed]]
  (when confirmed
    (aset js/localStorage storage-key
      (reset! repo empty-repo))))

(defn save-scan [val]
  (write-to-local-storage :scans val))

(defn journal-append [val]
  (write-to-local-storage :journal val))

(defn list-scans []
  (read-from-local-storage :scans))

(defn restore-from-local-storage []
  (reset! repo
    (or
      (read-repo)
      empty-repo)))
