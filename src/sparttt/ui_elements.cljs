(ns sparttt.ui-elements
  (:require
    [rum.core :as rum]
    [clojure.string :as str]))

(rum/defc button [label & [{:keys [icon] :as attributes}]]
  [:li.fas.fa-chart-bar]
  [:div.button (dissoc attributes :icon)
   (when icon
     [(keyword (str "i.fas.fa-" (name icon))) " "])
   (when icon " ") label])