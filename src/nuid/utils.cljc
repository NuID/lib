(ns nuid.utils
  #?@(:clj [(:import java.util.Base64)]
      :cljs [(:require ["buffer" :as b])]))

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
  (-> [(subvec v 0 i) (subvec v (+ i 1))] flatten vec))

(defn str->hex [s]
  #?(:clj (apply str (map #(format "%02x" %) (if (string? s) (.getBytes s "UTF-8") s)))
     :cljs (-> s b/Buffer.from (.toString "hex"))))

(defn hex->str [hex]
  #?(:clj (let [f (fn [[x y]] (unchecked-byte (Integer/parseInt (str x y) 16)))
                bytes (into-array Byte/TYPE (map f (partition 2 hex)))]
            (String. bytes "UTF-8"))
     :cljs (-> hex (b/Buffer.from "hex") .toString)))

(defn str->base64 [s]
  #?(:clj (.encodeToString (Base64/getEncoder) (if (string? s) (.getBytes s) s))
     :cljs (.toString (b/Buffer.from s) "base64")))

(defn base64->bytes [b64]
  #?(:clj (.decode (Base64/getDecoder) b64)
     :cljs (js/Array.prototype.slice.call (b/Buffer.from b64 "base64"))))

(defn base64->str [b64]
  #?(:clj (String. (base64->bytes b64) "UTF-8")
     :cljs (.toString (b/Buffer.from b64 "base64") "utf-8")))

#?(:clj (defn when-complete [cf f]
          (let [f' (reify java.util.function.BiConsumer
                     (accept [this a b] (f this a b)))]
            (-> cf (.whenComplete f')))))

#?(:cljs (def exports
           #js {:deep-merge-with deep-merge-with
                :remove-index remove-index
                :deep-merge deep-merge
                :str->base64 str->base64
                :base64->str base64->str
                :str->hex str->hex
                :hex->str hex->str}))
