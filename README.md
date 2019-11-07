# nuid.utils

Common utilities.

## Requirements

[`jvm`](https://www.java.com/en/download/), [`node + npm`](https://nodejs.org/en/download/), [`clj`](https://clojure.org/guides/getting_started), [`shadow-cljs`](https://shadow-cljs.github.io/docs/UsersGuide.html#_installation)

## From Clojure and ClojureScript

### tools.deps:

`{nuid/utils {:git/url "https://github.com/nuid/utils" :sha "..."}}`

## Licensing

Apache v2.0 or MIT

## Contributing

### formatting:

```
$ clojure -A:cljfmt            # check
$ clojure -A:cljfmt:cljfmt/fix # fix
```

### dependencies:

```
## check
$ npm outdated
$ clojure -A:depot

## update
$ npm upgrade -s
$ clojure -A:depot:depot/update
```
