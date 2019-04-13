(ns sparttt.ui
  (:require
    [rum.core :as rum]
    [sparttt.state :as state]))

(def stage-cursor
  (rum/cursor-in state/app-state [:stage]))

(def stage-config
  {:home
   {:header
    {:visible true
     :title "Home"}
    :content {:visible true}
    :footer {:visible true}}

   :scan
   {:header
    {:visible true
     :title "Capture QR"}
    :content {:visible true}
    :footer {:visible false :rows 2}}

   :timer
   {:header
    {:visible true
     :title "Timer"}
    :content {:visible true}
    :footer {:visible false}}

   :settings
   {:header
    {:visible true
     :title "Settings"}
    :content {:visible true}
    :footer {:visible true}}})

(defn activate-stage
  [stage-key]

  (let [result
        {:stage-config
         {:keys (keys stage-config)
          :current (:current (deref stage-cursor))}}]

    (cond
      (get stage-config stage-key)
      (do
        (println "activate-stage request applied: " (assoc result :new-key stage-key))
        (swap! stage-cursor assoc :current stage-key))

      :else
      (println "activate-stage request failed. (no key for" stage-key ")"))))

(rum/defc stage-summary-widget < rum/reactive
  []

  (let [current (:current (rum/react stage-cursor))
        stage (get stage-config current)]
    [:div.widget.summary
     [:h3 "Summary"]
     [:p "Stage: " [:code (str current)]]
     [:p "Header: " [:code (str (:header stage))]]
     [:p "Content: " [:code (str (:content stage))]]
     [:p "Footer: " [:code (str (:footer stage))]]]))

(rum/defc stage-switcher-widget
  []

  [:div.widget.switcher
   (->>
     (keys stage-config)
     (map
       (fn [stage-key]
         [:button
          {:on-click #(activate-stage stage-key)}
          (str stage-key)])))])

(defn get-stage []
  (let [stage-key (:current (rum/react stage-cursor))
        stage (get stage-config stage-key)]
    stage))

(rum/defc header-widget < rum/reactive
  []

  (let [{{title :title} :header} (get-stage)]
    [:div.widget.header
     [:h3 title]]))

(rum/defc content-widget < rum/reactive
  []

  [:div.middle.scroll.comfort.flex
   [:p "oh hi"]])

(rum/defc footer-widget < rum/reactive
  []

  (let [{{visible :visible} :footer} (get-stage)]
    (when visible
      [:div.foot.comfort "Spartan Harriers"])))

(rum/defc draw-stage < rum/reactive
  []
  (let []
    [:div.grid-container
     (stage-summary-widget)
     (stage-switcher-widget)

     (header-widget)
     (content-widget)
     (footer-widget)]))