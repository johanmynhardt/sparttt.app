(ns ^:figwheel-hooks sparttt.app
  (:require
   [goog.dom :as gdom]
   [rum.core :as rum]
   [sparttt.stage-conductor :as stage-conductor]
   [sparttt.scenes.scan]
   [sparttt.scenes.home]
   [sparttt.scenes.settings]
   [sparttt.scenes.visitors]
   [sparttt.scenes.consolidate]
   [sparttt.scenes.timer :as timer]
   [sparttt.stage]
   [sparttt.repository :as repository]
   [sparttt.ui-elements :as ui]))

(println "This text is printed from src/sparttt/app.cljs. Go ahead and edit it and see reloading in action.")

(def stages
  [#'sparttt.scenes.home/scene-data
   #'sparttt.scenes.scan/scene-data
   #'sparttt.scenes.settings/scene-data
   #'sparttt.scenes.timer/scene-data
   #'sparttt.scenes.consolidate/scene-data
   #'sparttt.scenes.visitors/scene-data])

(defn get-app-element []
  (gdom/getElement "app"))

(rum/defc sparttt-app < rum/reactive []
  (stage-conductor/draw-stage stages))

(defn mount [el]
  (rum/mount (sparttt-app) el))

(defn mount-app-element []
  (let [el (get-app-element)]
    (when el
      (mount el))))

;; conditionally start your application based on the presence of an "app" element
;; this is particularly helpful for testing this ns without launching the app
(mount-app-element)

;; specify reload hook with ^;after-load metadata
(defn ^:after-load on-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  (swap! sparttt.state/app-state update-in [:__figwheel_counter] inc)
  (mount-app-element))

;; needs to run on first load to hydrate repo from localStorage
(repository/restore-from-local-storage)
(when (= :running (:timer-state @repository/repo))
  (timer/start-timer)
  (reset! sparttt.stage/stage-cursor :timer))

(when (not (:ping @sparttt.state/app-state))
  (swap! 
   sparttt.state/app-state assoc :ping
   (js/setInterval
    (fn []
      (when (aget js/localStorage "upversion")
        (ui/show-toast
         "Update available. Press OK to reload."
         {:dismiss-fn
          (fn [e]
            (.removeItem js/localStorage "upversion")
            (.reload js/window.location true))})))
    5000)))
