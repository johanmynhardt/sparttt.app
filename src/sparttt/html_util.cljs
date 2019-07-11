(ns sparttt.html-util)

(defn set-inner-html [id html]
  (->
    (.querySelector js/document id)
    (.-innerHTML)
    (set! html)))
