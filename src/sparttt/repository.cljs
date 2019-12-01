(ns sparttt.repository
  (:require
    [cljs.reader]))

(def non-clearing-keys
  [:camera-id
   :app-key
   :device-uuid])

(defonce empty-repo
  {:app-key ""
   :device-uuid (subs (str (random-uuid)) 0 8)
   :scans []
   :journal []
   :laps []
   :visitors []
   :laps-seq nil
   :camera-id nil
   :genesis nil
   :consolidate
   {:scans {:source []}
    :laps {:source []}
    :visitors {:source []}
    :results nil}})

(defonce repo
  (atom empty-repo))

(def ^:dynamic storage-key "tt-repo")

(defn- append-to-local-collection [col-key val]
  (aset js/localStorage storage-key
    (swap! repo update col-key
      (fn [col] (conj col val)))))

(defn- set-local-key [key value]
  (aset js/localStorage storage-key
    (swap! repo assoc key value)))


(defn- read-repo []
  (-> js/localStorage
    (aget storage-key)
    cljs.reader/read-string))

(defn- read-from-local-collection [col-key]
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
    (let [old-vals (select-keys @repo non-clearing-keys)])
    (aset js/localStorage storage-key
      (->>
        (select-keys @repo non-clearing-keys)
        (merge empty-repo)
        (reset! repo)))))

(defn save-scan [val]
  (append-to-local-collection :scans val))

(defn save-lap [val]
  (let [_ (when-not (= :genesis (:seq val))
            (swap! repo update-in [:laps-seq] inc))
        counter (:laps-seq @repo)]
    (println "counter: " counter)
    (append-to-local-collection :laps
      (if (= :genesis (:seq val))
        val
        (assoc val :seq counter)))))

(defn journal-append [val]
  (append-to-local-collection :journal val))

(defn save-camera-id [id]
  (set-local-key :camera-id id))

(defn camera-id []
  (:camera-id @repo))

(defn save-genesis [t]
  (set-local-key :genesis t)
  (save-lap
    {:seq :genesis
     :timestamp t}))

(defn genesis []
  (:genesis @repo))

(defn save-timer-state [state]
  (set-local-key :timer-state state))

(defn list-scans []
  (read-from-local-collection :scans))

(defn restore-from-local-storage []
  (reset! repo
    (or
      (read-repo)
      empty-repo)))

(defn save-visitor [visitor]
  (append-to-local-collection :visitors visitor))

(defn list-visitors []
  (read-from-local-collection :visitors))
