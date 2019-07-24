(ns sparttt.scenes.scan
  (:require
    [cljs-time.coerce :as time.coerce]
    [cljs-time.core :as time]
    [cljs.pprint]
    [Instascan]
    [Instascan.Camera]
    [Instascan.Scanner]
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

(defn get-video-preview []
  (.querySelector js/document "#preview"))

(defn get-video-modal []
  (.querySelector js/document "div.video-modal"))

(defn show-video-modal []
  (->
    (get-video-modal)
    (.removeAttribute "hidden")))

(defn hide-video-modal []
  (->
    (get-video-modal)
    (.setAttribute  "hidden" true)))




(defn get-scanner []
  (let [preview (get-video-preview)]
    (when (nil? (deref ascanner))
      (let [scnr
            (new js/Instascan.Scanner.
              (clj->js
                {:video preview
                 :mirror false}))]
        (reset! ascanner scnr))))
  (deref ascanner))

(defn capture-qr [on-scan]
  (let [scanner (get-scanner)]
    (->
      scanner
      (.addListener "scan"
        (fn [content]
          (.stop scanner)
          (hide-video-modal)
          (println "got content: " content)
          (on-scan content))))

    (->
      (.getCameras js/Instascan.Camera)
      (.then
        (fn [cms]
          (let [selected-cam @sparttt.scenes.settings/selected-camera

                cam
                (or
                  (first (filter #(= selected-cam (.-id %)) cms))
                  (first cms))]
            (when cam
              (println "cam.id: " (.-id cam))
              (println "cam.name: " (.-name cam))

              (-> scanner
                (.start cam)))))))

    (show-video-modal)))

(defn capture-athlete []
  ;; TODO: use regex from V1 to verify athlete name/id.
  (capture-qr
    (fn [content]
      (let [[_ nm id] (re-matches #"(.*):(.*)" content)]
        ;; TODO: get correct regex for extracting groups
        (swap! athlete-details assoc
          :name nm
          :id id
          :tstamp (time.coerce/to-string (time/now)))))))

(defn capture-sequence []
  ;; TODO: use regex from V1 to verify sequence
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
        last (rum/react last-capture)
        ]
    [:div

     [:div.video-modal {:hidden true}
      [:video#preview]
      [:button {:on-click
                #(let [scanner (get-scanner)]
                   (println "Stopping scanner " scanner)
                   (.stop scanner)
                   (hide-video-modal))} "stop scan"]]

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

     (when last
       [:div.card
        [:div.title [:li.fas.fa-check] " " "Last Stored"]
        [:p (with-out-str (cljs.pprint/pprint last))]])]))