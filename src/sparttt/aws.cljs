(ns sparttt.aws
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]))

(def config
  (atom
   {:api :js10g4adv4
    :region :eu-west-1
    :stage :beta
    :default-event-id "no-event-id"}))

(defn build-uri [& path-parts]
  (let [{:keys [api region stage]} @config
        base
        (str "https://" (name api) ".execute-api." (name region) ".amazonaws.com/" (name stage))]
    (str/join "/" (cons base path-parts))))

(defn fetch
  "Wrapper to js-ify cljs config and return a promise which parsed json response."
  [endpoint config & [on-resolve]]
  (->
   (js/fetch endpoint (clj->js config))
   (.then #(.json %))
   (.then #(walk/keywordize-keys (js->clj %)))
   (.then
    (fn [m]
      (cond
        on-resolve (on-resolve m)
        :else
        (println
         "result: "
         {:response m
          :config
          {:method (:method config)
           :headers (:headers config)
           :uri endpoint}}))))))

(defn post-event-data
  "Upload data relating to an event by specifying the `event-id`, `filename` and `data`."
  [event-id filename data & [on-resolve]]
  (try 
    (assert event-id "No event-id provided.")
    (assert filename "No filename provided.")
    (assert data "No data provided.")
    (fetch
     (build-uri "events" event-id "upload")
     {:method "POST"
      :headers {:x-filename filename}
      :body data}
     on-resolve)

    (catch :default e
      (js/alert e))))
