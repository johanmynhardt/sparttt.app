(ns sparttt.repository
  (:require
    [cljs.reader]))

(defonce repo
  (atom
    {:scans []
     :journal []}))

(def ^:dynamic storage-key "tt-repo")

(defn- write-to-local-storage [col-key val]
  (aset js/localStorage storage-key
    (swap! repo update col-key
      (fn [col] (conj col val)))))

(defn- read-from-local-storage [col-key]
  (-> js/localStorage
    (aget storage-key)
    cljs.reader/read-string
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

(defn save-scan [val]
  (write-to-local-storage :scans val))

(defn journal-append [val]
  (write-to-local-storage :journal val))

(defn list-scans []
  (read-from-local-storage :scans))
