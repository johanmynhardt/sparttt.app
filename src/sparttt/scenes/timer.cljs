(ns sparttt.scenes.timer
  (:require
    [cljs-time.core :as time]
    [cljs-time.coerce :as time.coerce]))

;; todo: use when lap timing functionality becomes available.
(defn race-duration [genesis finish-inst]
  (time.coerce/from-long
    (time/in-millis
      (time/interval
        genesis finish-inst))))