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

(def mime-types
  {:edn {:mime "application/edn" :ext "edn"}
   :csv {:mime "text/csv" :ext "csv"}
   :json {:mime "application/json" :ext "json"}})

(defn initiate-download [type data filename]
  (let [type-info (get mime-types type)
        mime (:mime type-info)
        blob (new js/Blob [data] (clj->js {:type mime}))
        link (.createElement js/document "a")
        _ (set! (.-href link) (.createObjectURL js/window.URL blob))
        _ (set! (.-download link) (str filename "." (:ext type-info)))]
    (.click link)))

;(initiate-download "text/plain" "hello world!" "foobar.txt")