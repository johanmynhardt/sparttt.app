(defproject sparttt.app "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :license
  {:name "Eclipse Public License"
   :url "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.7.1"

  :dependencies
  [[org.clojure/clojure "1.9.0"]
   [org.clojure/clojurescript "1.10.339"]
   [com.andrewmcveigh/cljs-time "0.5.2"]
   [rum "0.11.2"]

   ; figwheel deps
   [com.bhauman/figwheel-main "0.1.9"]
   [com.bhauman/rebel-readline-cljs "0.1.4"]]

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
     [figwheel-sidecar "0.5.16"]]}})

