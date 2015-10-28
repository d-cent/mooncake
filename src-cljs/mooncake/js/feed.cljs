(ns mooncake.js.feed
  (:require [ajax.core :refer [GET]]
            [dommy.core :as d]
            [hickory.core :as hic]
            [hickory.render :as hic-r]
            [cognitect.transit :as t])
  (:require-macros [dommy.core :as dm]))

(def r (t/reader :json))

(defn set-author-initials! [activity feed-item]
  (let [author (get-in activity ["actor" "displayName"])
        initial (-> author first str clojure.string/upper-case)]
    (d/set-text! (dm/sel1 feed-item :.clj--avatar__initials) initial)))

(defn set-author! [activity feed-item]
  (let [author (get-in activity ["actor" "displayName"])]
    (d/set-text! (dm/sel1 feed-item :.clj--activity-item__action__author) author)))

(defn set-title! [activity feed-item]
  (let [title (get activity "limited-title")]
    (d/set-text! (dm/sel1 feed-item :.clj--activity-item__title) title)))

(defn set-time! [activity feed-item]
  (let [human-readable-time (get activity "formatted-time")
        time (get activity "published")]
    (d/set-attr! (dm/sel1 feed-item :.clj--activity-item__time) "datetime" time)
    (d/set-text! (dm/sel1 feed-item :.clj--activity-item__time) human-readable-time)))

(defn set-link! [activity feed-item]
  (let [link (get-in activity ["object" "url"])]
    (d/set-attr! (dm/sel1 feed-item :.clj--activity-item__link) "href" link)))

(defn set-src-class! [activity feed-item]
  (let [prev-activity-source-class (re-find #"activity-src-\d+" (d/html feed-item))
        activity-source-class (str "activity-src-" (get activity "activity-src-no"))]
    (d/remove-class! (dm/sel1 feed-item :.clj--activity-item) prev-activity-source-class)
    (d/add-class! (dm/sel1 feed-item :.clj--activity-item) activity-source-class)))

(defn set-action! [activity feed-item]
  (let [action-text (get activity "action-text")]
    (d/remove-attr! (dm/sel1 feed-item :.clj--activity-item__action) "data-l8n")
    (d/set-text! (dm/sel1 feed-item :.clj--activity-item__action) action-text)))

(defn handler [response]
  (let [activities (get response "activities")
        feed-items (. (dm/sel1 :.clj--activity-stream) -innerHTML)
        feed-items-as-hic (map hic/as-hickory (hic/parse-fragment feed-items))
        feed-item (first feed-items-as-hic)]
    (doseq [activity activities]
      (let [new-feed-item (d/create-element "li")]
        (set! (. new-feed-item -innerHTML) (hic-r/hickory-to-html feed-item))
        (set-author-initials! activity new-feed-item)
        (set-author! activity new-feed-item)
        (set-title! activity new-feed-item)
        (set-time! activity new-feed-item)
        (set-link! activity new-feed-item)
        (set-src-class! activity new-feed-item)
        (set-action! activity new-feed-item)
        (d/append! (dm/sel1 :.clj--activity-stream) (.-firstElementChild new-feed-item))))))

(defn error-handler [response]
  (.log js/console (str "something bad happened: " response)))

(def load-activities-link ".clj--load-activities__link")

(defn load-more-activities [e]
  (let [stream (dm/sel1 :.clj--activity-stream)
        last-activity (.-lastChild stream)
        selector (dm/sel1 last-activity :.clj--activity-item__time)
        timestamp (d/attr selector "datetime")]
    (GET (str "/api/activities?timestamp=" timestamp)
         {:handler       handler
          :error-handler error-handler})))