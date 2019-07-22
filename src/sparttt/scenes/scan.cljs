(ns sparttt.scenes.scan
  (:require
    [cljs-time.coerce :as time.coerce]
    [cljs-time.core :as time]
    [cljs.pprint]
    [Instascan]
    [rum.core :as rum]
    [sparttt.repository :as repository]))

(def athlete-details (atom nil))
(def athlete-sequence (atom nil))
(def last-capture (atom nil))

(def ascanner (atom nil))
(add-watch ascanner :scanner
  (fn [k r o n]
    (println "scanner changed: " o " -> " n)
    (when (and (not= o n) (nil? n))
      (.stop o))))

(defn get-scanner []
  (let [preview (.querySelector js/document "#preview")]
    (when (or
            (nil? (deref ascanner))
            ;(empty? (.-innerHTML preview))
            )
      (let [_ (println "initiate scanner from preview")
            ;_ (println "prInstascan: " (js/Instascan.Scanner. nil))
            scnr (new js/Instascan.Scanner (clj->js {:video preview}))
            ]
        (reset! ascanner scnr))))
  (deref ascanner))

(defn capture-qr [on-scan]
  (let [preview (.querySelector js/document "#preview")
        scanner (get-scanner)]
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
      (.getCameras js/Instascan.Camera)
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
  (reset! ascanner nil)
  (let [det (rum/react athlete-details)
        seq (rum/react athlete-sequence)
        ]
    [:div
     [:button {:on-click #(let [scanner (get-scanner)]
                            (println "Stopping scanner " scanner)
                            (.stop scanner))} "stop scan"]
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