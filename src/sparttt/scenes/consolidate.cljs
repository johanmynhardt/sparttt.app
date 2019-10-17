(ns sparttt.scenes.consolidate
  (:require
   [rum.core :as rum]
   [clojure.string :as str]
   [sparttt.repository :as repository]))

(def storage-cursor (rum/cursor repository/repo :consolidate))
(def scans-cursor (rum/cursor-in repository/repo [:consolidate :scans]))
(def laps-cursor (rum/cursor-in repository/repo [:consolidate :laps]))
(def visitors-cursor (rum/cursor-in repository/repo [:consolidate :visitors]))

(defn files-from-event [e]
  (-> e (.-target) (.-files) array-seq))

(defn file-content-to-data
  [string-content]
  (->> (str/split-lines string-content)
       (map #(str/split % #","))
       vec))

;(file-content-to-data "id,name,surname\n1,piet,pompies")

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



(rum/defc scene < rum/reactive []
  [:div "Add a file to analyse"
   [:div 
    [:input
     {:type "file"
      :multiple "multiple"
      :on-change
      (fn [e]
        (doseq [file (files-from-event e)]
          (parse-csv-file
           file
           (fn [{:keys [name data] :as result}]
             (cond
               (str/starts-with? name "laps-data")
               (swap! laps-cursor update :source (fn [laps] (conj laps result)))

               (str/starts-with? name "scan-data")
               (swap! scans-cursor update :source (fn [scans] (conj scans result)))

               (str/starts-with? name "visitor-data")
               (swap! visitors-cursor update :source (fn [visitors] (conj visitors result)))
               
               :else
               (js/alert (str "Unsupported file found: " name)))
             (println "file parsed: " result)))))}]]

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
       [:li (:name v) " (" (dec (count (:data v))) " rows)"])]]])
