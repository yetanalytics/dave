.PHONY: clean watch-sass dev-repl test-clj test-cljs ci build-sass build-prod build-prod-server build-sass

clean:
	rm -rf target node_modules package.json package-lock.json gh_pages *.log resources/public/css/style.css*

watch-sass:
	clojure -M:watch-sass

dev-repl:
	clojure -M:fig:build

test-cljs:
	clojure -Mfig:test-cljs

test-clj:
	clojure -Mtest-clj

ci: test-cljs test-clj

build-prod:
	clojure -Mbuild-prod

build-prod-server:
	clojure -Mbuild-prod-server

build-sass:
	clojure -Mbuild-sass

gh_pages: build-prod build-sass
	mkdir -p gh_pages/cljs-out
	cp -rv resources/public/* gh_pages/
	rm gh_pages/index.html gh_pages/test.html
	mv gh_pages/prod_index.html gh_pages/index.html
	cp target/public/cljs-out/prod-main.js gh_pages/cljs-out/
