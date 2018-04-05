(ns clj-bitstamp-test.async-test
  (:require
    [clojure.core.async :as async]
    [clojure.test :refer [are deftest is testing]]
    [cheshire.core :as cheshire]
    [clj-bitstamp.async :as bs]
    [clj-async-test.core :refer :all])
  (:import
    com.pusher.client.Pusher
    com.pusher.client.connection.ConnectionState
    com.pusher.client.connection.ConnectionStateChange))


(definterface Client
  (connect [])
  (connect [listener _])
  (disconnect [])
  (subscribe [channel-name]))


(definterface Channel
  (bind [event-name listener]))


(defn- pusher-test
  [input output str-big-decimals?]
  (let [pusher-class (reify Client
                       (connect [this])

                       (connect [this listener _]
                         (.onConnectionStateChange listener (ConnectionStateChange. ConnectionState/CONNECTING
                                                                                    ConnectionState/CONNECTED)))
                       (disconnect [this])

                       (subscribe [this channel-name]
                         (reify Channel
                           (bind [this event-name listener]
                             (.onEvent listener
                                       channel-name
                                       "data"
                                       (cheshire/generate-string input))))))
        [pusher pusher-channel status-ch data-ch]
        (bs/pusher-factory pusher-class
                           {:channel-name "order_book_btceur"
                            :event-name "data"
                            :data-buffer-or-n 1
                            :status-buffer-or-n 16
                            :str-big-decimals? str-big-decimals?})]
    (let [[_ event-name data] (async/<!! data-ch)]
      (is (eventually (= :data event-name)))
      (is (eventually (= output data)))
      (is (eventually (= [:change {:current :connected, :previous :connecting}] (async/<!! status-ch))))
      (bs/disconnect pusher)
      (bs/connect pusher))))


(deftest new-pusher-test
  (doseq [[input output str-big-decimals?] [[{:bids [[5555.0 4.0702]]} {:bids [[5555.0M 4.0702M]]} false]
                                            [{:bids [["5555.00" "4.0702"]]} {:bids [[5555.00M 4.0702M]]} true]
                                            [{:bids [["5555.00" "4.0702"]]} {:bids [["5555.00" "4.0702"]]} false]]]
    (pusher-test input output str-big-decimals?)))
