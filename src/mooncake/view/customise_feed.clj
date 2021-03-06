(ns mooncake.view.customise-feed
  (:require [net.cgrand.enlive-html :as html]
            [mooncake.routes :as r]
            [mooncake.helper :as mh]
            [mooncake.view.view-helpers :as vh]
            [mooncake.translation :as translation]))

(defn set-form-action [enlive-m]
  (html/at enlive-m [:.clj--customise-feed__form] (html/set-attr :action (r/path :customise-feed))))

(defn- create-activity-type-id [activity-source-id activity-type-name]
  (str activity-source-id "_-_" activity-type-name))

(defn activity-type-description [activity-type]
  (str "content:activity-type/customise-feed-" (vh/format-translation-str activity-type)))

(defn generate-feed-item-children [enlive-m activity-source]
  (let [feed-item-child-snippet (first (html/select enlive-m [:.clj--feed-item-child]))]
    (html/at feed-item-child-snippet [html/root]
             (html/clone-for [activity-type (:activity-types activity-source)
                              :let [activity-type-id (create-activity-type-id (:id activity-source) (:id activity-type))]]
                             [:.clj--feed-item-child__label] (html/set-attr :for activity-type-id)
                             [:.clj--feed-item-child__name] (html/do->
                                                              (html/content (:id activity-type))
                                                              (html/set-attr :data-l8n (activity-type-description (:id activity-type))))
                             [:.clj--feed-item-child__checkbox] (html/do->
                                                                  (html/set-attr :name activity-type-id)
                                                                  (html/set-attr :id activity-type-id)
                                                                  (if (:selected activity-type)
                                                                    (html/set-attr :checked "checked")
                                                                    (html/remove-attr :checked)))))))

(defn get-selected-all-some-none [activity-src]
  (let [activity-types (:activity-types activity-src)
        selected-values (map :selected activity-types)]
    (cond
      (every? true? selected-values) :all
      (and (some true? selected-values) (some false? selected-values)) :some
      (every? false? selected-values) :none
      :default :none)))

(defn generate-feed-items [enlive-m activity-source-preferences]
  (let [feed-item-snippet (first (html/select enlive-m [:.clj--feed-item]))]
    (html/at feed-item-snippet [html/root]
             (html/clone-for [activity-source activity-source-preferences]
                             [:.clj--feed-item__name] (html/content (:name activity-source))
                             [:.clj--feed-item__signed] (html/set-attr :data-l8n
                                                                       (if (:signed? activity-source)
                                                                         "content:customise-feed/feed-digitally-signed-true"
                                                                         "content:customise-feed/feed-digitally-signed-false"))
                             [:.clj--src-checkbox] (html/do->
                                                     (html/remove-class "checkbox--all")
                                                     (html/remove-class "checkbox--some")
                                                     (html/remove-class "checkbox--none")
                                                     (case (get-selected-all-some-none activity-source)
                                                       :all (html/add-class "checkbox--all")
                                                       :some (html/add-class "checkbox--some")
                                                       :none (html/add-class "checkbox--none")))
                             [:.clj--feed-item__children-list] (html/content (generate-feed-item-children enlive-m activity-source))))))

(defn add-feed-items [enlive-m activity-source-preferences]
  (html/at enlive-m
           [:.clj--customise-feed__list] (html/content (generate-feed-items enlive-m activity-source-preferences))))

(defn render-sign-out-link [enlive-m signed-in?]
  (if signed-in?
    (html/at enlive-m [:.clj--sign-out__link] (html/do->
                                                (html/remove-class "clj--STRIP")
                                                (html/set-attr :href (r/path :sign-out))))
    enlive-m))

(defn render-customise-feed-link [enlive-m signed-in?]
  (if signed-in?
    (html/at enlive-m [:.clj--customise-feed__link] (html/do->
                                                      (html/remove-class "clj--STRIP")
                                                      (html/set-attr :href (r/path :show-customise-feed))))
    enlive-m))

(defn render-username [enlive-m username]
  (html/at enlive-m [:.clj--username] (html/content username)))

(defn customise-feed [request]
  (->
    (vh/load-template-with-lang "public/customise-feed.html" (translation/get-locale-from-request request))
    (render-username (get-in request [:session :username]))
    (render-sign-out-link (mh/signed-in? request))
    (render-customise-feed-link (mh/signed-in? request))
    (add-feed-items (get-in request [:context :activity-source-preferences]))
    set-form-action
    (vh/add-script "js/main.js")))

