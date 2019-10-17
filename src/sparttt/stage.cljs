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
    :footer {:visible true}
    :ui {:icon :i.fas.fa-home}}

   :scan
   {:header
    {:visible true
     :title "Capture QR"}
    :content {:visible true}
    :footer {:visible false :rows 2}
    :ui {:icon :i.fas.fa-address-card}}

   :timer
   {:header
    {:visible true
     :title "Timer"}
    :content {:visible true :class [:grid-container]}
    :footer {:visible false}
    :ui {:icon :i.fas.fa-stopwatch}}

   :settings
   {:header
    {:visible true
     :title "Settings"}
    :content {:visible true}
    :footer {:visible true}
    :ui {:icon :i.fas.fa-cog}}
   
   :visitors
   {:header
    {:visible true
     :title "Visitors"}
    :content {:visible true}
    :footer {:visible true}
    :ui {:icon :i.fas.fa-handshake}}
   
   :consolidate
   {:header 
    {:visible true 
     :title "Consolidate Data"}
    :content {:visible true}
    :footer {:visible false}
    :ui {:icon :i.fas.fa-sort-amount-down}}})

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

  (let [scene (active-key (deref scene-cursor))
        scene-classes (get-in stage-config [active-key :content :class])]
    [:div.scene {:class scene-classes}
     (cond
       (some? scene)
       (cond
         (fn? scene) (scene)

         :else scene)

       :else
       [:div.card.warn
        (str "No scene for " active-key " yet.")])]))
