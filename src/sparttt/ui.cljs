(ns sparttt.ui
  (:require
    [rum.core :as rum]
    [sparttt.stage :as stage]
    [sparttt.scenes.home]))

(rum/defc stage-summary-widget < rum/reactive
  []

  (let [stage (stage/active-stage)]
    [:div.widget.summary
     [:h3 "Summary"]
     [:p "Stage: " [:code (str (stage/active-stage-key))]]
     [:p "Header: " [:code (str (:header stage))]]
     [:p "Content: " [:code (str (:content stage))]]
     [:p "Footer: " [:code (str (:footer stage))]]]))

(rum/defc stage-switcher-widget
  []

  [:div.widget.switcher
   (->>
     (keys stage/stage-config)
     (map
       (fn [stage-key]
         [:button
          {:on-click #(stage/activate-stage stage-key)}
          (str stage-key)])))])

(rum/defc header-widget < rum/reactive
  []

  (let [{{title :title} :header} (stage/active-stage)]
    [:div.widget.header
     [:h3 title]]))

(rum/defc content-widget < rum/reactive
  []

  [:div.middle.scroll.comfort.flex
   (stage/scene-for (stage/active-stage-key))])

(rum/defc footer-widget < rum/reactive
  []

  (let [{{visible :visible} :footer} (stage/active-stage)]
    (when visible
      [:div.foot.comfort "Spartan Harriers"])))

(rum/defc draw-stage < rum/reactive
  []
  (stage/register-scene :home sparttt.scenes.home/scene)

  (let []
    [:div.grid-container
     (stage-summary-widget)
     (stage-switcher-widget)

     (header-widget)
     (content-widget)
     (footer-widget)]))