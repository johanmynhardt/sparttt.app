(ns sparttt.stage-conductor
  (:require
    [rum.core :as rum]
    [sparttt.stage :as stage]
    [sparttt.ui-elements :as ui-e]))

(rum/defc stage-switcher-widget < rum/reactive
  []
  (let [active-stage-key (stage/active-stage-key)
        is-active? (partial = active-stage-key)
        icon
        (fn [sk]
          (stage/icon
           (get-in
            @stage/scene-cursor
            [sk :layout :graphics :icon])))]

    [:div.widget.switcher
     (->>
       (keys @stage/scene-cursor)
       (map
         (fn [stage-key]
           [:li
            {:on-click #(stage/activate-stage stage-key)
             :class [(when (is-active? stage-key) :active)]}
            (let [{:keys [icon]} (icon stage-key)]
              (if icon [icon] (str stage-key)))])))]))

(rum/defc header-widget < rum/reactive
  []
  [:div.widget.header
   [:h3 (get-in (stage/active-stage) [:layout :header :title])]])

(rum/defc content-widget < rum/reactive
  []

  [:div.middle.scroll.comfort.flex
   (stage/scene-for
    (stage/active-stage-key))])

(rum/defc footer-widget < rum/reactive
  []
  (when
      (-> (stage/active-stage)
          (get-in [:layout :footer :visibility])
          (= :show))
      [:div
       [:div.foot.comfort.shadow.with-gradient
        "Spartan Harriers"]]))

(rum/defc draw-stage < rum/reactive
  []

  (let []
    [:div.grid-container

     (stage-switcher-widget)

     (header-widget)
     (content-widget)
     (ui-e/video-modal)
     (footer-widget)]))
