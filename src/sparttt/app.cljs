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
    [sparttt.repository :as repository]))

(println "This text is printed from src/sparttt/app.cljs. Go ahead and edit it and see reloading in action.")

(defn get-app-element []
  (gdom/getElement "app"))

(rum/defc sparttt-app < rum/reactive []
  (stage-conductor/draw-stage))

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
  (mount-app-element)
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

;; needs to run on first load to hydrate repo from localStorage
(repository/restore-from-local-storage)
(when (= :running (:timer-state @repository/repo))
  (timer/start-timer))
