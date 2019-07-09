(ns sparttt.scenes.scan
  (:require
    [cljs-time.coerce :as time.coerce]
    [cljs-time.core :as time]
    [rum.core :as rum]
    [sparttt.repository :as repository]))

(def athlete-details (atom nil))
(def athlete-sequence (atom nil))
(def last-capture (atom nil))

(defn capture-athlete []
  ;; TODO: use instascan to capture and verify athlete details.
  (swap! athlete-details assoc
    :name "Jon Doe"
    :id "aeu3"
    :tstamp (time.coerce/to-string (time/now))))

(defn capture-sequence []
  ;; TODO: use instascan to capture and verify sequence
  (swap! athlete-sequence assoc
    :seq (time.coerce/to-epoch (time/now))
    :tstamp (time.coerce/to-string (time/now))))

(defn discard-details []
  (reset! athlete-details nil)
  (reset! athlete-sequence nil))

(defn persist-details []
  (let [value
        {:athlete @athlete-details
         :seq @athlete-sequence
         :tstamp (time.coerce/to-string (time/now))}]
    (repository/save-scan value)
    (discard-details)
    (reset! last-capture value)))

(rum/defc scene < rum/reactive []
  (let [det (rum/react athlete-details)
        seq (rum/react athlete-sequence)]
    [:div
     [:div.card
      [:div.title [:li.fas.fa-address-card] " " "Capture Athlete"]
      [:div.content
       (when-not det
         [:i "[ not captured ]"])
       (when det
         (str det))]
      [:div.actions
       [:button {:on-click capture-athlete} "Capture Athlete" (when det "again")]]]

     [:div.card
      [:div.title [:li.fas.fa-hashtag] " " "Capture Sequence"]
      [:div.content
       (when-not seq
         [:i "[ not captured ]"])
       (when seq
         (str seq))]
      [:div.actions
       [:button {:on-click capture-sequence} "Capture Sequence"]]]



     (when (and det seq)
       [[:button {:on-click persist-details} "persist"] "|" [:button {:on-click discard-details} "discard"]])

     (when @last-capture
       [:div.card
        [:div.title [:li.fas.fa-check] " " "Last Stored"]
        [:p (with-out-str (cljs.pprint/pprint @last-capture))]])]))