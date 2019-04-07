(ns nuid.utils)

(defn deep-merge-with [f & ms]
  (apply merge-with
         (fn [a b]
           (if (and (map? a) (map? b))
             (deep-merge-with f a b)
             (f a b)))
         ms))

(defn deep-merge [& ms]
  (apply deep-merge-with
         (fn [a b] b)
         ms))

(defn remove-index [v i]
  (vec (flatten [(subvec v 0 i) (subvec v (+ i 1))])))
