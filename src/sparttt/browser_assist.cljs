(ns sparttt.browser-assist
  (:require [clojure.string :as str]))

(defn speak
  "Use the browser's assistive technology to read out text."
  [content & more]
  (when content
    (-> js/window.speechSynthesis
      (.speak
        (new js/SpeechSynthesisUtterance
          (if-not more
            content
            (str/join " "
              (cons content (map str more)))))))))