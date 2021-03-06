(ns sparttt.scenes.home
  (:require
    [rum.core :as rum]
    [sparttt.stage :as stage]
    [sparttt.repository :as repository]))

(def scans-cursor (rum/cursor repository/repo :scans))

(rum/defc scene < rum/reactive []
  (let [scans (rum/react scans-cursor)]
    [:div
     [:h3 "Scan list"]

     [:table.card
      [:thead
       [:tr
        [:td {:col-span 2} "Number of scans: "] [:td (count scans)]]

       [:tr
        [:th "Seq"] [:th "Name"] [:th "ID"]]]
      [:tbody
       (for [{{seq :seq} :seq
              {:keys [id name]} :athlete} (rseq scans)]
         [:tr
          [:td seq] [:td name] [:td id]])]]]))

(defn scene-data []
  {:home 
   {:layout
    {:navbar {:index 0}
     :header {:title "Home"}
     :graphics {:icon :home}}

    :scene #'scene}})