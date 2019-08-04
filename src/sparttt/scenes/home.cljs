(ns sparttt.scenes.home
  (:require
    [rum.core :as rum]
    [sparttt.repository :as repository]))

(rum/defc scene < rum/reactive []
  (let [scans (repository/list-scans)]
    [:div
     [:h2 "Overview"]

     [:table #_{:width "100%"}
      [:tr
       [:td {:col-span 2} "Number of scans: "] [:td (count scans)]]

      [:tr
       [:th "Seq"] [:th "Name"] [:th "ID"]]
      (for [{{seq :seq} :seq
             {:keys [id name]} :athlete} scans]
        [:tr
         [:td seq] [:td name] [:td id]])]]))