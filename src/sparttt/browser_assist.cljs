(ns sparttt.browser-assist
  (:require
    [clojure.string :as str]
    [clojure.browser.event :as browser.event]))

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
   :json {:mime "application/json" :ext "json"}
   :txt {:mime "text/plain" :ext "txt"}})

(defn initiate-download [type data filename]
  (let [type-info (get mime-types type)
        mime (:mime type-info)
        blob (new js/Blob [data] (clj->js {:type mime}))
        link (.createElement js/document "a")
        reader (js/FileReader.)]

    (set!
      (.-onload reader)
      (fn [_]
        (set! (.-href link) (.-result reader))
        (set! (.-download link) (str filename "." (:text type-info)))
        (.click link)))
    (-> reader (.readAsDataURL blob))))

(defn initiate-form-post-download [type data filename]
  (let [type-info (get mime-types type)
        mime (:mime type-info)
        form (-> js/document (.createElement "form"))
        el-filename (-> js/document (.createElement "input"))
        filebody (-> js/document (.createElement "textarea"))
        submit (-> js/document (.createElement "input"))]

        
    (set! (.-action form) "init-download")
    (set! (.-method form) "post")
    (set! (.-hidden form) "hidden")
    
    (set! (.-id el-filename) "filename")
    (set! (.-name el-filename) "filename")
    (set! (.-type el-filename) "text")
    (set! (.-value el-filename) (str filename "." (:text type-info)))

    (set! (.-id filebody) "filebody")
    (set! (.-name filebody) "filebody")
    (set! (.-innerHTML filebody) data)

    (set! (.-type submit) "submit")

    (-> form (.appendChild el-filename))
    (-> form (.appendChild filebody))
    (-> form (.appendChild submit))


    (-> js/document .-body (.appendChild form))

    (js/setTimeout
     (fn [_]
       (.submit form)

       (js/setTimeout
        (fn [_] (-> js/document .-body (.removeChild form)))
        500))
     1000)))

(defn vibrate [& pattern]
  (-> js/document
    (.dispatchEvent (new js/CustomEvent "sparttt.vibrate" (clj->js {:detail pattern}))))
  #_(browser.event/dispatch-event js/document (new js/CustomEvent (clj->js {:detail pattern}))))

;(initiate-download "text/plain" "hello world!" "foobar.txt")
(comment 
 (initiate-form-post-download nil "1,2,3" "test1.txt")

)


