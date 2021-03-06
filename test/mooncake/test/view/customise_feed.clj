(ns mooncake.test.view.customise-feed
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [mooncake.test.test-helpers.enlive :as eh]
            [mooncake.routes :as r]
            [mooncake.view.customise-feed :as cf]))

(fact "create-feed page should render create-feed template"
      (let [page (cf/customise-feed ...request...)]
        page => (eh/has-class? [:body] "func--customise-feed-page")))

(eh/test-translations "customise-feed page" cf/customise-feed)
(eh/test-logo-link cf/customise-feed)

(fact "page has script link to javascript file"
             (let [page (cf/customise-feed ...request...)]
               (html/select page [[:script (html/attr= :src "js/main.js")]]) =not=> empty?))

(fact "username is rendered"
      (cf/customise-feed {:session {:username "Dave"}}) => (eh/text-is? [:.clj--username] "Dave"))

(fact "sign-out link is rendered and directs to /sign-out when user is signed in"
      (let [page (cf/customise-feed {:session {:username ...username...}})]
        page => (eh/links-to? [:.clj--sign-out__link] (r/path :sign-out))
        page =not=> (eh/has-class? [:.clj--sign-out__link] "clj--STRIP")))

(fact "sign-out link is not rendered if user is not signed in"
      (let [page (cf/customise-feed {})]
        page => (eh/has-class? [:.clj--sign-out__link] "clj--STRIP")))

(fact "customise-feed link is rendered and directs to /customise-feed when user is signed in"
      (let [page (cf/customise-feed {:session {:username ...username...}})]
        page => (eh/links-to? [:.clj--customise-feed__link] (r/path :show-customise-feed))
        page =not=> (eh/has-class? [:.clj--customise-feed__link] "clj--STRIP")))

(fact "customise-feed link is not rendered if user is not signed in"
      (let [page (cf/customise-feed {})]
        page => (eh/has-class? [:.clj--customise-feed__link] "clj--STRIP")))

(fact "customise feed form action is set correctly"
      (let [page (cf/customise-feed ...request...)]
        page => (every-checker
                  (eh/has-form-method? "post")
                  (eh/has-form-action? (r/path :customise-feed)))))

(facts "available feed sources are displayed with the user's preferences"
       (let [activity-source-preferences [{:id             "objective8"
                                           :name           "Activity Source"
                                           :url            "some url"
                                           :activity-types [{:id       "Create"
                                                             :selected false}
                                                            {:id       "Question"
                                                             :selected true}]}
                                          {:id             "Helsinki"
                                           :name           "Another Source"
                                           :url            "other url"
                                           :activity-types [{:id       "Type"
                                                             :selected false}]}]
             context {:activity-source-preferences activity-source-preferences}
             page (cf/customise-feed {:context context})]
         (fact "only items generated for activity sources are present"
               (count (html/select page [:.clj--feed-item])) => 2)

         (fact "only provided activity types of activity sources are present"
               (count (html/select page [:.clj--feed-item-child])) => 3)

         (fact "names of activity sources are displayed"
               (let [[first-activity-source-label-name second-activity-source-label-name]
                     (html/select page [:.clj--feed-item__name])]
                 (html/text first-activity-source-label-name) => "Activity Source"
                 (html/text second-activity-source-label-name) => "Another Source"))

         (fact "names of provided activity types of activity sources are displayed"
               (let [[first-activity-type-label-name second-activity-type-label-name third-activity-type-label-name]
                     (html/select page [:.clj--feed-item-child__name])]
                 (:attrs first-activity-type-label-name) => (contains {:data-l8n "content:activity-type/customise-feed-create"})
                 (:attrs second-activity-type-label-name) => (contains {:data-l8n "content:activity-type/customise-feed-question"})
                 (:attrs third-activity-type-label-name) => (contains {:data-l8n "content:activity-type/customise-feed-type"})
                 (html/text third-activity-type-label-name) => "Type"))

         (fact "name attributes for provided activity types of activity sources selection checkboxes are set correctly"
               (let [[first-activity-type-checkbox second-activity-type-checkbox third-activity-type-checkbox]
                     (html/select page [:.clj--feed-item-child__checkbox])]
                 (:attrs first-activity-type-checkbox) => (contains {:name "objective8_-_Create"})
                 (:attrs second-activity-type-checkbox) => (contains {:name "objective8_-_Question"})
                 (:attrs third-activity-type-checkbox) => (contains {:name "Helsinki_-_Type"})))

         (fact "'for' attributes of activity types labels match 'id' attributes of activity types inputs"
               (let [[first-activity-type-label second-activity-type-label third-activity-type-label] (html/select page [:.clj--feed-item-child__label])
                     first-label-checkbox (first (html/select first-activity-type-label [:.clj--feed-item-child__checkbox]))
                     second-label-checkbox (first (html/select second-activity-type-label [:.clj--feed-item-child__checkbox]))
                     third-label-checkbox (first (html/select third-activity-type-label [:.clj--feed-item-child__checkbox]))]
                 (-> first-activity-type-label :attrs :for) => (-> first-label-checkbox :attrs :id)
                 (-> second-activity-type-label :attrs :for) => (-> second-label-checkbox :attrs :id)
                 (-> third-activity-type-label :attrs :for) => (-> third-label-checkbox :attrs :id)
                 (-> first-label-checkbox :attrs :id) =not=> (-> second-label-checkbox :attrs :id)))

         (fact "selected activity types are checked"
               (let [[first-activity-type-checkbox second-activity-type-checkbox third-activity-type-checkbox]
                     (html/select page [:.clj--feed-item-child__checkbox])]
                 (contains? (:attrs first-activity-type-checkbox) :checked) => falsey
                 (:attrs second-activity-type-checkbox) => (contains {:checked "checked"})
                 (contains? (:attrs third-activity-type-checkbox) :checked) => falsey))))

(facts "about setting the class on src-checkboxes"
       (let [activity-source-preferences [{:id             "activity-src"
                                           :name           "Activity Source"
                                           :url            "some url"
                                           :activity-types [{:id       "activity-src-activity-type-1"
                                                             :selected true}
                                                            {:id       "activity-src-activity-type-2"
                                                             :selected true}]}
                                          {:id             "another-activity-src"
                                           :name           "Another Source"
                                           :url            "another url"
                                           :activity-types [{:id       "another-activity-src-activity-type-1"
                                                             :selected false}
                                                            {:id       "another-activity-src-activity-type-2"
                                                             :selected false}]}
                                          {:id             "yet-another-activity-src"
                                           :name           "Yet Another Source"
                                           :url            "yet another url"
                                           :activity-types [{:id       "yet-another-activity-src-activity-type-1"
                                                             :selected true}
                                                            {:id       "yet-another-activity-src-activity-type-2"
                                                             :selected false}]}]
             context {:activity-source-preferences activity-source-preferences}
             page (cf/customise-feed {:context context})
             [first-src-checkbox second-src-checkbox third-src-checkbox] (html/select page [:.clj--src-checkbox])]
         (fact "when all types are selected, class is checkbox--all"
               (let [first-src-checkbox-classes (-> first-src-checkbox :attrs :class)]
                 first-src-checkbox-classes => (contains "checkbox--all")
                 first-src-checkbox-classes =not=> (contains "checkbox--some")
                 first-src-checkbox-classes =not=> (contains "checkbox--none")))

         (fact "when no types are selected, class is checkbox--none"
               (let [second-src-checkbox-classes (-> second-src-checkbox :attrs :class)]
                 second-src-checkbox-classes => (contains "checkbox--none")
                 second-src-checkbox-classes =not=> (contains "checkbox--all")
                 second-src-checkbox-classes =not=> (contains "checkbox--some")))

         (fact "when some types are selected, class is checkbox--some"
               (let [third-src-checkbox-classes (-> third-src-checkbox :attrs :class)]
                 third-src-checkbox-classes => (contains "checkbox--some")
                 third-src-checkbox-classes =not=> (contains "checkbox--all")
                 third-src-checkbox-classes =not=> (contains "checkbox--none")))))


(facts "available feed sources are displayed if no activity types are available"
       (let [activity-source-preferences [{:id             "activity-src"
                                           :name           "Activity Source"
                                           :url            "some url"
                                           :selected       true
                                           :activity-types []}
                                          {:id       "another-activity-src"
                                           :name     "Another Source"
                                           :url      "other url"
                                           :selected false}]
             context {:activity-source-preferences activity-source-preferences}
             page (cf/customise-feed {:context context :t {}})]

         (fact "names of activity sources are displayed"
               (let [[first-activity-source-label-name second-activity-source-label-name]
                     (html/select page [:.clj--feed-item__name])]
                 (html/text first-activity-source-label-name) => "Activity Source"
                 (html/text second-activity-source-label-name) => "Another Source"))

         (fact "no acitivity type items are displayed"
               (html/select page [:.clj--feed-item-child]) => empty?)))

(facts "activity source shows whether they can be signed"
       (let [activity-source-preferences [{:id             "signed-activity-src"
                                           :name           "Signed Source"
                                           :url            "some url"
                                           :signed?        true
                                           :selected       true
                                           :activity-types []}
                                          {:id       "unsigned-activity-src"
                                           :name     "Unsigned Source"
                                           :url      "other url"
                                           :selected false}]
             context {:activity-source-preferences activity-source-preferences}
             page (cf/customise-feed {:context context :t {}})
             [first-activity-source-signed-status second-activity-source-signed-status] (html/select page [:.clj--feed-item__signed])]

         (:data-l8n (:attrs first-activity-source-signed-status)) => "content:customise-feed/feed-digitally-signed-true"
         (:data-l8n (:attrs second-activity-source-signed-status)) => "content:customise-feed/feed-digitally-signed-false"))