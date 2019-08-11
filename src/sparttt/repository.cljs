(ns sparttt.repository
  (:require
    [cljs.reader]))

(def empty-repo
  {:scans []
   :journal []
   :laps []
   :camera-id nil
   :genesis nil})

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
    (aset js/localStorage storage-key
      (->>
        (:camera-id @repo)
        (assoc empty-repo :camera-id )
        (reset! repo)))))

(defn save-scan [val]
  (append-to-local-collection :scans val))

(defn save-lap [val]
  (append-to-local-collection :laps val))

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
