(ns sparttt.ui-elements
  (:require
    [rum.core :as rum]
    [sparttt.state]
    [clojure.string :as str]))

(rum/defc button [label & [{:keys [icon] :as attributes}]]
  #_[:li.fas.fa-chart-bar]
  [:button.button (dissoc attributes :icon)
   (when icon
     [(keyword (str "i.fas.fa-" (name icon))) " "])
   (when icon " ") label])

(rum/defc icon-button [icon on-click]
  [:button.icon-button
   {:on-click on-click}
   [(keyword (str "i.fas.fa-" (name icon)))]])

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

(def toast-cursor
  (rum/cursor-in sparttt.state/app-state [:toast]))

(rum/defc toast < rum/reactive []
  (let [toast-data (rum/react toast-cursor)]
    (when (= :show (:visibility toast-data))
      [[:div.toast.scale-up-center
        (or (:text toast-data) "no text set.")
        (when-not (:timeout? toast-data)
          (button
           (or (:button-text toast-data) "OK")
           {:on-click
            (cond
              (:dismiss-fn toast-data)
              (do
                (fn [e]
                  ((:dismiss-fn toast-data))
                  ((:fn toast-data))))
              :else (:fn toast-data))}))]])))

(defn show-toast [text & [{:keys [timeout visibility keep-open? dismiss-fn] :or {keep-open? false}}]]
  (swap! toast-cursor assoc
         :text text
         :visibility :show
         :timeout? (some? timeout)
         :dismiss-fn dismiss-fn)
  
  (when (and timeout (not keep-open?))
    (js/setTimeout
     #(swap! toast-cursor assoc :visibility :hide)
     timeout)))

(comment
  (show-toast "hello2" {:timeout 2000 :keep-open? false})

  (show-toast [:span "oh hi :)" [:br] "xxx"] {:keep-open? false})
)

(rum/defc help [content & [{:keys [title icon]}]]
  (icon-button (or icon :question)
      (fn [_]
        (show-toast
         (into
          [:span [:i.fas.fa-question] " " [:b (or title "Help")] [:br]
           content])
         {:keep-open? true})))
  )
