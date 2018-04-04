clj-bitstamp
=================

A client for [Bitstamp API](https://www.bitstamp.net/websocket) based on [core.async](https://github.com/clojure/core.async).

[![CircleCI](https://circleci.com/gh/druids/clj-bitstamp.svg?style=svg)](https://circleci.com/gh/druids/clj-bitstamp)
[![Dependencies Status](https://jarkeeper.com/druids/clj-bitstamp/status.png)](https://jarkeeper.com/druids/clj-bitstamp)
[![License](https://img.shields.io/badge/MIT-Clause-blue.svg)](https://opensource.org/licenses/MIT)


Leiningen/Boot
--------------

```clojure
[clj-bitstamp "0.0.0"]
```

Documentation
-------------

This library is a thin wrapper around `com.pusher/pusher-java-client` within a simple usage. A function `new-pusher`
 opens a new connection with the Pusher server, subscribes a requested channel and returns a following tuple
`[pusher pusher-channel status-ch data-ch]` where

- `pusher` a Pusher instance
- `pusher` a subscribed Channel instance
- `status-ch` an async channel containing a pusher and channel messages as a tuple `[action data]`
- `data-ch` an async channel containing data for the subscribed channel as a tuple `[channel-name event-name data]`

The function takes an option object:
- `channel-name` a channel name to subscribe, required
- `pusher-key` a Pusher key, default de504dc5763aeef9ff52, optional
- `event-name` an event name to bind on the subscribed channel, optional
- `status-buffer-or-n` a buffer-or-n for the status channel, optional
- `data-buffer-or-n` a buffer-or-n for the data channel, optional

Data channel returns a tuple of channel-name, event-name (as `keyword`), and data.

Status channel returns a tuple of event-name (as `keyword`), and data. Expected events:

- `:change`
- `:connecting`
- `:connected`
- `:disconnecting`
- `:disconnected`


Example:

```clojure
(require '[clj-bitstamp.core :as clj-bitstamp])

(let [[pusher pusher-channel status-ch data-ch]
      (bitstamp/new-pusher {:channel-name "order_book_btceur" ;; required
                            :event-name "data" ;; required
                            :data-buffer-or-n (async/sliding-buffer 16) ;; optinal
                            :status-buffer-or-n 16})] ;; optional
  (async/go-loop []
                 (let [[channel-name event-name data] (async/<! data-ch)]
                   (println channel-name event-name data)) ;; <-- put you logic here
                 (recur))
  (async/go-loop []
                 (let [[event-name data] (async/<! status-ch)]
                   (println event-name data)) ;; <-- put status handler here
                 (recur)))
```

An established connection can be disconnected by

```clojure
(clj-bitstamp/disconnect pusher)
```

and again connected withoin same `pusher` instance

```clojure
(clj-bitstamp/connect pusher)
```
