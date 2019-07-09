(ns sparttt.scenes.home
  (:require
    [rum.core :as rum]
    [sparttt.repository :as repository]))

(rum/defc scene < rum/reactive []
  (let [scans (repository/list-scans)]
    [:div "Home stuff here..."

     [:p "scans: " (count scans)]

     [:ul
      (for [scan scans]
        [:li [:code (with-out-str (cljs.pprint/pprint scan))]])]]))