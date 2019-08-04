(ns sparttt.scenes.settings
  (:require
    [rum.core :as rum]
    [clojure.string :as str]
    [Instascan]
    [Instascan.Camera]
    [clojure.string :as str]
    [sparttt.ui-elements :as ui]
    [sparttt.repository :as repository]))

(defonce cameras (atom nil))
(defonce selected-camera (atom nil))

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
             :name (.-name cam)}))))))

(rum/defc scene < rum/reactive []
  (let [cameras (rum/react cameras)
        cam (rum/react selected-camera)]
    (println "cameras: " cameras)

    [:div

     [:div.card
      [:div.title [:li.fas.fa-camera] " " "Camera"]
      [:div.content
       [:select
        {:on-change
         (fn [e]
           (let [v (-> e (.-target) (.-value))]
             (reset! selected-camera
               (if (str/blank? v) nil
                 (str/trim v)))))

         :value cam}
        (conj
          (map
            (fn [cam]
              [:option {:value (:id cam)} (:name cam)])
            cameras)
          [:option {:value ""} "Please select"])]]]

     [:div.card
      [:div.title [:li.fas.fa-database] " " "Data"]
      [:div.content
       [:ul
        [:li "list"]
        [:li "export csv"]
        [:li "export journal"]]]]

     [:div.card
      [:div.title [:li.fas.fa-chart-bar] " " "Session"]
      [:div.content
       (ui/button "Purge Data"
         {:on-click #(repository/purge (js/confirm "All the data will be wiped! Are you sure?"))})
       [:ul
        [:li "purge data"]]]]]))