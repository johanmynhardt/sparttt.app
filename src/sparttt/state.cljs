(ns sparttt.state)

(defonce app-state
  (atom
    {:current-scene :home
     :scenes {:home nil}
     :toast
     {:text nil
      :visibility :hide
      :fn (fn [e]
            (swap!
             app-state assoc-in [:toast :visibility] :hidden))}}))
