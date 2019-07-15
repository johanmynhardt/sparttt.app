(ns sparttt.scenes.scan
  (:require
    [cljs-time.coerce :as time.coerce]
    [cljs-time.core :as time]
    [cljs.pprint]
    [instascan :as Instascan]
    [rum.core :as rum]
    [sparttt.repository :as repository]))

(def athlete-details (atom nil))
(def athlete-sequence (atom nil))
(def last-capture (atom nil))

(defn capture-qr [on-scan]
  (let [preview (.querySelector js/document "#preview")
        scanner (Instascan/Scanner. (clj->js {:video preview}))]
    (->
      scanner
      (.addListener "scan"
        (fn [content]
          (.stop scanner)
          (->
            preview
            (.setAttribute "hidden" true))
          (println "got content: " content)
          (on-scan content))))

    (->
      preview
      (.removeAttribute "hidden"))

    (->
      (.getCameras Instascan/Camera)
      (.then
        (fn [cms]
          (let [cam (first cms)]
            (when cam
              (-> scanner
                (.start cam)))))))))

(defn capture-athlete []
  ;; TODO: use instascan to capture and verify athlete details.
  (capture-qr
    (fn [content]
      (let [[_ nm id] (re-matches #"(.*):(.*)" content)]
        ;; TODO: get correct regex for extracting groups
        (swap! athlete-details assoc
          :name nm
          :id id
          :tstamp (time.coerce/to-string (time/now)))))))

(defn capture-sequence []
  ;; TODO: use instascan to capture and verify sequence
  (capture-qr
    (fn [content]
      (swap! athlete-sequence assoc
        :seq content
        :tstamp (time.coerce/to-string (time/now))))))

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
     [:video#preview {:hidden true}]
     [:div.card
      [:div.title [:li.fas.fa-address-card] " " "Capture Athlete"]
      [:div.content
       (when-not det
         [:i "[ not captured ]"])
       (when det
         (str det))]
      [:div.actions
       [:button {:on-click capture-athlete} "Capture Athlete" (when det " again")]]]

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