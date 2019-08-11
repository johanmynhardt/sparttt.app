(ns sparttt.scenes.timer
  (:require
    [cljs-time.coerce :as time.coerce]
    [cljs-time.core :as time]
    [cljs-time.format :as time.format]
    [rum.core :as rum]
    [sparttt.repository :as repository]
    [sparttt.ui-elements :as ui-e]))

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
        (for [{:keys [seq duration]} (reverse laps)]
          [:tr
           [:td [:b (str seq)]]
           [:td (when duration (time-formatted duration))]])]]]

     [:div.foot
      (ui-e/button (if int "Stop" "Start")
        {:icon (if int :stop :play)
         :on-click
         #(if int (stop-timer) (start-timer))})
      (ui-e/button "Lap"
        {:icon :plus
         :on-click
         #(let [now (time/now)
                lap-count (count laps)
                duration (race-duration genesis now)]
            (repository/save-lap
              {:seq lap-count
               :timestamp now
               :duration duration}))})]]))