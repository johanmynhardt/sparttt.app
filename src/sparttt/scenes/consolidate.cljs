(ns sparttt.scenes.consolidate
  (:require
   [rum.core :as rum]
   [cljs-time.core :as time]
   [cljs-time.coerce :as time.coerce]
   [cljs-time.format :as time.format]
   [clojure.string :as str]
   [sparttt.repository :as repository]
   [sparttt.ui-elements :as ui-e]))

(def storage-cursor (rum/cursor repository/repo :consolidate))
(def scans-cursor (rum/cursor-in repository/repo [:consolidate :scans]))
(def laps-cursor (rum/cursor-in repository/repo [:consolidate :laps]))
(def visitors-cursor (rum/cursor-in repository/repo [:consolidate :visitors]))

;;;; Time processing utilities =========================

(defn duration [genesis other-instant]
  (time.coerce/from-long
   (time/in-millis
    (time/interval
     genesis other-instant))))

(defn time-formatted [dt]
  (when dt
    (->
      (time.format/formatter "HH:mm:ss.SSS")
      (time.format/unparse (time.coerce/to-date-time dt)))))

;;;; CSV processing utilities ==========================

(defn extract-source-data
  [k]
  (->>
   @repository/repo
   :consolidate
   k
   :source
   (map :data)))

(defn process-laps
  [lap-data]
  (let [raw-map
        (->>
         lap-data
         (map (fn [data] (into (sorted-map) data)))
         (reduce merge)
         (reduce
          (fn [acc [seq timestamp :as next]]
            ;(println next)
            (cond
              (re-matches #"\d+" seq) (assoc acc (js/parseInt seq) timestamp)
              :else (assoc acc seq timestamp)))
          {}))

        genesis (time.format/parse (get raw-map 0))]
    (reduce
     (fn [acc [idx timestamp]]
       (cond
         (number? idx)
         (assoc acc idx
                (let [duration (duration genesis (time.format/parse (get raw-map idx)))]
                  {:duration duration
                   :duration-string (time-formatted duration)}))
         :else
         (assoc acc idx
                {:meta timestamp}))) 
     {:genesis 
      {:duration (duration genesis genesis)
       :duration-string (time-formatted (duration genesis genesis))}}
     raw-map)))

(comment
  (->>
   @repository/repo
   :consolidate
   :laps
   :source
   :data)

  (->>
   (extract-source-data :laps)
   #_(map (fn [data] (into (sorted-map) data)))
   #_(reduce merge))

  (time.format/parse "2019-10-15T07:33:58.626Z")

  (println  (process-laps (extract-source-data :laps)))
)


;;;; UI functions ===========================================

(defn files-from-event [e]
  (-> e (.-target) (.-files) array-seq))

(defn file-content-to-data
  [string-content]
  (->> (str/split-lines string-content)
       (map #(str/split % #","))
       vec))

(defn parse-csv-file
  [file on-success]
  (let [reader (js/FileReader.)
        filename (:name file)
        _ (set! 
           (.-onload
            reader)
           (fn [e]
             (let [data (file-content-to-data (-> e (.-target) (.-result)))]
               (on-success
                {:name (.-name file)
                 :data data}))))]
    
    (.readAsText reader file)))

(defn populate-consolidate-source
  [{:keys [name data] :as result}]
  (let [cursor
        (cond
          (str/starts-with? name "laps-data") laps-cursor
          (str/starts-with? name "scan-data") scans-cursor
          (str/starts-with? name "visitor-data") visitors-cursor
          :else (js/alert (str "Unsupported file found: " name)))]
    (when cursor
      (swap! cursor update :source (fn [coll] (conj coll result))))))

;;;; UI Scene + Components ================================
(rum/defc scene < rum/reactive []
  [:div "Select CSV files to consolidate into results."
   
   [:div.actions 
    (ui-e/button "Import Files"
                 {:icon :file-import
                  :on-click #(-> (js/document.querySelector "input#filechooser") (.click))})
    [:input#filechooser
     {:type "file"
      :hidden :hidden
      :accept :.csv
      :multiple "multiple"
      :on-change
      (fn [e]
        (doseq [file (files-from-event e)]
          (parse-csv-file file populate-consolidate-source)))}]]

   [:div
    [:h3 "Lap Data"]

    [:ul
     (for [s (:source (rum/react laps-cursor))]
       [:li (:name s) " (" (dec (count (:data s))) " rows)"])]]

   [:div
    [:h3 "Scan Data"]
    
    [:ul
     (for [s (:source (rum/react scans-cursor))]
       [:li (:name s) " (" (dec (count (:data s))) " rows)"])]]

   [:div
    [:h3 "Visitor Data"]

    [:ul
     (for [v (-> (rum/react visitors-cursor) :source)]
       [:li (:name v) " (" (dec (count (:data v))) " rows)"])]]

   [:div.actions
    (ui-e/button "Process" {:icon :sync})]])

