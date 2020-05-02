(ns sparttt.aws
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [clojure.string :as str]
   [cljs.reader :as reader]
   [cljs.core.async :refer [<!]]
   [cljs-http.client :as http]))

(def default-config
  {:api :js10g4adv4
   :region :eu-west-1
   :stage :beta
   :default-event-id "no-event-id"})

(def config
  (atom default-config))

(defn config-from-edn [uri]
  (go
    (let [response (<! (http/get uri))]
      (cond
        (:success response)
        (let [_ (println "got response: " response)
              updated
              (reset! config (merge @config (reader/read-string (:body response))))]
          (println "Config updated to: " updated))
        :else
        (js/console.log
         "Could not update config"
         (clj->js
          (select-keys response [:status :error-text :body])))))))

(config-from-edn "aws.edn")

(defn build-uri [& path-parts]
  (let [{:keys [api region stage]} @config
        base (str "https://" (name api) ".execute-api." (name region) ".amazonaws.com/" (name stage))]
    (str/join "/" (cons base path-parts))))

(defn fetch
  "Wrapper to js-ify cljs config and return a promise which parsed json response."
  [endpoint config & [on-resolve]]

  (go
    (let [response (<! (http/request (merge config {:url endpoint :with-credentials? false})))]
      (cond
        (and (:success response) on-resolve)
        (on-resolve (:body response))

        :else
        (println
         "result: "
         {:response response
          :config
          {:method (:method config)
           :headers (:headers config)
           :uri endpoint}})))))

(defn post-event-data
  "Upload data relating to an event by specifying the `event-id`, `filename` and `data`."
  [event-id filename data & [on-resolve]]
  (try
    (assert event-id "No event-id provided.")
    (assert filename "No filename provided.")
    (assert data "No data provided.")
    (fetch
     (build-uri "events" event-id "upload")
     {:method :post
      :headers {"x-filename" filename}
      :body data}
     on-resolve)

    (catch :default e
      (js/alert e))))

(defn fetch-consolidation-data
  [event-id on-resolve]
  (fetch
   (build-uri "events" event-id "for-consolidation") {} on-resolve))
