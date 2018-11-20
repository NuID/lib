(ns nuid.utils
  (:require
   [cognitect.transit :as t]
   [nuid.ecc :as ecc]
   [nuid.bn :as bn]
   #?@(:cljs
       [["scryptsy" :as scryptjs]
        ["brorand" :as brand]
        ["hash.js" :as h]
        ["buffer" :as b]]))
  #?@(:clj
      [(:import
        (java.io
         ByteArrayInputStream
         ByteArrayOutputStream)
        (java.security
         MessageDigest
         SecureRandom))]))

(def transit-reader-opts
  {:handlers (merge ecc/point-read-handler
                    bn/read-handler)})

(def transit-writer-opts
  {:handlers (merge ecc/point-write-handler
                    bn/write-handler)})

(def transit-read
  #?(:clj
     (fn [s]
       (let [in (ByteArrayInputStream. (.getBytes s "UTF-8"))
             default-reader (t/reader in :json transit-reader-opts)]
         (t/read default-reader)))
     :cljs
     (let [default-reader (t/reader :json transit-reader-opts)]
       (fn
         ([s] (t/read default-reader s))
         ([reader s] (t/read reader s))))))

(def transit-write
  #?(:clj
     (fn [data]
       (let [out (ByteArrayOutputStream.)
             default-writer (t/writer out :json transit-writer-opts)]
         (t/write default-writer data)
         (str out)))
     :cljs
     (let [default-writer (t/writer :json transit-writer-opts)]
       (fn
         ([data] (t/write default-writer data))
         ([writer data] (t/write writer data))))))

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
  #?(:clj (apply str (map #(format "%02x" %) (.getBytes s "UTF-8")))
     :cljs (-> s b/Buffer.from (.toString "hex"))))

(defn hex-encode [data]
  (str "0x" (->> data transit-write str->hex)))

(defn hex->str [hex]
  #?(:clj
     (let [f (fn [[x y]] (unchecked-byte (Integer/parseInt (str x y) 16)))
           bytes (into-array Byte/TYPE (map f (partition 2 hex)))]
       (String. bytes "UTF-8"))
     :cljs
     (-> hex (b/Buffer.from "hex") .toString)))

(defn hex-decode [hex]
  (->> (subs hex 2) hex->str transit-read))

(def secure-random-bytes
  #?(:clj
     (let [srand (SecureRandom.)]
       (fn [n]
         (let [b (byte-array n)]
           (.nextBytes srand b)
           b)))
     :cljs
     (fn [n] (brand n))))

(defn secure-random-bn [n]
  #?(:clj (nuid.bn.BN (BigInteger. 1 (secure-random-bytes n)))
     :cljs (bn/str->bn (secure-random-bytes n))))

(defn randlt [n lt]
  (let [ret (secure-random-bn n)]
    (if (bn/lte ret lt)
      ret
      (recur n lt))))

(defn sha256 [a]
  #?(:cljs
     (-> (h/sha256) (.update a) .digest)))

(defn sha512 [a]
  #?(:clj
     (let [md (MessageDigest/getInstance "SHA-512")]
       (->> a .getBytes (.digest md)))
     :cljs
     (-> (h/sha512) (.update a) .digest)))

(defn generate-salt [n]
  #?(:cljs (.toString (b/Buffer.from (secure-random-bytes n)) "base64")))

(defn generate-scrypt-parameters
  [{:keys [salt n r p key-length normalization-form]}]
  {:fn :scrypt
   :salt (or salt (generate-salt 32))
   :normalization-form (or normalization-form "NFKC")
   :key-length (or key-length 32)
   :n (or n 16384)
   :r (or r 16)
   :p (or p 1)})

(defn scrypt
  [{:keys [salt n r p key-length normalization-form]} a]
  #?(:cljs
     (let [form (or normalization-form "NFKC")
           a' (if (string? a)
                (b/Buffer.from (.normalize a form))
                a)]
       (scryptjs a' salt n r p key-length))))

(defmulti generate-hashfn :fn)
(defmethod generate-hashfn :scrypt
  [opts]
  (let [params (generate-scrypt-parameters opts)]
    (fn [a] (assoc params :result (scrypt params a)))))

(defmethod generate-hashfn :sha512
  [opts]
  (fn [a] (assoc opts :result (sha512 a))))

#?(:clj
   (defn when-complete [cf f]
     (let [f' (reify java.util.function.BiConsumer
                (accept [this a b] (f this a b)))]
       (-> cf (.whenComplete f')))))

#?(:cljs (def exports
           #js {:generate-scrypt-parameters generate-scrypt-parameters
                :transit-reader-opts transit-reader-opts
                :transit-writer-opts transit-writer-opts
                :secure-random-bytes secure-random-bytes
                :secure-random-bn secure-random-bn
                :generate-hashfn generate-hashfn
                :deep-merge-with deep-merge-with
                :generate-salt generate-salt
                :transit-write transit-write
                :transit-read transit-read
                :remove-index remove-index
                :deep-merge deep-merge
                :hex-encode hex-encode
                :hex-decode hex-decode
                :str->hex str->hex
                :hex->str hex->str
                :sha256 sha256
                :sha512 sha512
                :scrypt scrypt
                :randlt randlt}))
