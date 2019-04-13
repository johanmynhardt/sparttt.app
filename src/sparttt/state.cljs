(ns sparttt.state)

(defonce app-state
  (atom
    {:text "Hello world!"

     :menu
     {:expanded false}

     :stage
     {:current :home}}))