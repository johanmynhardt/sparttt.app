(ns sparttt.scenes.settings
  (:require
    [cljs-time.coerce :as time.coerce]
    [cljs-time.core :as time]
    [clojure.string :as str]
    [Instascan]
    [Instascan.Camera]
    [rum.core :as rum]
    [sparttt.ui-elements :as ui]
    [sparttt.browser-assist :as browser-assist]
    [sparttt.repository :as repository]))

(defonce cameras (atom nil))
(defonce selected-camera (atom nil))
(add-watch selected-camera :selected-camera
  (fn [k r o n]
    (println "watch: selected camera: " n)))

(defonce selected-camera-inst (atom nil))

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

         :value (or (repository/camera-id) cam)}
        (conj
          (map
            (fn [cam]
              [:option {:value (:id cam)} (:name cam)])
            cameras)
          [:option {:value ""} "Please select"])]]]

     [:div.card
      [:div.title [:li.fas.fa-database] " " "Data"]
      [:div.content

       [:div [:p "Export Actions"]
        (ui/button "Repository"
          {:icon :database
           :on-click
           #(browser-assist/initiate-download :edn @repository/repo
              (str "repo-data-" (time.coerce/to-local-date (time/now))))})

        (ui/button "Scans"
          {:icon :address-card
           :on-click #()})]

       [:div [:p "Destructive Actions"]

        (ui/button "Purge Data"
          {:icon :trash
           :class [:warn]
           :on-click
           #(repository/purge
              (js/confirm "All the data will be wiped! Are you sure?"))})]]]

     #_[:div.card
      [:div.title [:li.fas.fa-chart-bar] " " "Session"]
      [:div.content
       [:ul
        [:li "list"]
        [:li "export csv"]
        [:li "export journal"]]]]]))