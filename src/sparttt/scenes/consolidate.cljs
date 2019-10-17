(ns sparttt.scenes.consolidate
  (:require
   [rum.core :as rum]
   [clojure.string :as str]))

(defn file-content-to-data
  [string-content]
  (->> (str/split-lines string-content)
       (map #(str/split % #","))
       vec))

;(file-content-to-data "id,name,surname\n1,piet,pompies")

(defn parse-csv-file
  [file on-success]
  (let [reader (js/FileReader.)
        filename (:name file)
        _ (set! 
           (.-onload
            reader)
           (fn [e]
             (let [data (file-content-to-data (-> e (.-target) (.-result)))]
               (on-success
                {:name (.-name file)
                 :data data}))))]
    
    (.readAsText reader file)))

(rum/defc scene []
  [:div "Add a file to analyse"
   [:div 
    [:input
     {:type "file"
      :multiple "multiple"
      :on-change
      (fn [e]
        (let [first-file (-> e (.-target) (.-files) (aget 0))
              reader (new js/FileReader)
              csv-handler
              (fn [e] (println "got file content: " (-> e (.-target) (.-result))))]

          (set! (.-onload reader) csv-handler)
          (js/console.info "file changed: " first-file)

          (parse-csv-file
           first-file
           (fn [result]
             (println "file parsed: " result)))
          ))}]]])
