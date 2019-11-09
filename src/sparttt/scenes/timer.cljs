(ns sparttt.scenes.timer
  (:require
    [cljs-time.coerce :as time.coerce]
    [cljs-time.core :as time]
    [cljs-time.format :as time.format]
    [rum.core :as rum]
    [sparttt.stage :as stage]
    [sparttt.repository :as repository]
    [sparttt.ui-elements :as ui-e]
    [sparttt.browser-assist :as browser-assist]))

(defn race-duration [genesis finish-inst]
  (time.coerce/from-long
    (time/in-millis
      (time/interval
        genesis finish-inst))))

(def ticker
  (atom
    {:instant nil
     :interval nil}))

(def interval (rum/cursor ticker :interval))
(def instant (rum/cursor ticker :instant))
(def laps (rum/cursor repository/repo :laps))

(defn start-timer []
  (when-not (repository/genesis)
    (repository/save-genesis (time/now)))
  (swap! ticker assoc
    :interval
    (js/setInterval
      #(swap! ticker assoc :instant (time/now))
      110))
  (repository/save-timer-state :running))

(defn stop-timer []
  (when @interval
    (js/clearInterval @interval)
    (swap! ticker dissoc :interval))
  (repository/save-timer-state nil))

(defn time-formatted [dt]
  (when dt
    (->
      (time.format/formatter "HH:mm:ss.SSS")
      (time.format/unparse (time.coerce/to-date-time dt)))))

(rum/defc scene < rum/reactive []
  (let [t (rum/react instant)
        int (rum/react interval)
        genesis (repository/genesis)
        laps (rum/react laps)]
    [:div.grid-container
     [:div.card.with-gradient
      [:code {:style {:font-size "33pt"}}
       (str
         (when (and t genesis)
           (time-formatted
             (race-duration genesis (time/now)))))]]

     [:div.middle.scroll.flex
      [:table
       [:thead
        [:tr
         [:th "Lap"] [:th "Time"]]]
       [:tbody
        (for [{:keys [seq duration]} (take 15 (rseq laps))]
          [:tr
           [:td [:b (str seq)]]
           [:td (when duration (time-formatted duration))]])]]]

     [:div.foot
      [:p]
      [:div.row
       (ui-e/button (if int "Stop" "Start")
         {:icon (if int :stop :play)
          :on-click
          #(cond
             int
             (do
               (stop-timer)
               (browser-assist/vibrate 300 50 300 50 300))

             :else
             (do
               (start-timer)
               (browser-assist/vibrate 500 50 100)))})
       [:div.flex]
       (ui-e/button "Lap"
         {:icon :plus
          :on-click
          #(let [now (time/now)
                 duration (race-duration genesis now)]
             (browser-assist/vibrate 50)
             (repository/save-lap
               {:timestamp now
                :duration duration}))})]]]))

(stage/register-scene
 (stage/configure-scene
  :timer
  {:layout
   {:header {:title "Timer"}
    :content {:class [:grid-container]}
    :footer {:visibility :hide}
    :graphics {:icon :stopwatch}}
    
   :scene #'scene}))
