(ns mooncake.test.domain.activity
  (:require [midje.sweet :refer :all]
            [mooncake.domain.activity :as activity]))

(fact "about activity->default-action-text"
      (let [activity-1 {:type      "Create"
                        :published "2015-09-06T11:05:53+03:00"
                        :object    {:type    "UnrecognisedType"
                                    :name    "SAMPLE TITLE"
                                    :content "SAMPLE CONTENT"}}]
        (activity/activity->default-action-text activity-1) => "- UnrecognisedType - Create"))
