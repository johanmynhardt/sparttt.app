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
(def sequence-override (atom nil))
(def show-sequence-override (atom false))
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
            (browser-assist/vibrate 500)
            (browser-assist/speak "Sorry, I couldn't read an athlete from the input!")
            (js/setTimeout
              #(js/alert
                 (str "`" content "` did not match `athlete-regex`.")))))))))

(defn capture-unknown-athlete []
  (swap! athlete-details assoc
    :name "Unknown"
    :id "N/A"
    :tstamp (time/now)))

(def sequence-regex #"^(\d+)$")
(defn process-sequence [content]
  (let [[_ sequence] (re-matches sequence-regex content)]
    (cond
      sequence
      (swap! athlete-sequence assoc
        :seq content
        :tstamp (time/now))

      :else
      (do
        (browser-assist/vibrate 500)
        (browser-assist/speak "Sorry, I couldn't read a sequence from the input!")
        (js/setTimeout
          #(js/alert
             (str "`" content "` does not match `sequence-regex`.")))))))
(defn capture-sequence []
  (capture-qr process-sequence))

(defn discard-details []
  (reset! athlete-details nil)
  (reset! athlete-sequence nil)
  (reset! sequence-override nil))

(defn persist-details []
  (let [value
        {:athlete @athlete-details
         :seq @athlete-sequence
         :tstamp (time/now)}]
    (repository/save-scan value)
    (discard-details)
    (reset! last-capture value)
    (browser-assist/vibrate 100 50 100)
    (browser-assist/speak "Just saved" (-> value :seq :seq) "for" (-> value :athlete :name))))

(def touch-icon-style
  {:style
   {:font-size 100
    :vertical-align "middle"}})

(rum/defc scene < rum/reactive []
  (let [athlete (rum/react athlete-details)
        sequence (rum/react athlete-sequence)
        last-athlete (rum/react last-capture)
        sequence-o (rum/react sequence-override)
        show-sequence-o (rum/react show-sequence-override)]

    [:div
     (when (and (not athlete) (not last-athlete))
       [:div
        [:div.card.with-gradient {:on-click capture-athlete}
         [:div [:li.fas.fa-address-card touch-icon-style]
          [:li.no-list "Capture Athlete"]]]

        (ui-e/button "Unknown"
          {:icon :question-circle
           :on-click capture-unknown-athlete})])

     (when (and athlete (not sequence))
       [[:div.card.with-gradient
         [:div.title [:li.fas.fa-info] " " "Capturing"]
         [:p [:b "Name:"] " " (:name athlete)]]
        (when-not show-sequence-o
          [:div.card.with-gradient {:on-click capture-sequence}
           [:div [:li.fas.fa-hashtag touch-icon-style]
            [:li.no-list "Capture Sequence"]]])

        (when show-sequence-o
          [:div.card
           [:div.title "Override Sequence"]
           [:p [:b "Sequence:"] " "
            [:input
             {:type :number :min 0 :step 1
              :value sequence-o
              :on-change
              (fn [e]
                (let [v (-> e (.-target) (.-value))]
                  (reset! sequence-override v)))}]]
           (ui-e/button "Next"
             {:icon :arrow-circle-right
              :class [(when (pos? (js/parseInt sequence-o)) :primary)]
              :on-click #(process-sequence (or sequence-o ""))})
           (ui-e/button "Cancel"
             {:icon :trash
              :class [:warn]
              :on-click
              #(do
                 (reset! sequence-override nil)
                 (reset! show-sequence-override false))})])

        (ui-e/button "Undo"
          {:icon :undo
           :on-click
           #(do
              (reset! athlete-details nil)
              (reset! show-sequence-override false))})

        (when-not show-sequence-o
          (ui-e/button "Override"
            {:icon :keyboard
             :on-click #(reset! show-sequence-override true)}))])

     (when (and athlete sequence)
       [[:div.card.with-gradient
         [:div.title [:li.fas.fa-info] " " "Capturing"]
         [:p [:b "Name:"] " " (:name athlete)]
         [:div [:b "Sequence:"] " " (:seq sequence)]]

        [:div.card
         [:div.title [:li.fas.fa-database] " " "Data"]
         [:p "Save or discard this record?"]
         (ui-e/button "Save"
           {:icon :save
            :class [:primary]
            :on-click persist-details})
         " "
         (ui-e/button "Discard"
           {:icon :trash
            :class [:warn]
            :on-click discard-details})]])

     (when last-athlete
       [[:div.card.success
         [:div.title [:li.fas.fa-check] " " "Just Captured"]
         [:p [:b "Name: "] " " (-> last-athlete :athlete :name)]
         [:div [:b "Sequence:"] " " (-> last-athlete :seq :seq)]]
        (ui-e/button "Next"
          {:icon :arrow-circle-right
           :style {:color :black}
           :on-click #(reset! last-capture nil)})])]))