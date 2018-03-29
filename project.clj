(defproject clj-bitstamp "0.0.0"
  :description "A client for Bitstamp API based on core.async"
  :url "https://github.com/druids/clj-bitstamp"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/core.async "0.4.474"]
                 [com.pusher/pusher-java-client "1.8.0"]]

  :profiles {:dev {:plugins [[lein-cloverage "1.0.10"]
                             [lein-kibit "0.1.6"]
                             [jonase/eastwood "0.2.5"]]
                   :dependencies [[org.clojure/clojure "1.9.0"]]
                   :source-paths ["src" "dev/src"]}}
  :aliases {"coverage" ["cloverage" "--fail-threshold" "95" "-e" "dev|user"]})
