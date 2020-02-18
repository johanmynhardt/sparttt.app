(ns sparttt.scenes.consolidate
  (:require
   [rum.core :as rum]
   [cljs-time.core :as time]
   [cljs-time.coerce :as time.coerce]
   [cljs-time.format :as time.format]
   [clojure.string :as str]
   [sparttt.aws :as aws]
   [sparttt.repository :as repository]
   [sparttt.ui-elements :as ui-e]
   [sparttt.stage :as stage]
   [sparttt.browser-assist :as browser-assist]))

(def storage-cursor (rum/cursor repository/repo :consolidate))
(def scans-cursor (rum/cursor-in repository/repo [:consolidate :scans]))
(def laps-cursor (rum/cursor-in repository/repo [:consolidate :laps]))
(def visitors-cursor (rum/cursor-in repository/repo [:consolidate :visitors]))
(def results-cursor (rum/cursor-in repository/repo [:consolidate :results]))
(def source-cursor (rum/cursor-in repository/repo [:config :consolidate-source]))

(def backend-key-cursor (rum/cursor repository/repo :event-key))

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
              :else acc))
          (sorted-map)))

        genesis (time.format/parse (get raw-map 0))]
    (->> 
     raw-map
     (reduce
      (fn [acc [idx timestamp]]
        (cond
          (number? idx)
          (assoc acc idx
                 (let [duration (duration genesis (time.format/parse (get raw-map idx)))]
                   {:duration duration
                    :duration-string (time-formatted duration)}))
          :else
          acc))
      (sorted-map)))))

(defn process-scans
  [scan-data]
  (let [raw-map
        (->>
         scan-data
         (map
          (fn [data]
            (->> 
             data
             (map
              (fn [[seq name id timestamp dist disc :as scan]]
                (println "processing scan: " scan)
                [(if (re-matches #"\d+" seq) (js/parseInt seq) seq)
                 {:id id
                  :seq (if (re-matches #"\d+" seq) (js/parseInt seq) seq)
                  :name name
                  :timestamp timestamp
                  :dist (when dist (str/replace dist #"\:" ""))
                  :disc (when disc (str/replace disc #"\:" ""))}]))
             (into {}))))
         (reduce merge))]
    raw-map))

(defn process-visitors
  [visitor-data]
  (let [raw-map
        (->>
         visitor-data
         (map
          (fn [data]
            (->>
             data
             (map
              (fn [[ident first-name last-name]]
                [ident (str first-name " " last-name)]))
             (into {}))))
         (reduce merge))]
    raw-map))

(defn zero-pad [n]
  (cond
    (< n 10)
    (str "00" n)

    (< n 100)
    (str "0" n)

    :else
    (str n)))

(defn collate-data
  [lap-data scan-data visitor-data]
  (->>
   lap-data
   (reduce
    (fn [acc [idx lap :as next]]
      (let [found-scan (get scan-data idx)]
        (assoc acc idx
               (merge 
                lap
                (select-keys found-scan [:id :name :seq :dist :disc]))))) 
    {})
   (map
    (fn [[idx {:keys [id] :as data} :as entry]]
      (let [visitor-name (get visitor-data id)]
        [idx (assoc data
                    :name
                    (or
                     visitor-name
                     (:name data)
                     "[ -- No Scan -- ]")
                    
                    :seqs
                    (cond
                      (nil? (:seq data))
                      (zero-pad idx)
                      
                      :else
                      (zero-pad (:seq data))))])))
   (map (fn [[_ v]] v))
   (sort-by :seqs)))

(defn extract-data [results]
  (->>
   results 
   (cons {:seqs "Position" :duration-string "Time" :name "Name" :dist "Dist" :disc "Disc"})
   (map (juxt :seqs :duration-string :name :dist :disc))))


(defn render-data-text [results]
  (->>
    (extract-data results)
    (map (partial str/join " __ "))
    (str/join \newline)))

(defn render-data-csv [results]
  (->>
    (extract-data results)
    (map (partial str/join ","))
    (str/join \newline)))

(def data-renderers
  {:txt #'render-data-text
   :csv #'render-data-csv})

(defn export-results [results & types]
  (doseq [t types]
    (browser-assist/initiate-download
     t ((get data-renderers t) results)
     (str "results-" (time.coerce/to-local-date (time/now))))))

(comment
  (export-results
   (collate-data 
    (process-laps (extract-source-data :laps))
    (process-scans (extract-source-data :scans))
    (process-visitors (extract-source-data :visitors)))
   :txt)
  
  (println  (process-scans (extract-source-data :scans)))
  
  (println "collated:" (with-out-str (cljs.pprint/pprint (collate-data 
    (process-laps (extract-source-data :laps))
    (process-scans (extract-source-data :scans))
    (process-visitors (extract-source-data :visitors))))))
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

(comment 
  (aws/fetch-consolidation-data
   "20191203-aoeu"
   (fn [result]
     (doseq [{:keys [filename content]} (:files result)]
       (populate-consolidate-source {:name filename :data (file-content-to-data content)})))))

(defn process-consolidation-data []
  (let [results
        (collate-data 
         (process-laps (extract-source-data :laps))
         (process-scans (extract-source-data :scans))
         (process-visitors (extract-source-data :visitors)))]
    (reset! results-cursor results)))

;;;; UI Scene + Components ================================
(rum/defc scene < rum/reactive []
  [:div
   
   [:div.card
    [:div.title [:i.fas.fa-file-import] " " "Data Source"
     (ui-e/help
      [:span
       "The consolidation process consists of selecting files
              from the local device or from the cloud."
       [:br]
       "The data is then processed and presented in a table with
              the data available as a CSV or text download." [:br]]
      {:icon :question-circle})]
    
    [:p "Choose origin of data:" ]
    (let [source (rum/react source-cursor)]
      [[:select.select
        {:value (or (rum/react source-cursor) "")
         :on-change
         (fn [e]
           (println "source change: " (reset! source-cursor (keyword (-> e .-target .-value)))))}
        [:option {:value :local} "File/s on device"]
        [:option {:value :cloud} "Sync using event key"]]

       (cond
         (= :cloud (rum/react source-cursor))
         [:div 
          (ui-e/input backend-key-cursor "Event Key")
          [:div.actions
           (ui-e/button
            "Process from Cloud"
            {:icon :cloud-download-alt
             :on-click
             (fn [e]
               (cond
                 (and @backend-key-cursor (not (empty? @backend-key-cursor)))
                 (do
                   (ui-e/show-toast 
                    [:div [:i.fas.fa-spinner] " " [:b "Fetching"]
                     [:p "Fetching data from cloud storage..."]]
                    
                    {:keep-open? true})
                   
                   (aws/fetch-consolidation-data
                    @backend-key-cursor
                    (fn [result]
                      (cond
                        (seq (:files result))
                        (do
                          (ui-e/show-toast
                           [:div [:b "Received Data"]
                            [:p "Processing results... " [:li.fas.fa-spinner]]])
                          (doseq [{:keys [filename content]} (:files result)]
                            (populate-consolidate-source
                             {:name filename
                              :data (file-content-to-data content)}))
                          (process-consolidation-data)
                          (ui-e/show-toast
                           [:div [:i.fas.fa-check] " " [:b "Processing Complete"]
                            [:p "The results are processed and available as CSV and Text."
                             [:br]
                             "Use the relevant buttons to get the data."]]))
                        
                        :else
                        (ui-e/show-toast [:div [:b "No Files"] [:p "0 files returned from cloud storage."]])))))
                 
                 :else
                 (ui-e/show-toast
                  [:div
                   [:b "Event Key Required"]
                   [:p "Please provide the event key to process."]]
                  {:keep-open? true})))})]]

         :else
         [:div.actions
          (ui-e/button
           "Select Files"
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
                (parse-csv-file file populate-consolidate-source)))}]])])]

   [:div.card
    [:div.title [:i.fas.fa-file] " " "Sources"]

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
        [:li (:name v) " (" (dec (count (:data v))) " rows)"])]]]

   (when (not= :cloud (rum/react source-cursor))
     [:div.actions
      (ui-e/button "Process" {:icon :sync :on-click process-consolidation-data})])

   (when (rum/react results-cursor)
     [:div
      [:h3 "Results"]

      [:div.actions
       (ui-e/button
        "CSV"
        {:icon :file-csv
         :on-click
         #(export-results
           (collate-data 
            (process-laps (extract-source-data :laps))
            (process-scans (extract-source-data :scans))
            (process-visitors (extract-source-data :visitors)))
           :csv)})

       (ui-e/button
        "Text"
        {:icon :file-alt
         :on-click
         #(export-results
           (collate-data 
            (process-laps (extract-source-data :laps))
            (process-scans (extract-source-data :scans))
            (process-visitors (extract-source-data :visitors)))
           :txt)})]

      [:table
       [:thead
        [:tr
         [:th "Position"] [:th "Time"] [:th "Name"]]]

       [:tbody
        (for [{:keys [seqs duration-string name]} @results-cursor]
          [:tr
           [:td (or seqs "--")]
           [:td (or duration-string "--")]
           [:td (or name "--")]])]]])])


(defn scene-data []
  {:consolidate
   {:layout
    {:navbar {:index 5}
     :header {:title "Consolidate Data"}
     :footer {:visibility :hide}
     :graphics {:icon :sort-amount-down}}

    :scene #'scene}})
