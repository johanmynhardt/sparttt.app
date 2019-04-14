(ns sparttt.stage
  (:require
    [rum.core :as rum]
    [sparttt.state :as state]))

(def stage-config
  {:home
   {:header
    {:visible true
     :title "Home"}
    :content {:visible true}
    :footer {:visible true}}

   :scan
   {:header
    {:visible true
     :title "Capture QR"}
    :content {:visible true}
    :footer {:visible false :rows 2}}

   :timer
   {:header
    {:visible true
     :title "Timer"}
    :content {:visible true}
    :footer {:visible false}}

   :settings
   {:header
    {:visible true
     :title "Settings"}
    :content {:visible true}
    :footer {:visible true}}})

(def stage-cursor
  (rum/cursor-in state/app-state [:stage]))

(def scene-cursor
  (rum/cursor-in state/app-state [:scenes]))

(defn active-stage-key
  []
  (:current (rum/react stage-cursor)))

(defn active-stage
  []
  (get stage-config (active-stage-key)))

(defn activate-stage
  [stage-key]

  (let [result
        {:stage-config
         {:keys (keys stage-config)
          :current (:current (deref stage-cursor))}}]

    (cond
      (get stage-config stage-key)
      (do
        ;(println "activate-stage request applied: " (assoc result :new-key stage-key))
        (swap! stage-cursor assoc :current stage-key))

      :else
      (let [message (str "activate-stage request failed. (no key for " stage-key ")")]
        (println message)
        (js/alert message)))))

(defn register-scene
  [key scene-data]
  (swap! scene-cursor assoc
    key scene-data))

(defn scene-for
  [active-key]

  (let [scene (active-key (deref scene-cursor))]
    [:div.scene
     (cond
       (some? scene)
       scene

       :else
       [:b.warn.shadow
        (str "No scene for " active-key)])]))