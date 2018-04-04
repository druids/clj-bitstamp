(ns clj-bitstamp.async
  (:require
    [clojure.core.async :as async]
    [cheshire.core :as cheshire]
    [cheshire.parse :as parse]
    [tol.core :as tol])
  (:import
    com.pusher.client.channel.ChannelEventListener
    com.pusher.client.channel.SubscriptionEventListener
    com.pusher.client.connection.ConnectionEventListener
    com.pusher.client.connection.ConnectionState
    com.pusher.client.Pusher))


(def default-pusher-key "de504dc5763aeef9ff52")


(defn state->keyword
  [state]
  (tol/case+ state
    ConnectionState/ALL :all
    ConnectionState/CONNECTING :connecting
    ConnectionState/CONNECTED :connected
    ConnectionState/DISCONNECTING :disconnecting
    ConnectionState/DISCONNECTED :disconnected
    nil))


(defn- connection-listener
  [pusher callback]
  (reify ConnectionEventListener
    (onConnectionStateChange [this change]
      (callback pusher
                :change
                {:current (-> change .getCurrentState state->keyword)
                 :previous (-> change .getPreviousState state->keyword)}))
    (onError [this message code exception]
      (callback pusher
                :error
                {:message message
                 :code (keyword code)
                 :exception exception}))))


(defn- channel-listener
  [callback]
  (reify ChannelEventListener
    (onSubscriptionSucceeded [this channel-name]
      (callback this channel-name))))


(defn- subscription-listener
  [callback]
  (reify SubscriptionEventListener
    (onEvent [this channel-name event-name data]
      (binding [parse/*use-bigdecimals?* true]
        (callback channel-name (keyword event-name) (cheshire/parse-string data true))))))


(defn disconnect
  [pusher]
  (.disconnect pusher))


(defn connect
  [pusher]
  (.connect pusher))


(defn pusher-factory
  [pusher {:keys [channel-name pusher-key event-name status-buffer-or-n data-buffer-or-n] :as opts}]
  (let [status-ch (async/chan status-buffer-or-n)
        data-ch (async/chan data-buffer-or-n)
        pusher-callback (fn [pusher action data]
                          (async/>!! status-ch [action data]))
        subs-callback (fn [channel-name event-name data]
                        (async/>!! data-ch [channel-name event-name data]))
        pusher-channel (.subscribe pusher channel-name)]
    (.connect pusher (connection-listener pusher pusher-callback) (into-array [ConnectionState/ALL]))
    (.bind pusher-channel (name event-name) (subscription-listener subs-callback))
    [pusher pusher-channel status-ch data-ch]))


(defn new-pusher
  "Opens a new connection with the Pusher server, subscribes a requested channel and returns a following tuple
   [pusher pusher-channel status-ch data-ch]
   - `pusher` a Pusher instance
   - `pusher` a subscribed Channel instance
   - `status-ch` an async channel containing a pusher and channel messages as a tuple `[action data]`
   - `data-ch` an async channel containing data for the subscribed channel as a tuple `[channel-name event-name data]`

  The function takes an option object:
   - `channel-name` a channel name to subscribe
   - `pusher-key` a Pusher key, default de504dc5763aeef9ff52
   - `event-name` an event name to bind on the subscribed channel
   - `status-buffer-or-n` a buffer-or-n for the status channel
   - `data-buffer-or-n` a buffer-or-n for the data channel"
  [{:keys [channel-name pusher-key event-name status-buffer-or-n data-buffer-or-n] :as opts}]
  (pusher-factory (Pusher. (or pusher-key default-pusher-key)) opts))
