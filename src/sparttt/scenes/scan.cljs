(ns sparttt.scenes.scan
  (:require
    [cljs-time.core :as time]
    [cljs-time.instant]
    [cljs.pprint]
    [Instascan]
    [Instascan.Camera]
    [Instascan.Scanner]
    [rum.core :as rum]
    [sparttt.repository :as repository]
    [sparttt.scenes.settings :as settings]
    [sparttt.browser-assist :as browser-assist]))

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

(def camera-atom (atom nil))
(defn with-camera [when-ready-fn]
  (cond
    (nil? @camera-atom)
    (->
      (Instascan.Camera/getCameras)
      (.then
        (fn [cms]
          (let [selected-cam @settings/selected-camera

                cam
                (or
                  (first (filter #(= selected-cam (.-id %)) cms))
                  (first cms))]
            (when cam
              (println "cam.id: " (.-id cam))
              (println "cam.name: " (.-name cam))
              (when-ready-fn (reset! camera-atom cam)))))))

    :else
    (when-ready-fn @camera-atom)))

(defn capture-qr [on-scan]
  (let [scanner (get-scanner)]
    (->
      scanner
      (.addListener "scan"
        (fn [content]
          (.stop scanner)
          (hide-video-modal)
          (repository/journal-append {:inst (time/now) :on-scan content})
          (on-scan content))))
    (with-camera
      (fn [camera]
        (-> scanner
          (.start camera))
        (show-video-modal)))))

(def athlete-regex #"^([\w\ \-'`]+){1}:(.+){1}$")
(defn capture-athlete []
  (capture-qr
    (fn [content]
      (let [[_ athlete ident] (re-matches athlete-regex content)]
        (cond
          (and athlete ident)
          (swap! athlete-details assoc
            :name athlete
            :id ident
            :tstamp (time/now))

          :else
          (do
            (browser-assist/speak "Sorry, I couldn't read a user from the input!")
            (js/alert (str "`" content "` did not match `athlete-regex`."))))))))

(def sequence-regex #"^(\d+)$")
(defn capture-sequence []
  (capture-qr
    (fn [content]
      (let [[_ sequence] (re-matches sequence-regex content)]
        (cond
          sequence
          (swap! athlete-sequence assoc
            :seq content
            :tstamp (time/now))

          :else
          (do
            (browser-assist/speak "Sorry, I couldn't read a sequence from the input!")
            (js/alert
              (str "`" content "` does not match `sequence-regex`."))))))))

(defn discard-details []
  (reset! athlete-details nil)
  (reset! athlete-sequence nil))

(defn persist-details []
  (let [value
        {:athlete @athlete-details
         :seq @athlete-sequence
         :tstamp (time/now)}]
    (repository/save-scan value)
    (discard-details)
    (reset! last-capture value)))

(def touch-icon-style
  {:style
   {:font-size 100
    :vertical-align "middle"}})

(rum/defc scene < rum/reactive []
  (reset! ascanner nil)
  (let [athlete (rum/react athlete-details)
        sequence (rum/react athlete-sequence)
        last-athlete (rum/react last-capture)]

    [:div

     [:div.video-modal {:hidden true}
      [:video#preview]
      [:button
       {:on-click
        #(let [scanner (get-scanner)]
           (println "Stopping scanner " scanner)
           (.stop scanner)
           (hide-video-modal))}
       [:li.fas.fa-stop] " stop scan"]]

     (when-not athlete
       [:div.card.with-gradient {:on-click capture-athlete}
        [:div [:li.fas.fa-address-card touch-icon-style]
         [:li.no-list "Capture Athlete"]]])

     (when (and athlete (not sequence))
       [:div.card.with-gradient {:on-click capture-sequence}
        [:div [:li.fas.fa-hashtag touch-icon-style]
         [:li.no-list "Capture Sequence"]]])

     (when (and athlete sequence)
       [:div.card
        [:div
         [:div [:b "Name:"]]
         [:div [:p (:name athlete)]]
         #_[:br]
         [:div [:b "Sequence:"] " " (:seq sequence)]]
        [:hr]
        [:button {:on-click persist-details}
         [:li.fas.fa-save] " persist"] " "
        [:button {:on-click discard-details}
         [:li.fas.fa-trash] " " "discard"]])

     (when last-athlete
       [:div.card
        [:div.title [:li.fas.fa-check] " " "Last Stored"]
        [:p (with-out-str (cljs.pprint/pprint last-athlete))]])]))