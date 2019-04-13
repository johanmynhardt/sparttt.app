(ns sparttt.ui
  (:require
    [rum.core :as rum]
    [sparttt.state :as state]))

(def menu-cursor (rum/cursor-in state/app-state [:menu]))
(def stage-cursor (rum/cursor-in state/app-state [:stage]))

(def stage-config
  {:home
   {:header {:visible true}
    :content {:visible true}
    :footer {:visible false}}

   :scan
   {:header {:visible true}
    :content {:visible true}
    :footer {:visible true :rows 2}}

   :timer
   {:header {:visible true}
    :content {:visible true}
    :footer {:visible true :rows 1}}})

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
        [:p "Stage: " (str current)]
        [:p "Header: " (str (:header stage))]
        [:p "Content: " (str (:content stage))]
        [:p "Footer: " (str (:footer stage))]])]))

(rum/defc stage-switcher
  []

  [:div.widget.switcher
   (into []
     (->>
       stage-config
       keys
       (map
         (fn [stage-key]
           [:a {:on-click #(activate-stage stage-key)}
            (str stage-key) " -- "]))))])

(rum/defc draw-stage < rum/reactive [app-state]
  (let [
        _ (println "stage-state: " stage-cursor)
        ]
    [:div.stage
     (stage-summary-widget)
     (stage-switcher)]))