(ns sparttt.scenes.settings
  (:require
    [rum.core :as rum]
    [instascan]
    [qrcode]))


(defn qr [id text]
  (let [e (js/QRCode. id)]
    (-> e (.makeCode text))))

#_(->
  (.getCameras instascan/Camera)
  (.then (fn [cameras] (println "got cameras: " cameras))))



(rum/defc scene < rum/reactive []

  [:div

   [:div.card
    [:div.title [:li.fas.fa-camera] " " "Camera"]
    [:div.content
     [:select
      [:option "x"]]]]

   [:div.card
    [:div.title [:li.fas.fa-database] " " "Data"]
    [:div.content
     [:ul
      [:li "list"]
      [:li "export csv"]
      [:li "export journal"]]]]

   [:div.card
    [:div.title [:li.fas.fa-chart-bar] " " "Session"]
    [:div.content
     [:ul
      [:li "purge data"]]]]

   [:div#foo
    [:span
     {:style {:display "none"}}
     (js/setTimeout (fn [] (qr "foo" "hello")) 100)]]])