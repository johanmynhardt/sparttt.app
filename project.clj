(defproject sparttt.app "0.1.0-SNAPSHOT"
  :description "Running Time Trial app in ClojureScript."
  :url "https://github.com/johanmynhardt/sparttt.app/"
  :license
  {:name "Eclipse Public License"
   :url "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.8.3"

  :dependencies
  [[org.clojure/clojure "1.9.0"]
   [org.clojure/clojurescript "1.10.339"]
   [com.andrewmcveigh/cljs-time "0.5.2"]
   [rum "0.11.2"]

   [nrepl "0.6.0"]

   ; figwheel deps
   [com.bhauman/figwheel-main "0.1.9"]
   [com.bhauman/rebel-readline-cljs "0.1.4"]
  
   [figwheel-sidecar "0.5.16"]
   [cider/piggieback "0.4.1"]]

  :source-paths ["src"]

  :aliases
  {"fig" ["trampoline" "run" "-m" "figwheel.main"]
   "fig:build" ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]
   "fig:min" ["run" "-m" "figwheel.main" "-O" "advanced" "-bo" "min"]
   "fig:test" ["run" "-m" "figwheel.main" "-co" "test.cljs.edn" "-m" sparttt.test-runner]
   "build-dev" ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]
   "fig:min:pc" ["run" "-m" "figwheel.main" "-pc" "-b" "min" "-r"]}

  :profiles
  {:dev
   {:dependencies
    [[com.bhauman/figwheel-main "0.1.9"]
     [com.bhauman/rebel-readline-cljs "0.1.4"]
     [figwheel-sidecar "0.5.16"]
     [cider/piggieback "0.4.1"]
     [org.clojure/tools.nrepl "0.2.13"]]

    :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}}})

