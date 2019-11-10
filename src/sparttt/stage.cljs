(ns sparttt.stage
  (:require
    [rum.core :as rum]
    [clojure.string :as str]
    [sparttt.state :as state]))

(defn deep-merge [a & maps]
  (if (map? a)
    (apply merge-with deep-merge a maps)
    (apply merge-with deep-merge maps)))

(defn configure-scene [scene-key & scene-component-maps]
  {scene-key
   (reduce
    deep-merge
    (cons
     {:layout
      {:header {:visibility :show}
       :content {:visibility :show :class []}
       :footer {:visibility :show}
       :graphics {:icon :fa-question}}
      
      :scene :undefined}
     scene-component-maps))})

(defn icon [fa-id]
  {:icon
   (keyword
    (str "i.fas.fa-" (name (or fa-id :exclamation))))})

(def stage-cursor
  (rum/cursor-in state/app-state [:current-scene]))

(def scene-cursor
  (rum/cursor-in state/app-state [:scenes]))

(defn active-stage-key
  []
  (rum/react stage-cursor))

(defn active-stage
  []
  (get @scene-cursor (active-stage-key)))

(defn activate-stage
  [stage-key]
  ;(println "request to activate stage: " stage-key)
  (cond
    (get @scene-cursor stage-key)
    (reset! stage-cursor stage-key)
    
    :else
    (let [message (str "activate-stage request failed. (no key for " stage-key ")")]
      (println message)
      (js/alert message))))

(defn register-scene
  "Registers scene to stage/scene-cursor.

  Until figured out, it's compulsory to require the
  namespace in sparttt.app so the scenes' registrations
  get triggered."

  [scene-config]
  (println "Registering scene: " (keys scene-config))
  (swap! scene-cursor merge scene-config))

(defn scene-for
  [active-key]
  (println "handling scene for active-key " active-key)
  (let [{:keys [layout scene]}
        (active-key (deref scene-cursor))]

    [:div.scene
     {:class (get-in layout [:content :class])}
     
     (cond
       (some? scene)
       (cond
         (fn? scene) (scene)

         :else scene)

       :else
       [:div.card.warn
        (str "No scene for " active-key " yet.")])]))
