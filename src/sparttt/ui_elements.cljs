(ns sparttt.ui-elements
  (:require
    [rum.core :as rum]))

(rum/defc button [label & [{:keys [on-click] :as attributes}]]
  [:div.button attributes label])