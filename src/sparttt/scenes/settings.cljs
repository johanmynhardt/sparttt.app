(ns sparttt.scenes.settings
  (:require
    [rum.core :as rum]))

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
      [:li "purge data"]]]]])