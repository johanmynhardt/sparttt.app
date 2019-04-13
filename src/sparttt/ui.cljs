(ns sparttt.ui
  (:require
    [rum.core :as rum]
    [sparttt.state :as state]))

(def menu-cursor (rum/cursor-in state/app-state [:menu]))
(def stage-cursor (rum/cursor-in state/app-state [:stage]))

(def stage-config
  {:home
   {:header {:visible true :title "Home"}
    :content {:visible true}
    :footer {:visible false}}

   :scan
   {:header {:visible true :title "Capture QR"}
    :content {:visible true}
    :footer {:visible true :rows 2}}

   :timer
   {:header {:visible true :title "Timer"}
    :content {:visible true}
    :footer {:visible true :rows 1}}

   :settings
   {:header {:visible true :title "Settings"}
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

(defn row
  [& children]
  [:div.row children])

(rum/defc menu < rum/reactive
  []

  (let [expanded (-> menu-cursor rum/react :expanded)]
    (print (str "menu-state: " (deref menu-cursor)))
    [:div.menu-wrapper
     [:h1 "Menu"]
     [:code (pr-str (rum/react menu-cursor))]
     [:p {:hidden (-> expanded not)} "not hidden"]

     [:button
      {:on-click
       (fn []

         (swap! menu-cursor
           assoc :expanded (-> @menu-cursor :expanded not)))}
      "toggle"]]))

(rum/defc body-wrapper < rum/reactive
  [& elements]
  [:div.body-wrapper
   elements])

(rum/defc footer < rum/reactive
  []
  (let []
    (row
      [:p "footer"])))

(rum/defc stage-summary-widget < rum/reactive
  []

  (let [current (:current (rum/react stage-cursor))
        stage (get stage-config current)]
    [:div.widget.summary
     (into []
       [[:h3 "Summary"]
        [:p "Stage: " [:code (str current)]]
        [:p "Header: " [:code (str (:header stage))]]
        [:p "Content: " [:code (str (:content stage))]]
        [:p "Footer: " [:code (str (:footer stage))]]])]))

(rum/defc stage-switcher-widget
  []

  [:div.widget.switcher
   (into []
     (->>
       stage-config
       keys
       (map
         (fn [stage-key]
           [:button {:on-click #(activate-stage stage-key)}
            (str stage-key)]))))])

(rum/defc header-widget < rum/reactive
  []
  (let [stage
        (->> stage-cursor
          (rum/react)
          :current
          (get stage-config))]
    [:div.widget.header
     (into []
       [[:h3 (-> stage :header :title)]])]))

(rum/defc content-widget < rum/reactive
  []
  (into []
    [[:div.middle.scroll.comfort.flex
      [:p "oh hi"]]]))

(rum/defc footer-widget < rum/reactive
  []

  (into []
    [[:div.foot.comfort "Spartan Harriers"]]))

(rum/defc draw-stage < rum/reactive [app-state]
  (let []
    [:div.grid-container
     (stage-summary-widget)
     (stage-switcher-widget)

     (header-widget)
     (content-widget)
     (footer-widget)]))