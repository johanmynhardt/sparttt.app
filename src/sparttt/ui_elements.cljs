(ns sparttt.ui-elements
  (:require
    [rum.core :as rum]
    [clojure.string :as str]))

(rum/defc button [label & [{:keys [icon] :as attributes}]]
  [:li.fas.fa-chart-bar]
  [:button.button (dissoc attributes :icon)
   (when icon
     [(keyword (str "i.fas.fa-" (name icon))) " "])
   (when icon " ") label])

(defn get-video-preview []
  (.querySelector js/document "#preview"))

(defn get-scanner []
  (cond
    (nil? (.-msc js/document))
    (let [preview (get-video-preview)]
      (let [scnr
            (new js/Instascan.Scanner.
              (clj->js
                {:video preview
                 :mirror false
                 :refactoryPeriod 15000
                 :scanPeriod 10}))]
        (set! (.-msc js/document) scnr)))
    :else
    (.-msc js/document)))

(defn get-video-modal []
  (.querySelector js/document "div.video-modal"))

(defn show-video-modal []
  (->
    (get-video-modal)
    (.removeAttribute "hidden")))

(defn hide-video-modal []
  (->
    (get-video-modal)
    (.setAttribute  "hidden" true)))

(rum/defc video-preview []
  [:video#preview])

(rum/defc video-modal []
  [:div.video-modal.shadow {:hidden true}
   (video-preview)
   [:br]
   (button "Stop"
     {:icon :stop
      :on-click
      #(let [scanner (get-scanner)]
         (println "Stopping scanner " scanner)
         (hide-video-modal)
         (.stop scanner))})])

(rum/defc input [cursor label & [{:keys [type placeholder] :as options}]]
  [[:div.field
    [:label (or label "(no label)")]
    [:input
     (merge
      options
      {:type (or type "text")
       :value @cursor
       :placeholder placeholder
       :on-change (fn [e] (reset! cursor (-> e (.-target) (.-value))))})]]])

