(ns sparttt.scenes.visitors
  (:require
   [clojure.string :as str]
   [rum.core :as rum]
   [sparttt.browser-assist :as browser-assist]
   [sparttt.ui-elements :as ui-e]
   [sparttt.repository :as repository]
   [sparttt.stage :as stage]))

(def visitors-cursor
  (rum/cursor repository/repo :visitors))

(def empty-user
  {:first-name ""
   :last-name ""
   :ident ""})

(def new-user (atom empty-user))

(defn normalize-ident [ident]
  (let [[_ c d] (re-matches #"(?i)([a-zA-Z]*)(\d*)+?" ident)
        C (cond
            (empty? c)
            "V"
            :else (str/upper-case c))
        
        n (js/parseInt d)
        D
        (cond
          (< n 10)
          (str "00" n)

          (< n 100)
          (str "0" n)

          :else
          (str n))]
    (str C D)))

(rum/defc scene < rum/reactive []
  (let [nuser (rum/react new-user)
        ident (rum/cursor-in new-user [:ident])
        first-name (rum/cursor-in new-user [:first-name])
        last-name (rum/cursor-in new-user [:last-name])
        
        got-all-details?
        (every?
         not-empty
         [(:ident nuser)
          (:first-name nuser)
          (:last-name nuser)])]
    [:div
     
     [:div.card
      [:div.title [:li.fas.fa-handshake] " " "Add Visitor"]
      [:div.content
       [:p "Fill out the visitor id, first name and last name and press \"Add\""]
       
       [:div.actions
        (ui-e/input ident "Visitor ID" {:id "inputVid" :placeholder "V001"})
        (ui-e/input first-name "First Name" {:placeholder "John"})
        (ui-e/input last-name "Last Name" {:placeholder "Doe"})]
       
       [:div.actions
        (ui-e/button
         "Add"
         {:class [(when got-all-details? :primary)]
          :on-click
          #(let [corrected-ident (normalize-ident @ident)
                 existing-visitor-with-id
                 (first (filter (comp (partial = corrected-ident) :ident) @visitors-cursor))]
             (cond
               (and got-all-details? (not existing-visitor-with-id))
               (do
                 (repository/save-visitor (assoc nuser :ident corrected-ident))
                 (reset! new-user empty-user)
                 (->
                  js/document
                  (.querySelector "#inputVid")
                  (.focus)))
               
               (not got-all-details?)
               (js/alert "Not all the fields are completed.")

               existing-visitor-with-id
               (js/alert (str "A Visitor with this id already exists: " existing-visitor-with-id))

               :else
               (js/alert "This is an unexpected condition. Please report a bug!")))})]]]

     [:div
      [:h3 "Visitors"]
      
      [:table
       [:thead
        [:tr [:th "VID"] [:th "Name"] [:th "Surname"]]]
       [:tbody
        (->>
         (rum/react visitors-cursor)
         reverse
         (map
          (fn [{:keys [ident first-name last-name]}]
            [:tr [:td ident] [:td first-name] [:td last-name]])))]]]]))

(stage/register-scene
 (stage/configure-scene
  :visitors
  {:layout
   {:header {:title "Visitors"}
    :graphics {:icon :handshake}}}
  
  {:scene #'scene}))
