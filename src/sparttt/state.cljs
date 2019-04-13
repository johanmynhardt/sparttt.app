(ns sparttt.state)

(defonce app-state
  (atom
    {:text "Hello world!"

     :stage
     {:current :home}

     :scenes
     {:home nil}}))