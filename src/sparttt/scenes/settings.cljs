(ns sparttt.scenes.settings
  (:require
    [cljs-time.coerce :as time.coerce]
    [cljs-time.core :as time]
    [clojure.string :as str]
    [goog.functions :as gfns]
    [Instascan]
    [Instascan.Camera]
    [rum.core :as rum]
    [sparttt.aws :as aws]
    [sparttt.ui-elements :as ui]
    [sparttt.browser-assist :as browser-assist]
    [sparttt.repository :as repository]
    [sparttt.stage :as stage]))
 
(defonce cameras (atom nil))
(defonce selected-camera (atom nil))
(add-watch selected-camera :selected-camera
  (fn [k r o n]
    (println "watch: selected camera: " n)))

(defonce selected-camera-inst (atom nil))

(defonce app-key (rum/cursor-in repository/repo [:app-key]))
(add-watch
 app-key :app-key
 (->
  (fn [k r o n]
    (when (not= o n)
      (println "app-key changed: " o " -> " n)
      (repository/set-local-key :app-key n)))
  (goog.functions/debounce 500)))

(when (or (nil? @cameras) (not (seq @cameras)))
  (->
    (Instascan.Camera/getCameras)
    (.then
      (fn [cms]
        (doseq [cam cms]
          (when cam
            (println "cam.id: " (.-id cam))
            (println "cam.name: " (.-name cam))
            (println "----------------")
            (swap! cameras conj
              {:id (.-id cam)
               :name (.-name cam)})
            (when (= (.-id cam) (repository/camera-id))
              (reset! selected-camera-inst cam))))))))

(defn as-csv [extract transform headers data]
  (->>
   data
   (map extract)
   (map transform)
   (map #(str/join "," %))
   (cons (str/join "," headers))
   (str/join \newline)))

(defn laps-csv []
  (->>
   (:laps @repository/repo)
   (as-csv
    (juxt :seq :timestamp)
    (fn [[seq timestamp]]
      [(if (= :genesis seq) 0 seq) (time.coerce/to-string timestamp)])
    ["lap" "timestamp"])))

(defn scans-csv []
  (->>
   (:scans @repository/repo)
   (as-csv
    (juxt :seq :athlete)
    (fn [[{:keys [seq]} {:keys [name id tstamp]}]]
      [(if (= :genesis seq) 0 seq) name id (time.coerce/to-string tstamp)])
    ["seq" "name" "id" "timestamp"])))

(defn visitors-csv []
  (->>
   (:visitors @repository/repo)
   (as-csv
    (juxt :ident :first-name :last-name)
    (fn [[ident first-name last-name]]
      [ident first-name last-name])
    ["ident" "first-name" "last-name"])))

(defn push-remote
  "Push Lap, Scan or Visitor data to backend."
  []
  (let [date-part (time.coerce/to-local-date (time/now))
        {:keys [app-key device-uuid]} @repository/repo
        eid (when (some? (seq app-key)) (str date-part "-" app-key))
        filename (fn [base-name & [ext]] (str base-name "-" device-uuid (or ext ".csv")))

        data-to-send
        (->>
         [[:scans (filename "scan-data") (scans-csv)]
          [:laps (filename "laps-data") (laps-csv)]
          [:visitors (filename "visitor-data") (visitors-csv)]          ]
         (filter
          (fn [[k _ d]]
            (let [line-count (count (str/split-lines d))]
              (println k "line-count: " line-count)
              (> line-count 1))))
         (cons [:repo (filename "repository-data" ".edn") (str @repository/repo)]))

        result-atom (atom [])]

    (cond
      (and eid (seq data-to-send))
      (do
        (ui/show-toast [:span "Submitted request/s." [:br] "Waiting for results..."] {:keep-open? true})
        (doseq [[k fname d] data-to-send]
          (aws/post-event-data
           eid fname d 
           (fn [m]
             (swap! result-atom conj k)
             (ui/show-toast
              [:span "Got result for " (str @result-atom) [:br]]
              {:keep-open? true})))))

      :else
      (ui/show-toast [:span [:b "Warning:"] " " "No results to submit." [:br]
                      "Is a key set and are there results captured?"] {:keep-open? true}))))

(rum/defc scene < rum/reactive []
  (let [cameras (rum/react cameras)
        cam (rum/react selected-camera)]
    (println "cameras: " cameras)

    [:div

     [:div.card
      [:div.title [:li.fas.fa-camera] " " "Camera"]
      [:div.content
       [:select.select
        {:on-change
         (fn [e]
           (let [v (-> e (.-target) (.-value))
                 selected
                 (reset! selected-camera
                   (if (str/blank? v)
                     ""
                     (str/trim v)))]
             (when (pos? (count selected))
               (->
                 (Instascan.Camera/getCameras)
                 (.then
                   (fn [cms]
                     (let [cam
                           (first (filter #(= selected (.-id %)) cms))]
                       (println "setting in atom: " (.-name cam))
                       (when cam
                         (println "cam.id: " (.-id cam))
                         (println "cam.name: " (.-name cam))
                         (repository/save-camera-id (.-id cam))
                         (reset! selected-camera-inst cam)))))))))

         :value (or (repository/camera-id) cam "")}
        (conj
          (map
            (fn [cam]
              [:option {:value (:id cam)} (:name cam)])
            cameras)
          [:option {:value ""} "Please select"])]]]

     [:div.card
      [:div.title [:li.fas.fa-database] " " "Data"]
      [:div.content

       [:div [:p "Export:"]
        (ui/button "Repository"
          {:icon :database
           :on-click
           #(browser-assist/initiate-download :edn @repository/repo
              (str "repo-data-" (time.coerce/to-local-date (time/now))))})

        (ui/button "Laps"
          {:icon :list
           :on-click
           #(browser-assist/initiate-download
             :csv (laps-csv)
             (str "laps-data-" (time.coerce/to-local-date (time/now))))})

        (ui/button "Scans"
          {:icon :address-card
           :on-click
           #(browser-assist/initiate-download
             :csv (scans-csv)
             (str "scan-data-" (time.coerce/to-local-date (time/now))))})

        (ui/button "Visitors"
          {:icon :handshake
           :on-click
           #(browser-assist/initiate-form-post-download
             :csv
             (visitors-csv)
              (str "visitor-data-" (time.coerce/to-local-date (time/now))))})]

       [:div [:p "Backend Synchronization"]
        (ui/input app-key "Key") [:br]
        (ui/button
         "Sync"
         {:icon :cloud-upload-alt
          :on-click push-remote})]


       [:div [:p "Clean up:"]
        (ui/button "Purge Data"
          {:icon :trash
           :class [:warn]
           :on-click
           #(repository/purge
              (js/confirm "All the data will be wiped! Are you sure?"))})]]]]))

(stage/register-scene
 (stage/configure-scene
  :settings
  {:layout
   {:navbar {:index 3}
    :header {:title "Settings"}
    :graphics {:icon :cog}}
   
   :scene #'scene}))
