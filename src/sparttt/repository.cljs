(ns sparttt.repository
  (:require
    [cljs.reader]
    [clojure.data]
    [sparttt.browser-assist :as browser-assist]))

(def empty-repo
  {:scans []
   :journal []
   :laps []
   :laps-seq nil
   :camera-id nil
   :genesis nil})

(defonce repo
  (atom empty-repo))

(def lap-queue (atom nil))

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
  (js/console.info (str "writing records: " records))
  (doseq [[col-key entry] records]
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

;; to-write: {:a nil, :to-write [{:seq :genesis, :timestamp #inst "2019-08-22T23:03:33.615-00:00"}], :both nil}

(comment



  )

(js/setInterval
  #(do
     (let [ls-laps (->> (read-from-local-collection :laps) set)
           laps (->> (seq (:laps @repo)) set)

           _ (js/console.info (with-out-str (cljs.pprint/pprint {:ls-laps ls-laps :laps laps})))

           [a to-write both] (clojure.data/diff ls-laps laps)]
       (js/console.info (str "to-write: " {:a a :to-write to-write :both both}))
       (when (seq to-write)
         (batch-write
           (map
             (fn [r] [:laps r])
             to-write)))))
  5000)

(defn save-lap [val]
  (let [_ (when-not (= :genesis (:seq val))
            (swap! repo update-in [:laps-seq] inc))
        counter (:laps-seq @repo)

        v
        (if (= :genesis (:seq val))
          val
          (assoc val :seq counter))]

    (browser-assist/vibrate 200)
    (swap! repo update :laps
      (fn [col] (conj col v)))))

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
