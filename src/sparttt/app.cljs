(ns ^:figwheel-hooks sparttt.app
  (:require
   [goog.dom :as gdom]
   [rum.core :as rum]
   [sparttt.state :as state]
   [sparttt.ui :as ui]))

(println "This text is printed from src/sparttt/app.cljs. Go ahead and edit it and see reloading in action.")

(defn get-app-element []
  (gdom/getElement "app"))

(rum/defc sparttt-app < rum/reactive []
  (ui/draw-stage state/app-state))

(defn mount [el]
  (rum/mount (sparttt-app) el))

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))

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
