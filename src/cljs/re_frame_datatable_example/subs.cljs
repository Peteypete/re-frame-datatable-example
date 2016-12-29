(ns re-frame-datatable-example.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]))



(re-frame/reg-sub
  ::email-threads
  (fn [db]
    (:email-threads db)))


(re-frame/reg-sub
  ::labels
  (fn [db]
    (let [default-labels (:labels db)]
      (concat
        (filter #(#{:inbox} (:key %)) default-labels)
        [{:key :starred :title "Starred"}
         {:key :important :title "Important"}
         {:key :all :title "All"}]
        (filter #(#{:spam :trash} (:key %)) default-labels)))))


(re-frame/reg-sub
  ::active-label
  (fn [db]
    (:active-label db)))


(re-frame/reg-sub
  ::threads-digest
  :<- [::email-threads]
  :<- [::active-label]
  (fn [[email-threads active-label]]
    (->> email-threads
         (vals)
         (filter (fn [{:keys [label emails starred?]}]
                   (cond
                     (#{:inbox :spam :trash} active-label)
                     (= active-label label)

                     (= :all active-label)
                     (#{:inbox :archived} label)

                     (= :important active-label)
                     (and (<= 3 (count emails)) (= label :inbox))

                     (= :starred active-label)
                     (and starred? (#{:inbox :archived} label))

                     :else
                     false)))

         (map (fn [{:keys [emails] :as thread}]
                (merge
                  (select-keys thread [:label :id :starred?])
                  {:participants       (map :from emails)
                   :subject            (:subject (first emails))
                   :body-digest        (let [bodies (map :body emails)
                                             dates (map (comp #(.format (js/moment %) "YYYY-MM-DD") :date) emails)]
                                         (clojure.string/join \space (interleave bodies dates)))
                   :last-received-date (:date (last emails))})))

         (sort-by :last-received-date >))))
