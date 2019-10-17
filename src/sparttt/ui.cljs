(ns sparttt.ui
  (:require
    [rum.core :as rum]
    [sparttt.stage :as stage]
    [sparttt.scenes.home]
    [sparttt.scenes.settings]
    [sparttt.scenes.scan]
    [sparttt.scenes.timer]
    [sparttt.scenes.visitors]
    [sparttt.scenes.consolidate]
    [sparttt.ui-elements :as ui-e]))

(rum/defc stage-summary-widget < rum/reactive
  []

  (let [stage (stage/active-stage)]
    [:div.widget.summary
     [:h3 "Summary"]
     [:p "Stage: " [:code (str (stage/active-stage-key))]]
     [:p "Header: " [:code (str (:header stage))]]
     [:p "Content: " [:code (str (:content stage))]]
     [:p "Footer: " [:code (str (:footer stage))]]]))

(rum/defc stage-switcher-widget < rum/reactive
  []
  (let [active-stage-key (stage/active-stage-key)
        is-active? (partial = active-stage-key)
        icon (fn [sk] (-> stage/stage-config sk :ui :icon))]

    [:div.widget.switcher
     (->>
       (keys stage/stage-config)
       (map
         (fn [stage-key]
           [:li
            {:on-click #(stage/activate-stage stage-key)
             :class [(when (is-active? stage-key) :active)]}
            (let [icon (icon stage-key)]
              (if icon [icon] (str stage-key)))])))]))

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
  ;; TODO think about a hook-strategy for controlling buttons in footer.
  (let [{{visible :visible} :footer} (stage/active-stage)]
    (when visible
      [:div
       ;[:div.foot.comfort [:div.button "hi"]]
       [:div.foot.comfort.shadow.with-gradient "Spartan Harriers"]])))

(rum/defc draw-stage < rum/reactive
  []
  (stage/register-scene :home sparttt.scenes.home/scene)
  (stage/register-scene :settings sparttt.scenes.settings/scene)
  (stage/register-scene :timer sparttt.scenes.timer/scene)
  (stage/register-scene :scan sparttt.scenes.scan/scene)
  (stage/register-scene :visitors sparttt.scenes.visitors/scene)
  (stage/register-scene :consolidate sparttt.scenes.consolidate/scene)

  (let []
    [:div.grid-container

     ;(stage-summary-widget)
     (stage-switcher-widget)

     (header-widget)
     (content-widget)
     (ui-e/video-modal)
     (footer-widget)]))
