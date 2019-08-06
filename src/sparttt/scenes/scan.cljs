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
    [sparttt.browser-assist :as browser-assist]
    [sparttt.ui-elements :as ui-e]
    [clojure.string :as str]))

(def athlete-details (atom nil))
(def athlete-sequence (atom nil))
(def last-capture (atom nil))

(defn with-camera [when-ready-fn]
  (let [sci @settings/selected-camera-inst]
    (cond
      (not (nil? sci))
      (when-ready-fn sci)

      :else
      (js/alert "No camera selected!"))))

(defn capture-qr [on-scan]
  (let [scanner (ui-e/get-scanner)]
    (with-camera
      (fn [camera]
        (-> scanner
          (.start camera))
        (ui-e/show-video-modal)))
    (->
      scanner
      (.addListener "scan"
        (fn [content]
          (.stop scanner)
          (.removeAllListeners scanner)
          (ui-e/hide-video-modal)
          (repository/journal-append {:inst (time/now) :on-scan content})
          (on-scan content))))))

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
            (js/setTimeout
              #(js/alert
                 (str "`" content "` did not match `athlete-regex`.")))))))))

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
            (js/setTimeout
              #(js/alert
                 (str "`" content "` does not match `sequence-regex`.")))))))))

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
    (reset! last-capture value)
    (browser-assist/speak "Just saved" (-> value :seq :seq) "for" (-> value :athlete :name))))

(def touch-icon-style
  {:style
   {:font-size 100
    :vertical-align "middle"}})

(rum/defc scene < rum/reactive []
  (let [athlete (rum/react athlete-details)
        sequence (rum/react athlete-sequence)
        last-athlete (rum/react last-capture)]

    [:div
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
        (ui-e/button "Save"
          {:icon :save
           :class [:primary]
           :on-click persist-details})
        " "
        (ui-e/button "Discard"
          {:icon :trash
           :class [:warn]
           :on-click discard-details})])

     (when last-athlete
       [:div.card.success
        [:div.title [:li.fas.fa-check] " " "Last Stored"]
        [:p [:b (-> last-athlete :seq :seq) ": "] (-> last-athlete :athlete :name)]])]))