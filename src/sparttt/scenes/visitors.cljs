(ns sparttt.scenes.visitors
  (:require
   [rum.core :as rum]
   [sparttt.ui-elements :as ui-e]))

(defonce visitors
  (atom [{:ident "V001" 
          :first-name "Pietie"
          :last-name "vd Walt"}]))

(def new-user
  (atom 
   {:first-name ""
    :last-name ""
    :ident ""}))

(rum/defc scene < rum/reactive []
  (let [nuser (rum/react new-user)
        ident (rum/cursor-in new-user [:ident])
        first-name (rum/cursor-in new-user [:first-name])
        last-name (rum/cursor-in new-user [:last-name])]
    [:div
     
     [:div.card
      [:div.title [:li.fas.fa-handshake] " " "Add Visitor"]
      [:div.content
       [:div.field
        [:div "Visitor ID"]
        [:input 
         {:type "text"
          :value @ident
          :auto-focus false
          :on-change (fn [e] (reset! ident (-> e (.-target) (.-value))))}]]
       [:div.field
        [:div "First Name"]
        [:input
         {:type "text"
          :value @first-name
          :auto-focus false
          :on-change (fn [e] (reset! first-name (-> e (.-target) (.-value))))}]]
       [:div.field
        [:div "Last Name"]
        [:input
         {:type "text"
          :value @last-name
          :auto-focus false
          :on-change (fn [e] (reset! last-name (-> e (.-target) (.-value))))}]]

       (ui-e/button
        "Add"
        {:on-click
         #(do
            (swap! visitors conj nuser)
            (reset!
             new-user
             {:ident ""
              :first-name ""
              :last-name ""}))})]]

     [:div
      [:h3 "Visitors"]
      
      [:table
       [:tr
        [:th "VID"]
        [:th "Name"]
        [:th "Surname"]]
       (->>
        (rum/react visitors)
        (map
         (fn [{:keys [ident first-name last-name]}]
           [:tr
            [:td ident]
            [:td first-name]
            [:td last-name]])))]
       ]]))
