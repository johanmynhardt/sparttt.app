(ns sparttt.repository)

(defonce repo (atom {:scans []}))

(defn save-scan [val]
  (swap! repo update :scans (fn [scans] (conj scans val))))

(defn list-scans []
  (let [scans
        (get @repo :scans)]
    (js/console.info "returning scans: " (count scans))
    scans))