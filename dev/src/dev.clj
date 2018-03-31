(ns dev
  (:require
    [clojure.core.async :as async]
    [clj-bitstamp.async :as bitstamp]))


(defn subscribe-orderbook
  []
  (let [[pusher pusher-channel status-ch data-ch]
        (bitstamp/new-pusher {:channel-name "order_book_btceur"
                              :event-name "data"
                              :data-buffer-or-n (async/sliding-buffer 16)
                              :status-buffer-or-n 16})]
    (async/go-loop []
                   (let [[_ _ d] (async/<! data-ch)]
                     (println (-> d :bids first)))
                   (recur))
    (async/go-loop []
                   (let [[event-name data] (async/<! status-ch)]
                     (case event-name
                       :change nil
                       :connecting nil
                       :connected nil
                       :disconnecting nil
                       :disconnected nil
                       nil) ;; log error
                     (println event-name data))
                   (recur))))
