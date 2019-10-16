(ns sparttt.scenes.visitors
  (:require
   [rum.core :as rum]
   [sparttt.ui-elements :as ui-e]
   [sparttt.repository :as repository]))

(def visitors-cursor
  (rum/cursor repository/repo :visitors))

(def empty-user
  {:first-name ""
   :last-name ""
   :ident ""})

(def new-user (atom empty-user))

(rum/defc scene < rum/reactive []
  (let [nuser (rum/react new-user)
        ident (rum/cursor-in new-user [:ident])
        first-name (rum/cursor-in new-user [:first-name])
        last-name (rum/cursor-in new-user [:last-name])]
    [:div
     
     [:div.card
      [:div.title [:li.fas.fa-handshake] " " "Add Visitor"]
      [:div.content
       [:p "Fill out the visitor id, first name and last name and press \"Add\""]
       [:div.field
        [:label "Visitor ID"]
        [:input 
         {:type "text"
          :value @ident
          :auto-focus false
          :on-change (fn [e] (reset! ident (-> e (.-target) (.-value))))
          :placeholder "V001"}]]
       [:div.field
        [:label "First Name"]
        [:input
         {:type "text"
          :value @first-name
          :auto-focus false
          :on-change (fn [e] (reset! first-name (-> e (.-target) (.-value))))
          :placeholder "John"}]]
       [:div.field
        [:label "Last Name"]
        [:input
         {:type "text"
          :value @last-name
          :auto-focus false
          :on-change (fn [e] (reset! last-name (-> e (.-target) (.-value))))
          :placeholder "Doe"}]]

       
       [:div.actions
        (ui-e/button
         "Add"
         {:class [:primary]
          :on-click
          #(do
             (repository/save-visitor nuser)
             (reset! new-user empty-user))})]]]

     [:div
      [:h3 "Visitors"]
      
      [:table
       [:tr [:th "VID"] [:th "Name"] [:th "Surname"]]
       
       (->>
        (rum/react visitors-cursor)
        (map
         (fn [{:keys [ident first-name last-name]}]
           [:tr [:td ident] [:td first-name] [:td last-name]])))]]]))
