(ns sparttt.state)

(defonce app-state
  (atom
    {:current-scene :home

     :scenes
     {:home nil}}))